package org.mitre.svmp.events;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class ActivityServiceDetector extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub

//		Handler hndler = new Handler();
//		hndler.postDelayed(new Runnable() {
//
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//
//				ActivityManager am = (ActivityManager) ActivityServiceDetector.this.getSystemService(ACTIVITY_SERVICE);
//				List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
//				Log.d("topActivity", "CURRENT Activity ::" + taskInfo.get(0).topActivity.getClassName());
//				ComponentName componentInfo = taskInfo.get(0).topActivity;
//				componentInfo.getPackageName();
//
//				Handler hnd = new Handler();
//				hnd.postDelayed(new Runnable() {
//
//					@Override
//					public void run() {
//						// TODO Auto-generated method stub
//
//						ActivityManager am = (ActivityManager) ActivityServiceDetector.this
//								.getSystemService(ACTIVITY_SERVICE);
//						List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
//						Log.d("topActivity", "CURRENT Activity ::" + taskInfo.get(0).topActivity.getClassName());
//						ComponentName componentInfo = taskInfo.get(0).topActivity;
//						componentInfo.getPackageName();
//						Activity activity = ActivityServiceDetector.getActivity();
//					}
//				}, 10000);
//			}
//		}, 1000);
		
		new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override 
            public void run() {
            	
            	Activity activity = ActivityServiceDetector.getActivity();
            	
            	//View v =activity.findViewById(android.R.id.content);
            	View v= activity.getWindow().getDecorView();
            	
            	Bitmap b= getBitmapFromView(v);
            	
            	 //Save bitmap 
                String extr = Environment.getExternalStorageDirectory()+"/ovalFolder/";
                String fileName = "report.jpg";
                File myPath=null;
				try {
					myPath = createImageFile();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
                FileOutputStream fos = null;
                try { 
                    fos = new FileOutputStream(myPath);
                    b.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.flush();
                    fos.close();
                    MediaStore.Images.Media.insertImage(getApplicationContext().getContentResolver(), b, "Screen", "screen");
                }catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block 
                    e.printStackTrace();
                } catch (Exception e) {
                    // TODO Auto-generated catch block 
                    e.printStackTrace();
                } 
            	
            	Log.i("testing", "one second");
            	
            } 
        }, 0, 1000);

		return super.onStartCommand(intent, flags, startId);
	}
	
	private File createImageFile() throws IOException {
	    // Create an image file name 
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    String imageFileName = "JPEG_" + timeStamp + "_";
	    File storageDir = Environment.getExternalStoragePublicDirectory(
	            Environment.DIRECTORY_PICTURES);
	    File image = File.createTempFile(
	        imageFileName,  /* prefix */
	        ".jpg",         /* suffix */ 
	        storageDir      /* directory */
	    ); 
	 
	    // Save a file: path for use with ACTION_VIEW intents 
	  //  mCurrentPhotoPath = "file:" + image.getAbsolutePath();
	    return image;
	} 
	
	public static Bitmap getBitmapFromView(View view) {
        //Define a bitmap with the same size as the view 
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),Bitmap.Config.ARGB_8888);
        //Bind a canvas to it 
        Canvas canvas = new Canvas(returnedBitmap);
        //Get the view's background 
        Drawable bgDrawable =view.getBackground();
        if (bgDrawable!=null) 
            //has background drawable, then draw it on the canvas 
            bgDrawable.draw(canvas);
        else  
            //does not have background drawable, then draw white background on the canvas 
            canvas.drawColor(Color.WHITE);
        // draw the view on the canvas 
        view.draw(canvas);
        //return the bitmap 
        return returnedBitmap;
    } 

	public static Activity getActivity() {
		Class activityThreadClass;
		try {
			activityThreadClass = Class.forName("android.app.ActivityThread");

			Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
			Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
			activitiesField.setAccessible(true);
			ArrayMap activities = (ArrayMap) activitiesField.get(activityThread);
			for (Object activityRecord : activities.values()) {
				Class activityRecordClass = activityRecord.getClass();
				Field pausedField = activityRecordClass.getDeclaredField("paused");
				pausedField.setAccessible(true);
				if (!pausedField.getBoolean(activityRecord)) {
					Field activityField = activityRecordClass.getDeclaredField("activity");
					activityField.setAccessible(true);
					Activity activity = (Activity) activityField.get(activityRecord);
					return activity;
				}
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
	}

}
