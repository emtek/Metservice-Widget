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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncResult;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
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
import java.util.Currency;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nz.co.emtek.weather.MetserviceHelper;


public class ForcastWidget extends AppWidgetProvider {

	private static String NEXT_IMAGE_ACTION = "NextForecast";
	private static String FIRST_IMAGE_ACTION = "FirstForecast";
	private static String PLAY_SEQ = "PlaySeq";
	private static String PLAYING = "Playing";
	
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        // To prevent any ANR timeouts, we perform the update in a service
        context.startService(new Intent(context, UpdateService.class));
    }
   
    
    @Override
    public void onReceive(Context context, Intent intent) {
     super.onReceive(context, intent);
     String intentAction = intent.getAction();
	     if(isAction(intentAction)){ 
	    			context.startService(new Intent(intentAction).setClass(context, UpdateService.class));
	    	 
	     }else{//DIIRRTY
	    	 if(intentAction== "nz.co.emtek.RainForcast.PLAY_ALARM"){
	    			
					context.startService(new Intent(NEXT_IMAGE_ACTION).setClass(context, UpdateService.class));
					
	    		}
	     }
    }
    
    public boolean isAction(String intentAction){
    	return (((intentAction== FIRST_IMAGE_ACTION)||(intentAction == NEXT_IMAGE_ACTION))||(intentAction == PLAY_SEQ));
    }

    public static class UpdateService extends Service {
    	
    	@Override
        public void onStart(Intent intent, int startId) {
    		
    		//MetserviceHelper.prepareUserAgent(this.getApplicationContext());
    		String intentAction = intent.getAction();
    		if(!(((intentAction== FIRST_IMAGE_ACTION)||(intentAction == NEXT_IMAGE_ACTION))||(intentAction == PLAY_SEQ))){
    			MetserviceHelper.clearCache(this.getApplicationContext());
    			MetserviceHelper.updateForecast(this.getApplicationContext());
    			
            }
    		
    		if((intentAction== FIRST_IMAGE_ACTION)||(intentAction == PLAY_SEQ)){
    			MetserviceHelper.updateForecast(this.getApplicationContext());
    			MetserviceHelper.setPosition(1);
    		}
    		
    		if(intentAction == PLAY_SEQ){


				for(int i = 1; i <= MetserviceHelper.frames; i++){
					
					long timeToStart = System.currentTimeMillis() + i*100;
					MetserviceHelper.setPosition(i);
					RemoteViews updateViews = buildUpdate(this);
		            
		            //Dictionary <Action, elementID>
		            HashMap<String, Integer> eventTriggers = new HashMap<String, Integer>() ;
		            eventTriggers.put(NEXT_IMAGE_ACTION, R.id.icon);
		            eventTriggers.put(FIRST_IMAGE_ACTION, R.id.refresh_icon);
		            eventTriggers.put(PLAY_SEQ, R.id.play_icon);
		            Iterator<String> keys = eventTriggers.keySet().iterator();
		    	     // Register an onClickListeners
		            while(keys.hasNext()){
		            	String action = keys.next();
		    			Intent newIntent = new Intent(this.getApplicationContext(), ForcastWidget.class);
		    			newIntent.setAction(action);
		    	
		    			PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(),
		    					0, newIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		    			updateViews.setOnClickPendingIntent(eventTriggers.get(action), pendingIntent);
		            }
		            // Push update for this widget to the home screen
		            ComponentName thisWidget = new ComponentName(this, ForcastWidget.class);
		            AppWidgetManager manager = AppWidgetManager.getInstance(this);
		            manager.updateAppWidget(thisWidget, updateViews);
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//am.set(AlarmManager.RTC, timeToStart, pendingIntent);
				}
    		}else{
    		
	    		RemoteViews updateViews = buildUpdate(this);
	            
	            //Dictionary <Action, elementID>
	            HashMap<String, Integer> eventTriggers = new HashMap<String, Integer>() ;
	            eventTriggers.put(NEXT_IMAGE_ACTION, R.id.icon);
	            eventTriggers.put(FIRST_IMAGE_ACTION, R.id.refresh_icon);
	            eventTriggers.put(PLAY_SEQ, R.id.play_icon);
	            Iterator<String> keys = eventTriggers.keySet().iterator();
	    	     // Register an onClickListeners
	            while(keys.hasNext()){
	            	String action = keys.next();
	    			Intent newIntent = new Intent(this.getApplicationContext(), ForcastWidget.class);
	    			newIntent.setAction(action);
	    	
	    			PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(),
	    					0, newIntent, PendingIntent.FLAG_UPDATE_CURRENT);
	    			updateViews.setOnClickPendingIntent(eventTriggers.get(action), pendingIntent);
	            }
	            // Push update for this widget to the home screen
	            ComponentName thisWidget = new ComponentName(this, ForcastWidget.class);
	            AppWidgetManager manager = AppWidgetManager.getInstance(this);
	            manager.updateAppWidget(thisWidget, updateViews);
    		}
        }

        public RemoteViews buildUpdate(Context context) {
            String[] item = null;
            item = MetserviceHelper.getNextItem();
            RemoteViews views = null;

            if (item != null) {
                // Build an update that holds the updated widget contents
            	views = new RemoteViews(context.getPackageName(), R.layout.forcast_widget);
            	
				File cache = MetserviceHelper.downloadFile(context.getCacheDir().toString(),
						item[1]);
		  	   
		  	   Bitmap im = BitmapFactory.decodeFile(cache.getAbsolutePath());
		  	   Bitmap region = MetserviceHelper.cropBitmap(im, new Point(150,50), new Point(440,265));
				
				views.setImageViewBitmap(R.id.icon, region);
				views.setTextViewText(R.id.date_text, item[0]);		

            } else {
                
                views = new RemoteViews(context.getPackageName(), R.layout.loading_message);
                views.setTextViewText(R.id.message, context.getString(R.string.widget_error));
                Intent newIntent = new Intent(this.getApplicationContext(), ForcastWidget.class);
    			newIntent.setAction(FIRST_IMAGE_ACTION);
    	
    			PendingIntent pendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(),
    					0, newIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    			views.setOnClickPendingIntent(R.id.message, pendingIntent);
            }
            return views;
        }
        
       // public RemoteViews build

        @Override
        public IBinder onBind(Intent intent) {
            // We don't need to bind to this service
            return null;
        }
    }
}
