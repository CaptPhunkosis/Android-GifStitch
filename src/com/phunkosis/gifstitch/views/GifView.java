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

import com.phunkosis.gifstitch.R;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.webkit.WebView;

public class GifView extends WebView {
	private static final String TAG = "GifView";
	
	private GestureDetector mGestureDetector;
	private int mImageHeight = 0;
	private int mImageWidth = 0;
	private String mGifPath;
	
	
	public GifView(Context context){
		this(context, null);
	}
	
	public GifView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setVerticalScrollBarEnabled(false);
		this.setHorizontalScrollBarEnabled(false);
	}
	
	public String getGifPath(){
		return this.mGifPath;
	}

	//Long clicks weren't registering...not sure if this is
	//proper work around...but it works sooooo...yeah.
	SimpleOnGestureListener mGestListener = new SimpleOnGestureListener() {
		public boolean onDown(MotionEvent event) {return true;}
		public void onLongPress(MotionEvent event) {
			GifView.this.performLongClick();}
		};
	
	
	public void loadGif(String gifPath){
		BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
    	BitmapFactory.decodeFile(gifPath, o);
        
    	this.mGestureDetector = new GestureDetector(mGestListener);
    	
		this.mImageWidth = o.outWidth;
		this.mImageHeight = o.outHeight;
		Log.d(TAG, "Loading " + gifPath);
		this.loadUrl("file://"+gifPath);
		this.mGifPath = gifPath;
	}
	
	public void unloadGif(){
		this.clearHistory();
		this.loadUrl("");
	}
	
	@Override
    public boolean onTouchEvent(MotionEvent event) {
        return this.mGestureDetector.onTouchEvent(event);
    }

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		float parent_width = (float)MeasureSpec.getSize(widthMeasureSpec);
	    float parent_height = (float)MeasureSpec.getSize(heightMeasureSpec);
	    
	    
	    float viewScale = this.getResources().getDimension(R.dimen.gif_view_scale);
	    DisplayMetrics dm = this.getResources().getDisplayMetrics();
	    viewScale = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, dm) * viewScale;
	    float scaleImgWidth = this.mImageWidth * viewScale;
	    float scaleImgHeight = this.mImageHeight * viewScale;
	    int initialScale = (int)(100*viewScale);
	    
	    //For zoom
	    float origImgWidth = scaleImgWidth;
	    float origImgHeight = scaleImgHeight;
	    
	    if(parent_width < scaleImgWidth && scaleImgWidth > 0){
	    	initialScale = (int)(viewScale * 100 * (parent_width/origImgWidth));
	    	scaleImgHeight = (int)(scaleImgHeight * ((double) parent_width / scaleImgWidth));
	    	scaleImgWidth = parent_width;
	    }
	    
	    if(parent_height < scaleImgHeight && scaleImgHeight > 0){
	    	initialScale = (int)(viewScale * 100 * (parent_height/origImgHeight));
	    	scaleImgWidth = (int)(scaleImgWidth * ((double) parent_height / scaleImgHeight));
	    	scaleImgHeight = parent_height;
	    }
	    
    	this.setInitialScale(initialScale);
    	this.setMeasuredDimension((int)scaleImgWidth, (int)scaleImgHeight);
    	
	    
	}
}
