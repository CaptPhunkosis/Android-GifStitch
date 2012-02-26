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
package com.phunkosis.gifstitch;

import java.util.List;

import com.phunkosis.gifstitch.dialogs.DialogHelper;
import com.phunkosis.gifstitch.encoder.EncodeGifTask;
import com.phunkosis.gifstitch.helpers.ImageHelper;
import com.phunkosis.gifstitch.helpers.Utils;
import com.phunkosis.gifstitch.helpers.VariableAnimationDrawable;
import com.phunkosis.gifstitch.settings.GSSettings;
import com.phunkosis.gifstitch.settings.GSSettings.GIFOUTPUTSIZE;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class PreviewActivity extends Activity implements OnSeekBarChangeListener, OnCheckedChangeListener{
	
	private SavedState mSS;
	private ProgressDialog mProgressDialog;
	private List<String> mFrames;
	private ViewGroup mPreviewHolder;
	private ImageView mPreviewImage;
	private VariableAnimationDrawable mFrameAnimation;
	private int mFirstFrameHeight = 0;
	private int mFirstFrameWidth = 0;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    this.setContentView(R.layout.activity_preview);
	    final SavedState ss = (SavedState)this.getLastNonConfigurationInstance();
		if(ss != null)
			this.mSS = ss;
		else{
			this.mSS = new SavedState();
		}
	    
	    Bundle extras = this.getIntent().getExtras();
	    this.mFrames = extras.getStringArrayList("gifframes");
	    if(this.mFrames == null || this.mFrames.size() <= 0)
	    	this.finish();
	    //So I'm paranoid about memory...and you wait a quarter second...boohoo.
	    if(this.mFrames.size() >= 20)
	    	Utils.memoryPanic();
	    
	    this.mPreviewHolder = (ViewGroup) this.findViewById(R.id.frame_preview_holder);
	    this.mPreviewImage = (ImageView) this.findViewById(R.id.preview_image);
	    this.mFrameAnimation = new VariableAnimationDrawable();
	    this.mFrameAnimation.setDuration(GSSettings.getGifFrameDelay());
	    
	    
	    SeekBar durationBar = (SeekBar)this.findViewById(R.id.sb_frame_duration);
	    durationBar.setProgress(this.convertDurationToSliderProgress(GSSettings.getGifFrameDelay()));
	    durationBar.setOnSeekBarChangeListener(this);
	    
	    
	    RadioGroup sizeRG = (RadioGroup)this.findViewById(R.id.radiogroup_gif_size);
	    sizeRG.setOnCheckedChangeListener(this);
	    
	    int sizeRadioId; 
	    switch(GSSettings.getGifOutputSize()){
		case LARGE:
			sizeRadioId = R.id.radio_gif_large;
			break;
		case MEDIUM:
			sizeRadioId = R.id.radio_gif_med;
			break;
		default:
			sizeRadioId = R.id.radio_gif_small;
		}
		((RadioButton)this.findViewById(sizeRadioId)).setChecked(true);
		
		
		Button buildButton = (Button)this.findViewById(R.id.button_build);
		buildButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				buildAndSaveGif();
			}});
		
		Button backButton = (Button)this.findViewById(R.id.button_nav_back);
	    backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {PreviewActivity.this.finish();}
		});
		
		
		if(this.mSS.isEncoding){
	    	this.showEncodingProgressDialog();
	    	if(this.mSS.createTask != null)
	    		this.mSS.createTask.attach(this);
		}else{
			
			//Another memory management attempt.
			int previewLoadSize = GIFOUTPUTSIZE.LARGE.size();
			if(this.mFrames.size() > 20)
				previewLoadSize = GIFOUTPUTSIZE.SMALL.size();
			else if(this.mFrames.size() > 6)
				previewLoadSize = GIFOUTPUTSIZE.MEDIUM.size();
			
			for(String frame: this.mFrames){
		    	try{
		    		Bitmap b = ImageHelper.getDesirableBitmap(frame, previewLoadSize);
			    	if(this.mFrames.indexOf(frame) == 0){
			    		this.mFirstFrameHeight = b.getHeight();
			    		this.mFirstFrameWidth = b.getWidth();
			    	}
			    	if(b != null)
			    		this.mFrameAnimation.addFrame(new BitmapDrawable(b), GSSettings.getGifFrameDelay());
		    	}catch(Exception ex){
		    		ex.printStackTrace();
		    	}
		    }
		    
		    this.mPreviewImage.setImageDrawable(this.mFrameAnimation);
			
		    this.mPreviewImage.post(new Runnable() {
				@Override
				public void run() {
					updatePreviewSize();
					if(mFrameAnimation != null)
						mFrameAnimation.start();
				}
			});
		    DialogHelper.showHint(this, this.getResources().getString(R.string.hint_preview), false);
		}
		
	}
	
	private void updatePreviewSize(){
		DisplayMetrics dm = this.getResources().getDisplayMetrics();
		float viewScale = this.getResources().getDimension(R.dimen.gif_view_scale);
	    viewScale = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, dm) * viewScale;
		int newWidth = (int)(GSSettings.getGifOutputSize().size() * viewScale);
		int newHeight = newWidth;
		
		if(this.mFirstFrameWidth > this.mFirstFrameHeight)
			newHeight = (int)(this.mFirstFrameHeight * ((double) newWidth / this.mFirstFrameWidth));
		else
			newWidth = (int)(this.mFirstFrameWidth * ((double) newHeight / this.mFirstFrameHeight));
		
		if(this.mPreviewHolder.getWidth() < newWidth){
			newHeight = (int)(newHeight * ((double) this.mPreviewHolder.getWidth() / newWidth));
			newWidth = this.mPreviewHolder.getWidth();
		}
		
		if(this.mPreviewHolder.getHeight() < newHeight){
			newWidth = (int)(newWidth * ((double) this.mPreviewHolder.getHeight() / newHeight));
			newHeight = this.mPreviewHolder.getHeight();
		}
		
		LayoutParams params = this.mPreviewImage.getLayoutParams();
		params.height = newHeight;
		params.width = newWidth;
		this.mPreviewImage.setLayoutParams(params);
		this.mPreviewImage.invalidate();
	}
	
	
	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		GIFOUTPUTSIZE newSize;
		switch(checkedId){
		case R.id.radio_gif_large:
			newSize = GIFOUTPUTSIZE.LARGE;
			break;
		case R.id.radio_gif_med:
			newSize = GIFOUTPUTSIZE.MEDIUM;
			break;
		default:
			newSize = GIFOUTPUTSIZE.SMALL;
			break;
		}
		
		if(newSize != GSSettings.getGifOutputSize()){
			GSSettings.setGifOutputSize(newSize);
			this.updatePreviewSize();
		}
		
	}
	
	private int convertSliderProgressToDuration(int progress){
		if(progress <= 10)
			return (int)Utils.translateRanges((float)progress, 0.0f, 10.0f, 1000.0f, 500.0f);
		//else if(progress <= 20)
		//	return (int)Utils.translateRanges((float)progress, 10.0f, 20.0f, 750.0f, 500.0f);
		else
			return (int)Utils.translateRanges((float)progress, 10.0f, 100.0f, 500.0f, 50.0f);
	}
	
	private int convertDurationToSliderProgress(int duration){
		if(duration >= 500)
			return (int)Utils.translateRanges((float)duration, 1000.0f, 500.0f, 0.0f, 10.0f);
		//else if(duration >= 500)
			//return (int)Utils.translateRanges((float)duration, 750.0f, 500.0f, 10.0f, 20.0f);
		else
			return (int)Utils.translateRanges((float)duration, 500.0f, 50.0f, 10.0f, 100.0f);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		int duration = convertSliderProgressToDuration(seekBar.getProgress());
		if(this.mFrameAnimation != null)
			this.mFrameAnimation.setDuration(duration);
	}
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		int duration = convertSliderProgressToDuration(seekBar.getProgress());
		GSSettings.setGifFrameDelay(duration);
		if(this.mFrameAnimation != null)
			this.mFrameAnimation.setDuration(duration);
	}
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {}

	
	
	/*
	 * ENCODING STUFF
	 */
	
	@SuppressWarnings("unchecked")
	private void buildAndSaveGif(){
		//Animation is memory intensive...so try getting it cleaned up.
		this.mPreviewImage.setImageBitmap(null);
		this.mPreviewImage = null;
		this.mFrameAnimation = null;
		
		mSS.createTask = new EncodeGifTask(PreviewActivity.this);
		mSS.createTask.execute(mFrames);
		mSS.isEncoding = true;
		showEncodingProgressDialog();
	}

	private void showEncodingProgressDialog(){
		this.mProgressDialog = DialogHelper.getEncodingProgressDialog(this);
		this.mProgressDialog.setMax(this.mFrames.size());
		this.mProgressDialog.show();
	}
	
	public void updateEncoderProgress(int value){
		if(this.mProgressDialog != null)
			this.mProgressDialog.setProgress(value);
	}
	
	public void finishedDecodingGif(String newGifPath){
		if(this.mProgressDialog != null)
			this.mProgressDialog.dismiss();
		this.mSS.isEncoding = false;
		this.mSS.createTask = null;
		Long gifsMade = GSSettings.getGifCreateCount() + 1;
		GSSettings.setGifCreateCount(gifsMade);
		if(newGifPath != null)
			this.viewNewGif(newGifPath);
	}
	
	private void viewNewGif(String newGifPath){
		Intent viewGifIntent = new Intent(this, ViewGifActivity.class);
		viewGifIntent.putExtra("gifPath", newGifPath);
		this.startActivity(viewGifIntent);
		this.finish();  //Don't want this in the history.
	}



	//TODO: See if there's an smarter way to do this.
	@Override
	public Object onRetainNonConfigurationInstance() {
		
		if(this.mSS.createTask != null)
			this.mSS.createTask.detach();
		return this.mSS;
	}
	
	private class SavedState extends Object{
		public boolean isEncoding = false;
		public EncodeGifTask createTask = null;
	}


}
