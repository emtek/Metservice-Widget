/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nz.co.emtek.weather;

import nz.co.emtek.weather.R;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nz.co.emtek.weather.MetserviceHelper;


public class ForcastWidget extends AppWidgetProvider {

	private static String NEXT_IMAGE_ACTION = "NextForecast";
	private static String FIRST_IMAGE_ACTION = "FirstForecast";
	
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        // To prevent any ANR timeouts, we perform the update in a service
        context.startService(new Intent(context, UpdateService.class));
    }
   
    
    @Override
    public void onReceive(Context context, Intent intent) {
     super.onReceive(context, intent);
     if(intent != null){
     	if (intent.getAction().equals(NEXT_IMAGE_ACTION)) {
    	   String[] item = MetserviceHelper.getNextItem();
    	   File cache = new File(context.getCacheDir()
					+ item[1].substring(item[1].lastIndexOf("/"), item[1].length()));
    	   if (!cache.exists()){
    		  cache = MetserviceHelper.downloadImage(context.getCacheDir().toString(),item[1]);
    	   }
    	   Bitmap im = BitmapFactory.decodeFile(cache.getAbsolutePath());

    	   Bitmap region = MetserviceHelper.cropBitmap(im, new Point(150,50), new Point(440,265));
    	   RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
      				R.layout.forcast_widget);
    	   remoteViews.setImageViewBitmap(R.id.icon, region);
    	   remoteViews.setTextViewText(R.id.date_text, item[0]);
    	   ComponentName thisWidget = new ComponentName(context,ForcastWidget.class);
           AppWidgetManager manager = AppWidgetManager.getInstance(context);
           manager.updateAppWidget(thisWidget, remoteViews);
           
    	}
     	
     	if (intent.getAction().equals(FIRST_IMAGE_ACTION)) {
     	   String[] item = MetserviceHelper.getFirstItem();
     	   File cache = new File(context.getCacheDir()
 					+ item[1].substring(item[1].lastIndexOf("/"), item[1].length()));
     	   if (!cache.exists()){
     		  cache = MetserviceHelper.downloadImage(context.getCacheDir().toString(),item[1]);
     	   }
     	   Bitmap im = BitmapFactory.decodeFile(cache.getAbsolutePath());

     	   Bitmap region = MetserviceHelper.cropBitmap(im, new Point(150,50), new Point(440,265));
     	   RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
       				R.layout.forcast_widget);
     	   remoteViews.setImageViewBitmap(R.id.icon, region);
     	   remoteViews.setTextViewText(R.id.date_text, item[0]);
     	   ComponentName thisWidget = new ComponentName(context,ForcastWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            manager.updateAppWidget(thisWidget, remoteViews);
            
     	}
     }
    }

    public static class UpdateService extends Service {
        private MetserviceHelper helper;
        
    	
    	@Override
        public void onStart(Intent intent, int startId) {
//    		if(intent.getAction().equals(NEXT_IMAGE_ACTION)){
//    			
//    		}else{
        	MetserviceHelper.updateForecast();
        	
            // Build the widget update for today
            RemoteViews updateViews = buildUpdate(this);
            
    	     // Register an onClickListener
    			Intent newIntent = new Intent(this.getApplicationContext(), ForcastWidget.class);
    	
    			newIntent.setAction(NEXT_IMAGE_ACTION);
    	
    			PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(),
    					0, newIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    			updateViews.setOnClickPendingIntent(R.id.icon, pendingIntent);
            
    			Intent newIntent2 = new Intent(this.getApplicationContext(), ForcastWidget.class);
    	    	
    			newIntent.setAction(FIRST_IMAGE_ACTION);
    	
    			PendingIntent pendingIntent2 = PendingIntent.getBroadcast(this.getApplicationContext(),
    					0, newIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    			updateViews.setOnClickPendingIntent(R.id.refresh_icon, pendingIntent2);
    			
            
            // Push update for this widget to the home screen
            ComponentName thisWidget = new ComponentName(this, ForcastWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
        }

        public RemoteViews buildUpdate(Context context) {

            String[] item = null;

            MetserviceHelper.prepareUserAgent(context);
			item = MetserviceHelper.getFirstItem();

            RemoteViews views = null;

            if (item != null) {
                // Build an update that holds the updated widget contents
            	views = new RemoteViews(context.getPackageName(), R.layout.forcast_widget);
            	
				File cache = new File(context.getCacheDir()
						+ item[1].substring(item[1].lastIndexOf("/"), item[1].length()));
		  	   if (!cache.exists()){
						  cache = MetserviceHelper.downloadImage(context.getCacheDir().toString(),item[1]);
		  	   }
		  	   Bitmap im = BitmapFactory.decodeFile(cache.getAbsolutePath());
		  	   Bitmap region = MetserviceHelper.cropBitmap(im, new Point(150,50), new Point(440,265));
				
				views.setImageViewBitmap(R.id.icon, region);
				views.setTextViewText(R.id.date_text, item[0]);		

            } else {
                // Didn't find word of day, so show error message
                views = new RemoteViews(context.getPackageName(), R.layout.loading_message);
                views.setTextViewText(R.id.message, context.getString(R.string.widget_error));
            }
            return views;
        }

        @Override
        public IBinder onBind(Intent intent) {
            // We don't need to bind to this service
            return null;
        }
    }
}
