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
import android.graphics.Point;
import android.net.Uri;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nz.co.emtek.weather.MetserviceHelper;


public class ForcastWidget extends AppWidgetProvider {

	
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        // To prevent any ANR timeouts, we perform the update in a service
        context.startService(new Intent(context, UpdateService.class));
    }

    public static class UpdateService extends Service {
        @Override
        public void onStart(Intent intent, int startId) {
            // Build the widget update for today
            RemoteViews updateViews = buildUpdate(this);

            // Push update for this widget to the home screen
            ComponentName thisWidget = new ComponentName(this, ForcastWidget.class);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(thisWidget, updateViews);
        }

        public RemoteViews buildUpdate(Context context) {

            // Build the page title for today, such as "March 21"
            String pageName = "3day";
            String[] pageContent = null;

            try {
                // Try querying the Wiktionary API for today's word
                MetserviceHelper.prepareUserAgent(context);
                pageContent = MetserviceHelper.getPageContent(pageName);
            }catch(MetserviceHelper.ParseException e){
            	Log.e("WordWidget", "Couldn't do json", e);
            }

            RemoteViews views = null;

            if (pageContent != null) {
                // Build an update that holds the updated widget contents
            	views = new RemoteViews(context.getPackageName(), R.layout.forcast_widget);
                URL url;
				try {
					url = new URL(pageContent[1]);
					//Uri uri = Uri.parse(pageContent[1]);
					//views.setImageViewUri(R.id.icon, uri);
					Bitmap bm = MetserviceHelper.downloadImage(url);
					Bitmap region = MetserviceHelper.cropBitmap(bm, new Point(200,110), new Point(400,174));
					//views.setBitmap(R.id.icon, "", bm);
					
					views.setImageViewBitmap(R.id.icon, region);
	                //views.setTextViewText(R.id.word_type, pageContent[0]);
	                
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}		
                

                // When user clicks on widget, launch to Wiktionary definition page
//                String definePage = String.format("%s://%s/%s", ExtendedWikiHelper.WIKI_AUTHORITY,
//                        ExtendedWikiHelper.WIKI_LOOKUP_HOST, wordTitle);
//                Intent defineIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(definePage));
//                PendingIntent pendingIntent = PendingIntent.getActivity(context,
//                        0 /* no requestCode */, defineIntent, 0 /* no flags */);
//                views.setOnClickPendingIntent(R.id.widget, pendingIntent);

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
