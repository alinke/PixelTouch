/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.ioiomint.pixelpaint;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Random;

import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;

import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.SensorManager;
//import android.os.CountDownTimer;

//Need the following import to get access to the app resources, since this
//class is in a sub-package.


/**
 * Demonstrates the handling of touch screen and trackball events to
 * implement a simple painting app.
 */
public class TouchPaint extends IOIOActivity   {
	
	
	 private int columnIndex;   
		private ioio.lib.api.RgbLedMatrix.Matrix KIND;  //have to do it this way because there is a matrix library conflict
		private android.graphics.Matrix matrix2;
	    private static final String TAG = "PixelTouch";	  	
	  	private short[] frame_ = new short[512];
	  	public static final Bitmap.Config FAST_BITMAP_CONFIG = Bitmap.Config.RGB_565;
	  	private byte[] BitmapBytes;
	  	private InputStream BitmapInputStream;
	  	private Bitmap canvasBitmap;
	  	private Bitmap originalImage;
	  	private int width_original;
	  	private int height_original; 	  
	  	private float scaleWidth; 
	  	private float scaleHeight; 	  	
	  	private Bitmap resizedBitmap;  	
	  	private int deviceFound = 0;
	  	//private Handler mHandler;
	  	
	  	private SharedPreferences prefs;
		private String OKText;
		private Resources resources;
		private String app_ver;	
		private int matrix_model;
		private final String tag = "";	
		
		///********** Timers
		private ConnectTimer connectTimer; 	
		//private int size;  //the number of pictures
		private String imagePath;
		private boolean noSleep = false;	   
	    private int color;
	    private int penSize;
	    private OnAmbilWarnaListener listener;
	    private AmbilWarnaDialog colorDialog;
	
	
	
    /** Used as a pulse to gradually fade the contents of the window. */
    private static final int FADE_MSG = 1;
    
    /** Menu ID for the command to clear the window. */
    private static final int CLEAR_ID = Menu.FIRST;
    /** Menu ID for the command to toggle fading. */
    private static final int FADE_ID = Menu.FIRST+1;
    private static final int COLOR_ID = Menu.FIRST+2;
    
    
    /** How often to fade the contents of the window (in ms). */
    private int selected_fade = 3;
    private int FADE_DELAY = 180;
    
    /** The view responsible for drawing the window. */
    MyView mView;
    /** Is fading mode enabled? */
    boolean mFading;
    private int prefsColor;
    private Context context;
    private int selectedColor;
    private boolean prefFading;
    
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create and attach the view that is responsible for painting.
        mView = new MyView(this);
        setContentView(mView);
        mView.requestFocus();
      
        // Restore the fading option if we are being thawed from a
        // previously saved state.  Note that we are not currently remembering
        // the contents of the bitmap.
        mFading = savedInstanceState != null ? savedInstanceState.getBoolean("fading", true) : true;  
        
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        try
        {
            app_ver = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        }
        catch (NameNotFoundException e)
        {
            Log.v(tag, e.getMessage());
        }
        
        //******** preferences code
        resources = this.getResources();
        setPreferences();
        //***************************
        
        mFading = prefFading;
        
        connectTimer = new ConnectTimer(30000,5000); //pop up a message if it's not connected by this timer
 		connectTimer.start(); //this timer will pop up a message box if the device is not found
 		color = prefsColor; //color is what the touch is
 		
 		AmbilWarnaDialog(context, color, listener);	
	 	// initialColor is the initially-selected color to be shown in the rectangle on the left of the arrow.
	 	// for example, 0xff000000 is black, 0xff0000ff is blue. Please be aware of the initial 0xff which is the alpha.
	 	colorDialog = new AmbilWarnaDialog(this, prefsColor, new OnAmbilWarnaListener() {
	 	        @Override
	 	        public void onOk(AmbilWarnaDialog dialog, int selectedColor) {
	 	              color = selectedColor;
	 	        }
	 	                
	 	        @Override
	 	        public void onCancel(AmbilWarnaDialog dialog) {
	 	                // cancel was selected by the user
	 	        }
	 	});
	 	
