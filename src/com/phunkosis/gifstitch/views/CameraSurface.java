/*
 *  GifStitch - Android App
 *  Copyright (C) 2012 Nick Thuesen
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.phunkosis.gifstitch.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.phunkosis.gifstitch.R;
import com.phunkosis.gifstitch.dialogs.DialogHelper;
import com.phunkosis.gifstitch.settings.GSSettings;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;


/************************************************************************
 * I got annoyed trying to deal with camera, surface, activity...blah blah blah
 * all in one place.  So this guy just handles surface and camera nonsense
 * and provides basic hooks for whomever needs it.  A lot of this I learned 
 * (after solo first attempt) from reading the camera source.
************************************************************************/


public class CameraSurface extends LinearLayout implements SurfaceHolder.Callback{
	private static final String TAG = "CameraSurface";
	
    //private static final float DEFAULT_CAMERA_BRIGHTNESS = 0.7f;
	private static final String ENABLEDAUTOFOCUSSETTING = Camera.Parameters.FOCUS_MODE_AUTO;
	private static final String DISABLEDAUTOFOCUSSETTING = Camera.Parameters.FOCUS_MODE_INFINITY;
	private static final float DEFAULT_CAMERA_BRIGHTNESS = 1.0f;
	private static final int JPEGQUALITY = 100;
	
	private Parameters mParameters;
	
    private Activity mActivity;
    private Camera mCameraDevice;
	private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder = null;
    private boolean mFocusing;
    private boolean mCapturing;
    private boolean mPreviewing;
    private boolean mPausing;
    
	private Camera.PictureCallback mPictureCallbackListener;
    private Camera.ShutterCallback mShutterCallbackListener;
    
    private final ErrorCallback mErrorCallback = new ErrorCallback();
    private final RawPictureCallback mRawPictureCallback = new RawPictureCallback();
    private final JpegPictureCallback mJpgPictureCallback = new JpegPictureCallback();
    private final AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback();
    private final ShutterCallback mShutterCallback = new ShutterCallback();
    

	public CameraSurface(Context context){
		this(context, null);
	}
	
	public CameraSurface(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.mActivity = (Activity)context;
		
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.view_camerasurface, this);
		
