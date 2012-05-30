package nz.co.emtek.weather;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.util.Log;



public class MetserviceHelper {
//Constants
	private static String threeDayUrl = "http://www.metservice.com/publicData/rainForecast3Day";
	private static String sevenDayUrl = "http://www.metservice.com/publicData/rainForecast7Day";
	private static String baseUrl = "http://www.metservice.com";
	private static String sUserAgent = null;
	private static final String TAG = "AndroidMetserviceHelper";
	private static byte[] sBuffer = new byte[512];
	private static final int HTTP_STATUS_OK = 200;
	private static RainForcastObject[] currentForecast;
	private static int position = 1;
	static int frames =0;

    public static void updateForecast(Context context)//Todo: make it work for 7 day too
             {

        // Query the API for content
       position = 1;
        try {
        	 currentForecast = getCurrentForecastJSON(context,threeDayUrl);
        	frames = currentForecast.length;
        } catch (Exception e) {
			// TODO Auto-generated catch block
        	//throw new ParseException("Problem parsing API response", e);
		}
    }
    
    public static void clearCache(Context context){
    	File cacheDir = context.getCacheDir();

    	File[] files = cacheDir.listFiles();

    	if (files != null) {
    	    for (File file : files)
    	       file.delete();
    	}
    }
    
    public static int getPosition(){
    	if(currentForecast==null) return 1;
    	if(position>currentForecast.length){
    		position = 1;
    	}
    	return position;
    }
    
    public static void setPosition(int i){
    	if(currentForecast==null) return;
    	if(position>currentForecast.length){
    		position = 1;
    	}
    	position = i;
    }
    
    public static String[] getNextItem(){
    	if(currentForecast!=null){
	    	if(position>currentForecast.length){
	    		position = 1;
	    	}
	    	String url = baseUrl + currentForecast[currentForecast.length-position].url.replace("\\", ""); //removes escapes from json
	    	String[] response = new String[]{currentForecast[currentForecast.length-position].shortDateTime, url};
	    	position += 1;
	    	return response;
    	}else{
    		return null;
    	}
    }
	
	 public static class ParseException extends Exception {
	        public ParseException(String detailMessage, Throwable throwable) {
	            super(detailMessage, throwable);
	        }
	    }
	 
	 /**
	     * Pull the raw text content of the given URL. This call blocks until the
	     * operation has completed, and is synchronized because it uses a shared
	     * buffer {@link #sBuffer}.
	     *
	     * @param url The exact URL to request.
	     * @return The raw content returned by the server.
	     * @throws ApiException If any connection or server error occurs.
	     */
	    protected static synchronized RainForcastObject[] getCurrentForecastJSON(Context context,String url) throws Exception {
	        if (sUserAgent == null) {
	            prepareUserAgent(context);
	        }

	        // Create client and set our specific user-agent string
	        HttpClient client = new DefaultHttpClient();
	        HttpGet request = new HttpGet(url);
	        request.setHeader("User-Agent", sUserAgent);

	        try {
	            HttpResponse response = client.execute(request);

	            // Check if server response is valid
	            StatusLine status = response.getStatusLine();
	            if (status.getStatusCode() != HTTP_STATUS_OK) {
	               if(status.getStatusCode() == 302){
	            	   return currentForecast;
	               }
	            }
	            File file = MetserviceHelper.downloadFile(context.getCacheDir().toString(), url);
	            FileReader freader = new FileReader(file);

	            Gson gson = new Gson();
	            return gson.fromJson(freader, RainForcastObject[].class);  
	        } catch (IOException e) {
	            throw new Exception("Problem communicating with API", e);
	        }
	    }
	 
	 public static void prepareUserAgent(Context context) {
	        try {
	            // Read package name and version number from manifest
	            PackageManager manager = context.getPackageManager();
	            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
	            sUserAgent = String.format(context.getString(R.string.template_user_agent),
	                    info.packageName, info.versionName);

	        } catch(NameNotFoundException e) {
	            Log.e(TAG, "Couldn't find package information in PackageManager", e);
	        }
	    }

	public static File downloadFile(String directory, String urlString){
			if(urlString == null) return null;
			File cache = new File(directory
					+ urlString.substring(urlString.lastIndexOf("/"), urlString.length()));
	  	   if (!cache.exists()){
					 
			try {
				URL url = new URL(urlString);
				
				URLConnection conn;

				conn = url.openConnection();

				conn.connect();

				InputStream is;

				is = conn.getInputStream();

				/* Buffered is always good for a performance plus. */
				BufferedInputStream bis = new BufferedInputStream(is);
				/* Decode url-data to a bitmap. */
				FileOutputStream outStream = new FileOutputStream(
						cache, false);
				byte[] data = new byte[10240];
				int len = 0;
				while ((len = bis.read(data)) > 0) {
					outStream.write(data, 0, len);
				}
				
				bis.close();
				is.close();
				outStream.flush();
			return cache;
			}catch(IOException ex){
				ex.printStackTrace();
			}
	  	   }else{
	  		   return cache;
	  	   }
			return null;
		}
	 
	 public static Bitmap cropBitmap(Bitmap photo, Point topLeft,Point bottomRight){
		 return Bitmap.createBitmap(photo, topLeft.x, topLeft.y, bottomRight.x-topLeft.x, bottomRight.y-topLeft.y, null, true);
	 }
	 
	 public static Bitmap scaleDownBitmap(Bitmap photo, int newHeight, Context context) {

		 final float densityMultiplier = context.getResources().getDisplayMetrics().density;        

		 int h= (int) (newHeight*densityMultiplier);
		 int w= (int) (h * photo.getWidth()/((double) photo.getHeight()));

		 photo=Bitmap.createScaledBitmap(photo, w, h, true);

		 return photo;
	 }
}

class RainForcastObject{
	 public String issuedTime;
	 public String longDateTime;
	 public String shortDateTime;
	 public String url;

}