	 	showToast(getString(R.string.startupInstructions));
 		
        
    }
    
    private void AmbilWarnaDialog(Context context2, int color2,
			OnAmbilWarnaListener listener2) {
		// TODO Auto-generated method stub
		
	}

	private void loadRGB565() {
 	   
		try {
   			int n = BitmapInputStream.read(BitmapBytes, 0, BitmapBytes.length); // reads
   																				// the
   																				// input
   																				// stream
   																				// into
   																				// a
   																				// byte
   																				// array
   			Arrays.fill(BitmapBytes, n, BitmapBytes.length, (byte) 0);
   		} catch (IOException e) {
   			e.printStackTrace();
   		}

   		int y = 0;
   		for (int i = 0; i < frame_.length; i++) {
   			frame_[i] = (short) (((short) BitmapBytes[y] & 0xFF) | (((short) BitmapBytes[y + 1] & 0xFF) << 8));
   			y = y + 2;
   		}
   }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
       MenuInflater inflater = getMenuInflater();
       inflater.inflate(R.menu.mainmenu, menu);
       menu.add(0, FADE_ID, 0, R.string.FadeMenuLabel).setCheckable(true);
       menu.add(0, COLOR_ID,0,R.string.ColorMenuLabel);
       menu.add(0, CLEAR_ID, 0, R.string.ClearMenuLabel);
       return true;
    }

    @Override public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(FADE_ID).setChecked(mFading);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
    	
    	AlertDialog.Builder alert=new AlertDialog.Builder(this);
    	
        switch (item.getItemId()) {
            case CLEAR_ID:
                mView.clear();
                return true;
            case FADE_ID:
                mFading = !mFading;
                if (mFading) {
                    startFading();
                } else {
                    stopFading();
                }
                return true;
            case COLOR_ID:
            	colorDialog.show();
            	return true;
            case R.id.menu_instructions:            	
     	      	alert.setTitle(R.string.setupInstructionsStringTitle).setIcon(R.drawable.icon).setMessage(R.string.setupInstructionsString).setNeutralButton(OKText, null).show();
     	      	return true;
            case R.id.menu_about:
    	      	alert.setTitle(getString(R.string.menu_about_title)).setIcon(R.drawable.icon).setMessage(getString(R.string.menu_about_summary) + "\n\n" + getString(R.string.versionString) + " " + app_ver).setNeutralButton(OKText, null).show();	
     	      	return true;  	
            case R.id.menu_prefs:
            	Intent intent = new Intent()
   				.setClass(this,
   						com.ioiomint.pixelpaint.preferences.class);   
				this.startActivityForResult(intent, 0);
            	return true;
        }	
        return true;
    }
    
    
    
    @SuppressLint("ParserError")
	@Override
    public void onActivityResult(int reqCode, int resCode, Intent data) //we'll go into a reset after this
    {
    	super.onActivityResult(reqCode, resCode, data);    	
    	setPreferences(); //very important to have this here, after the menu comes back this is called, we'll want to apply the new prefs without having to re-start the app
    	
    }   
    
    //@SuppressLint({ "ParserError", "ParserError" })
	@SuppressLint("ParserError")
	private void setPreferences() //here is where we read the shared preferences into variables
    {
     SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this); 
       
     noSleep = prefs.getBoolean("pref_noSleep", false);     
         
     matrix_model = Integer.valueOf(prefs.getString(   //the selected RGB LED Matrix Type
    	        resources.getString(R.string.selected_matrix),
    	        resources.getString(R.string.matrix_default_value))); 
     
     selected_fade = Integer.valueOf(prefs.getString(   //how fast to fade
 	        resources.getString(R.string.fade_speed_key),
 	        resources.getString(R.string.fade_speed_default_value))); 
     
     penSize = Integer.valueOf(prefs.getString(   //the selected RGB LED Matrix Type
 	        resources.getString(R.string.selected_penSize),
 	        resources.getString(R.string.penSize_default_value))); 
    	   
     prefsColor = prefs.getInt("pref_pickColor", 0xffff0000);     
     prefFading = prefs.getBoolean("pref_fading", false);
     
     switch (selected_fade) {  //get this from the preferences
     case 0:
    	 FADE_DELAY = 25;
    	 break;
     case 1:
    	 FADE_DELAY = 50;
    	 break;
     case 2:
    	 FADE_DELAY = 100;
    	 break;
     case 3:
    	 FADE_DELAY = 180;
    	 break;
     case 4:
    	 FADE_DELAY = 300;
    	 break;
     case 5:
    	 FADE_DELAY = 500;
    	 break;
     default:	    		 
    	 FADE_DELAY = 180;
 }
     
     switch (matrix_model) {  //get this from the preferences
	     case 0:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x16;
	    	 BitmapInputStream = getResources().openRawResource(R.raw.selectpic);
	    	 break;
	     case 1:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.ADAFRUIT_32x16;
	    	 BitmapInputStream = getResources().openRawResource(R.raw.selectpic);
	    	 break;
	     case 2:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x32; //v1
	    	 BitmapInputStream = getResources().openRawResource(R.raw.selectpic32);
	    	 break;
	     case 3:
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x32_NEW; //v2
	    	 BitmapInputStream = getResources().openRawResource(R.raw.selectpic32);
	    	 break;
	     default:	    		 
	    	 KIND = ioio.lib.api.RgbLedMatrix.Matrix.SEEEDSTUDIO_32x32_NEW; //v2 as the default
	    	 BitmapInputStream = getResources().openRawResource(R.raw.selectpic32);
     }
         
     frame_ = new short [KIND.width * KIND.height];
	 BitmapBytes = new byte[KIND.width * KIND.height *2]; //512 * 2 = 1024 or 1024 * 2 = 2048
	 
	 loadRGB565(); //this function loads a raw RGB565 image to the matrix
     
     
 }

    @Override protected void onResume() {
        super.onResume();
        // If fading mode is enabled, then as long as we are resumed we want
        // to run pulse to fade the contents.
        if (mFading) {
            startFading();
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save away the fading state to restore if needed later.  Note that
        // we do not currently save the contents of the display.
        outState.putBoolean("fading", mFading);
    }

    @Override protected void onPause() {
        super.onPause();
        // Make sure to never run the fading pulse while we are paused or
        // stopped.
        stopFading();
    }

    /**
     * Start up the pulse to fade the screen, clearing any existing pulse to
     * ensure that we don't have multiple pulses running at a time.
     */
    void startFading() {
        mHandler.removeMessages(FADE_MSG);
        mHandler.sendMessageDelayed(
                mHandler.obtainMessage(FADE_MSG), FADE_DELAY);
    }
    
    /**
     * Stop the pulse to fade the screen.
     */
    void stopFading() {
        mHandler.removeMessages(FADE_MSG);
    }
    
    private Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                // Upon receiving the fade pulse, we have the view perform a
                // fade and then enqueue a new message to pulse at the desired
                // next time.
                case FADE_MSG: {
                    mView.fade();
                    mHandler.sendMessageDelayed(
                            mHandler.obtainMessage(FADE_MSG), FADE_DELAY);
                    break;
                }
                default:
                    super.handleMessage(msg);
            }
        }
    };
    
    private void WriteImagetoMatrix() {  //here we'll take a PNG, BMP, or whatever and convert it to RGB565 via a canvas, also we'll re-size the image if necessary
    	
	     originalImage = BitmapFactory.decodeFile(imagePath);   		 
		 width_original = originalImage.getWidth();
		 height_original = originalImage.getHeight();
		 scaleWidth = ((float) KIND.width) / width_original;
	 	 scaleHeight = ((float) KIND.height) / height_original;
   		 // create matrix for the manipulation
   		 matrix2 = new Matrix();
   		 // resize the bit map
   		 matrix2.postScale(scaleWidth, scaleHeight);
   		 resizedBitmap = Bitmap.createBitmap(originalImage, 0, 0, width_original, height_original, matrix2, true);
   		 canvasBitmap = Bitmap.createBitmap(KIND.width, KIND.height, Config.RGB_565); 
   		 Canvas canvas = new Canvas(canvasBitmap);
   		 canvas.drawRGB(0,0,0); //a black background
   	   	 canvas.drawBitmap(resizedBitmap, 0, 0, null);
   		 ByteBuffer buffer = ByteBuffer.allocate(KIND.width * KIND.height *2); //Create a new buffer
   		 canvasBitmap.copyPixelsToBuffer(buffer); //copy the bitmap 565 to the buffer		
   		 BitmapBytes = buffer.array(); //copy the buffer into the type array
		
}
    
    class IOIOThread extends BaseIOIOLooper {
  		private ioio.lib.api.RgbLedMatrix matrix_;

  		@Override
  		protected void setup() throws ConnectionLostException {
  			matrix_ = ioio_.openRgbLedMatrix(KIND);
  			deviceFound = 1; //if we went here, then we are connected over bluetooth or USB
  			connectTimer.cancel(); //we can stop this since it was found
  		}

  		@Override
  		public void loop() throws ConnectionLostException {
  		
  			matrix_.frame(frame_); //writes whatever is in bitmap raw 565 file buffer to the RGB LCD
  					
  			}	
  		}

  	@Override
  	protected IOIOLooper createIOIOLooper() {
  		return new IOIOThread();
  	}
    
    private void showToast(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast toast = Toast.makeText(TouchPaint.this, msg, Toast.LENGTH_LONG);
                toast.show();
			}
		});
	}  
    
    private void showToastShort(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast toast = Toast.makeText(TouchPaint.this, msg, Toast.LENGTH_SHORT);
                toast.show();
			}
		});
	} 
    
    public class ConnectTimer extends CountDownTimer
   	{

   		public ConnectTimer(long startTime, long interval)
   			{
   				super(startTime, interval);
   			}

   		@Override
   		public void onFinish()
   			{
   				if (deviceFound == 0) {
   					showNotFound(); 					
   				}
   				
   			}

   		@Override
   		public void onTick(long millisUntilFinished)				{
   			//not used
   		}
   	}
    
    private void showNotFound() {	
		AlertDialog.Builder alert=new AlertDialog.Builder(this);
		alert.setTitle(getResources().getString(R.string.notFoundString)).setIcon(R.drawable.icon).setMessage(getResources().getString(R.string.bluetoothPairingString)).setNeutralButton(getResources().getString(R.string.OKText), null).show();	
}
    
    public class MyView extends View {
        private static final int FADE_ALPHA = 0x06;
        private static final int MAX_FADE_STEPS = 256/FADE_ALPHA + 4;
        private Bitmap mBitmap;
        private Canvas mCanvas;
        private final Rect mRect = new Rect();
        private final Paint mPaint;
        private final Paint mFadePaint;
        private boolean mCurDown;
        private int mCurX;
        private int mCurY;
        private float mCurPressure;
        private float mCurSize;
        private int mCurWidth;
        private int mFadeSteps = MAX_FADE_STEPS;
        
        public MyView(Context c) {
            super(c);
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setARGB(255, 255, 255, 255);
            
          //  Button myButton = new Button(c);
        //    myButton.setText("Push Me");
         
            //LinearLayout ll = (LinearLayout)findViewById(R.id.buttonlayout);
           // LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            //MyView.addView(myButton, lp);
           // MyView.addView(myButton);
            //MyView.addView(myButton);
            mFadePaint = new Paint();
            mFadePaint.setDither(true);
            mFadePaint.setARGB(FADE_ALPHA, 0, 0, 0);
        }

        public void clear() {
            if (mCanvas != null) {
                mPaint.setARGB(0xff, 0, 0, 0);
                mCanvas.drawPaint(mPaint);
                invalidate();
                mFadeSteps = MAX_FADE_STEPS;
            }
        }
        
        public void fade() {
            if (mCanvas != null && mFadeSteps < MAX_FADE_STEPS) {
                mCanvas.drawPaint(mFadePaint);
                invalidate();
                mFadeSteps++;
            }
        }
        
        @Override protected void onSizeChanged(int w, int h, int oldw,
                int oldh) {
        	
            int curW = mBitmap != null ? mBitmap.getWidth() : 0;
            int curH = mBitmap != null ? mBitmap.getHeight() : 0;
            if (curW >= w && curH >= h) {
                return;
            }
            
            if (curW < w) curW = w;
            if (curH < h) curH = h;
            
            Bitmap newBitmap = Bitmap.createBitmap(curW, curH, Bitmap.Config.RGB_565);
           // Bitmap newBitmap = Bitmap.createBitmap(KIND.width*30, KIND.height*30, Bitmap.Config.RGB_565);            
         //  Bitmap newBitmap = Bitmap.createBitmap(500, 500,Bitmap.Config.RGB_565);
            
            Canvas newCanvas = new Canvas();
            newCanvas.setBitmap(newBitmap);
            if (mBitmap != null) {
                newCanvas.drawBitmap(mBitmap, 0, 0, null);
            }
            mBitmap = newBitmap;
            mCanvas = newCanvas;
            mFadeSteps = MAX_FADE_STEPS;  
            
        }
        
        public void loadImage() {

          		int y = 0;
          		for (int i = 0; i < frame_.length; i++) {
          			frame_[i] = (short) (((short) BitmapBytes[y] & 0xFF) | (((short) BitmapBytes[y + 1] & 0xFF) << 8));
          			y = y + 2;
          		}
          	}
        
        @Override protected void onDraw(Canvas canvas) {
            if (mBitmap != null) {
                 canvas.drawBitmap(mBitmap, 0, 0, null);	 
       		 	 width_original = mBitmap.getWidth();
       		 	 height_original = mBitmap.getHeight();
                 scaleWidth = ((float) KIND.width) / width_original;
         		 scaleHeight = ((float) KIND.height) / height_original;
      	   		 // create matrix for the manipulation
      	   		 matrix2 = new Matrix();
      	   		 // resize the bit map
      	   		 matrix2.postScale(scaleWidth, scaleHeight);
      	   		 resizedBitmap = Bitmap.createBitmap(mBitmap, 0, 0, width_original, height_original, matrix2, true);
      	   		 canvasBitmap = Bitmap.createBitmap(KIND.width, KIND.height, Config.RGB_565); 
      	   		 canvas = new Canvas(canvasBitmap);
      	   		 //canvas.drawRGB(0,0,0); //a black background
      	   	   	 canvas.drawBitmap(resizedBitmap, 0, 0, null);
      	   	   	 canvas.rotate(90);
      	   		 ByteBuffer buffer = ByteBuffer.allocate(KIND.width * KIND.height *2); //Create a new buffer
      	   		 canvasBitmap.copyPixelsToBuffer(buffer); //copy the bitmap 565 to the buffer		
      	   		 BitmapBytes = buffer.array(); //copy the buffer into the type array
      	   		 
      	   		 loadImage();  
            }
        }

        @Override public boolean onTrackballEvent(MotionEvent event) {
            boolean oldDown = mCurDown;
            mCurDown = true;
            int N = event.getHistorySize();
            int baseX = mCurX;
            int baseY = mCurY;
            final float scaleX = event.getXPrecision();
            final float scaleY = event.getYPrecision();
            for (int i=0; i<N; i++) {
                //Log.i("TouchPaint", "Intermediate trackball #" + i
                //        + ": x=" + event.getHistoricalX(i)
                //        + ", y=" + event.getHistoricalY(i));
                drawPoint(baseX+event.getHistoricalX(i)*scaleX,
                        baseY+event.getHistoricalY(i)*scaleY,
                        event.getHistoricalPressure(i),
                        event.getHistoricalSize(i));
            }
            //Log.i("TouchPaint", "Trackball: x=" + event.getX()
            //        + ", y=" + event.getY());
            drawPoint(baseX+event.getX()*scaleX, baseY+event.getY()*scaleY,
                    event.getPressure(), event.getSize());
            mCurDown = oldDown;
            return true;
        }
        
        @Override public boolean onTouchEvent(MotionEvent event) {
            int action = event.getAction();
            mCurDown = action == MotionEvent.ACTION_DOWN
                    || action == MotionEvent.ACTION_MOVE;
            int N = event.getHistorySize();
            for (int i=0; i<N; i++) {
                //Log.i("TouchPaint", "Intermediate pointer #" + i);
                drawPoint(event.getHistoricalX(i), event.getHistoricalY(i),
                        event.getHistoricalPressure(i),
                        event.getHistoricalSize(i));   
                
                //drawPoint(event.getHistoricalX(i), event.getHistoricalY(i),
                  //      event.getHistoricalPressure(i),
                    //    100);   
            }
            drawPoint(event.getX(), event.getY(), event.getPressure(),
                    event.getSize());
            
           /// drawPoint(event.getX(), event.getY(), event.getPressure(),
              //      100);
            return true;
        }
        
        private void drawPoint(float x, float y, float pressure, float size) {
            //Log.i("TouchPaint", "Drawing: " + x + "x" + y + " p="
            //        + pressure + " s=" + size);
            mCurX = (int)x;
            mCurY = (int)y;
            mCurPressure = pressure;
            mCurSize = size/penSize; //for some reason, the pen size is different on different devices so helps to have this, user can change from preferences
            mCurWidth = (int)(mCurSize*(getWidth()/3));
            if (mCurWidth < 1) mCurWidth = 1;
            if (mCurDown && mBitmap != null) {
                int pressureLevel = (int)(mCurPressure*255);
                mPaint.setARGB(pressureLevel, 255, 255, 255);
                mPaint.setColor(color);
                mCanvas.drawCircle(mCurX, mCurY, mCurWidth, mPaint);
                mRect.set(mCurX-mCurWidth-2, mCurY-mCurWidth-2,
                        mCurX+mCurWidth+2, mCurY+mCurWidth+2);
                invalidate(mRect);
            }
            mFadeSteps = 0;
        }
    }
}