		this.initializeScreenBrightness();
		this.mSurfaceView = (SurfaceView) this.findViewById(R.id.camera_preview);
        this.mSurfaceHolder = mSurfaceView.getHolder();
        this.mSurfaceHolder.addCallback(this);
        this.mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		this.startPreview();		
	}
	
	public void pause(){
		this.mPausing = true;
		this.stopPreview();
		this.closeCamera();
	}
	
	public void resume(){
		this.mPausing = false;
		if(!this.mPreviewing) this.startPreview();
	}
	
	public void setAutoFocusEnabled(boolean enabled){
		String focusmode = enabled ? ENABLEDAUTOFOCUSSETTING : DISABLEDAUTOFOCUSSETTING;
		if(this.mParameters.getFocusMode() != focusmode && this.isSupported(focusmode, this.mParameters.getSupportedFocusModes())){
			this.mParameters.setFocusMode(focusmode);
			this.mCameraDevice.setParameters(this.mParameters);
		}
	}
	
	public void setColorEffect(String colorEffect){
		if(this.mCameraDevice != null && this.isSupported(colorEffect, this.mParameters.getSupportedColorEffects())){
			this.mParameters.setColorEffect(colorEffect);
			this.mCameraDevice.setParameters(this.mParameters);
		}
	}
	
	public List<String> getSupportedEffects(){
		if(this.mParameters != null)
			return this.mParameters.getSupportedColorEffects();
		return new ArrayList<String>();
	}
	
    public void setPictureCallbackListener(Camera.PictureCallback listener) {
		this.mPictureCallbackListener = listener;
	}

	public void setShutterCallbackListener(Camera.ShutterCallback listener) {
		this.mShutterCallbackListener = listener;
	}
	
	
	public void startCapture(){
		if(this.mCapturing || this.mFocusing || !this.mPreviewing) return;
		if(GSSettings.getShouldAutoFocus())
			doFocus();
		else
			doSnap();
	}
	
	private void doFocus(){
		if(this.mPreviewing && !this.mFocusing && !this.mCapturing){
			this.mFocusing = true;
			this.mCameraDevice.autoFocus(this.mAutoFocusCallback);
		}
	}
	
	private void doSnap(){
		if(this.mCameraDevice != null && this.mPreviewing && !this.mFocusing && !this.mCapturing){
			this.mCapturing = true;
			this.mPreviewing = false;
			//moved shutcallback here since it works better for flash...should rename prob
			if(mShutterCallbackListener != null)
        		mShutterCallbackListener.onShutter();
			this.mCameraDevice.takePicture(this.mShutterCallback, this.mRawPictureCallback, this.mJpgPictureCallback);
			
		}
	}
	
	private void initializeScreenBrightness() {
        Window win = this.mActivity.getWindow();
        // Overright the brightness settings if it is automatic
        int mode = Settings.System.getInt(
        		this.getContext().getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        if(mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            WindowManager.LayoutParams winParams = win.getAttributes();
            winParams.screenBrightness = DEFAULT_CAMERA_BRIGHTNESS;
            win.setAttributes(winParams);
        }
    }
	
	private void restartPreview() {
        //In case I ever need a restart instead of just start.
        startPreview();
    }
	
	private void stopPreview() {
        if (this.mCameraDevice != null && this.mPreviewing) {
            Log.v(TAG, "stopPreview");
            this.mCameraDevice.stopPreview();
        }
        this.mPreviewing = false;
    }
	
	private void startPreview(){
		try {
			if(this.mActivity.isFinishing()) return;
			this.ensureCameraDevice();
			if(this.mPreviewing) this.stopPreview();
			this.setPreviewDisplay(this.mSurfaceHolder);
			this.setCameraParameters();
			this.mCameraDevice.setErrorCallback(mErrorCallback);
			try {
	            Log.v(TAG, "startPreview");
	            this.mCameraDevice.startPreview();
	            /************************************************
		         * Hack for when preview was technically started (but
		         * probably not all the way started) and autofocus was 
		         * crashing the camera hardware.That or I wrote crappy
		         * code somewhere.  Super rare crash but the worst kind.
		         *************************************************/
		        Thread.sleep(100);
	        } catch (Throwable ex) {
	            this.closeCamera();
	            throw new RuntimeException("startPreview failed", ex);
	        }
	        
	        
	        this.mPreviewing = true;
	        this.mCapturing = false;
	        
	        
		} catch (Exception e) {
			DialogHelper.displayFatalCameraError(this.mActivity);
	    }
	}
	
	private void setCameraParameters(){
    	this.mParameters = this.mCameraDevice.getParameters();
    	
    	List<Integer> frameRates = this.mParameters.getSupportedPreviewFrameRates();
    	if (frameRates != null) {
            Integer max = Collections.max(frameRates);
            this.mParameters.setPreviewFrameRate(max);
        }
    	
    	//If I understand this correctly all camera sizes are landscape and than
		//a rotation is applied.  So we just need to find the optimal landscape size.
		//That's if I understand correctly...
		//Basically we're trying to get a decent but small picture size for faster image capturing.
    	
    	double ratio = (double)8/3;
    	Display display = this.mActivity.getWindowManager().getDefaultDisplay();
    	if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
    		ratio = (double)display.getWidth() / display.getHeight();
    	else
    		ratio = (double)display.getHeight() / display.getWidth();
    	
		int optPicWidth = (int)this.getResources().getDimension(R.dimen.camera_pic_size);
		int optPicHeight = (int) (optPicWidth / ratio);
		
		Size optPicSize = this.getOptimalSize(this.mParameters.getSupportedPictureSizes(), optPicWidth, optPicHeight);
		Size optPreviewSize = this.getOptimalSize(this.mParameters.getSupportedPreviewSizes(), optPicSize.width, optPicSize.height);
		
		this.mParameters.setPictureSize(optPicSize.width, optPicSize.height);
		this.mParameters.setPreviewSize(optPreviewSize.width, optPreviewSize.height);
		this.mParameters.setPictureFormat(ImageFormat.JPEG);
        this.mParameters.setJpegQuality(JPEGQUALITY);
    	
        // Set the preview frame aspect ratio according to the picture size.
		//PreviewFrameLayout frameLayout = (PreviewFrameLayout) findViewById(R.id.frame_layout);
        if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
        	this.mParameters.setRotation(0);
        	//frameLayout.setAspectRatio((double) optPicSize.width / optPicSize.height);
        }
		else{
			this.mParameters.setRotation(90);
			//frameLayout.setAspectRatio((double) optPicSize.height / optPicSize.width);
		}
        
        String focusmode = GSSettings.getShouldAutoFocus() ? ENABLEDAUTOFOCUSSETTING : DISABLEDAUTOFOCUSSETTING;
		if(this.isSupported(focusmode, this.mParameters.getSupportedFocusModes()))
			this.mParameters.setFocusMode(focusmode);
		
		//Do I care about exposure?
        this.mCameraDevice.setParameters(this.mParameters);
    }
	
	
	private void setPreviewDisplay(SurfaceHolder holder) {
        try {
        	this.mCameraDevice.setPreviewDisplay(holder);
        } catch (Throwable ex) {
        	this.closeCamera();
            throw new RuntimeException("setPreviewDisplay failed", ex);
        }
    }

    private void ensureCameraDevice(){
        if (this.mCameraDevice == null) {
        	this.mCameraDevice = Camera.open();
        	if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
				this.mCameraDevice.setDisplayOrientation(0);
			else
				this.mCameraDevice.setDisplayOrientation(90);
        }
    }
    
    private void closeCamera() {
        if (this.mCameraDevice != null) {
        	this.mCameraDevice.release();
            this.mCameraDevice = null;
            this.mPreviewing = false;
        }
    }

    
    private boolean isSupported(String value, List<String> supported) {
        return supported == null ? false : supported.indexOf(value) >= 0;
    }
    
    
    /*
	 * SURFACE HANDLER STUFF
	 */
    
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		if(holder.getSurface() == null) {
            Log.d(TAG, "holder.getSurface() == null");
            return;
        }
		this.mSurfaceHolder = holder;
		
		if(this.mCameraDevice == null) return;
		if(this.mActivity.isFinishing()) return;
		
		if(this.mPreviewing && holder.isCreating())
			this.setPreviewDisplay(holder);
		else
            restartPreview();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		this.stopPreview();
        this.mSurfaceHolder = null;
        this.closeCamera();
	}    
    
	
	/*
	 * CAMERA CALLBACK STUFF
	 */
    
    private static final class ErrorCallback implements Camera.ErrorCallback {
	    public void onError(int error, Camera camera) {
	        if (error == android.hardware.Camera.CAMERA_ERROR_SERVER_DIED) {
	             Log.v(TAG, "media server died");
	        }
	    }   
	}
    
    private final class ShutterCallback implements Camera.ShutterCallback {
        public void onShutter() {}
    }
    
    private final class RawPictureCallback implements PictureCallback {
        public void onPictureTaken(byte [] rawData, android.hardware.Camera camera) {}}
    
    
    private final class JpegPictureCallback implements PictureCallback {
    	public void onPictureTaken(final byte [] jpegData, final Camera camera) {
    		if (mPausing) return;
    		if(mPictureCallbackListener != null)
    			mPictureCallbackListener.onPictureTaken(jpegData, camera);
    		Log.d(TAG, "CAPT FINISH ");
    		restartPreview();
    	}
    }
    
    private final class AutoFocusCallback implements Camera.AutoFocusCallback {
        public void onAutoFocus(boolean focused, android.hardware.Camera camera) {
        	//Don't care if successful.  Take the picture.
        	mFocusing = false;
        	Log.d(TAG, "FOCUS FINISH " + focused);
        	doSnap();
        }
    }
    
    
    private Size getOptimalSize(List<Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.0;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = Math.min(h, w);

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
    
}
