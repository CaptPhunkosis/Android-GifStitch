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

import java.util.ArrayList;
import java.util.List;

import com.phunkosis.gifstitch.adapters.GifFramesAdapter;
import com.phunkosis.gifstitch.dialogs.DialogHelper;
import com.phunkosis.gifstitch.helpers.IOHelper;
import com.phunkosis.gifstitch.helpers.ImageHelper;
import com.phunkosis.gifstitch.settings.GSSettings;
import com.phunkosis.gifstitch.views.CameraSurface;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.MediaColumns;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.BounceInterpolator;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;



/*********************************************************************
 * 
 * Now that it works this activity could use an overhaul to clean up
 * redundancy and sloppiness.  Yeah...I laughed as I wrote that too.
 *
 ********************************************************************/


public class GatherFramesActivity extends Activity implements OnClickListener, OnItemClickListener, OnItemLongClickListener{
	
	private static final int SELECT_IMAGE_ACTIVITY = 1;
	
	private SavedState mSS;
	private CameraSurface mCameraSurface;
	private GifFramesAdapter mFrameAdapter;
	private Gallery mFrameGallery;
	private AlertDialog mFrameOptionsDialog;
	private AlertDialog mConfirmDeleteDialog;
	private AlertDialog mConfirmClearDialog;
	private AlertDialog mConfirmExitDialog;
	private ImageView mWorkerImageView;
	private RelativeLayout mLayoutTools;
	private HorizontalScrollView mLayoutEffects;
	private FrameLayout mFlashFrame;
	private DisplayMetrics mDisplayMetrics;
	private Button mDeleteFrameButton;
	private boolean mInFrameLimitDialog = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_gather_frames);
		final SavedState ss = (SavedState)this.getLastNonConfigurationInstance();
		if(ss != null)
			this.mSS = ss;
		else{
			this.mSS = new SavedState();
			IOHelper.clearTmpDirectory();
		}
		
		this.mDisplayMetrics = new DisplayMetrics();
		this.getWindowManager().getDefaultDisplay().getMetrics(this.mDisplayMetrics);
		
		this.mWorkerImageView = (ImageView)this.findViewById(R.id.worker_image);
		this.mWorkerImageView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(mSS.isViewingFrame)
					hideFramePreview();
				else
					handleTakePhotoClick();
			}
		});
		
		this.mFrameAdapter = new GifFramesAdapter(this, this.mSS.currentFrames);
		this.mFrameGallery = (Gallery)this.findViewById(R.id.frame_gallery);
		this.mFrameGallery.setAdapter(this.mFrameAdapter);
		this.mFrameGallery.setOnItemLongClickListener(this);
		this.mFrameGallery.setOnItemClickListener(this);
		
		this.mCameraSurface = (CameraSurface)this.findViewById(R.id.camera_surface);
		this.mCameraSurface.setShutterCallbackListener(new ShutterCallback());
		this.mCameraSurface.setPictureCallbackListener(new PictureCallback());
		this.mCameraSurface.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				handleTakePhotoClick();
			}
		});
		this.mCameraSurface.post(new Runnable() {
			@Override
			public void run() {
				setColorEffect(mSS.mLastEffect);
			}
		});
		
		this.mFlashFrame = (FrameLayout)this.findViewById(R.id.layout_flash_frame);
		
		this.initButtons();
		this.initEffectsButtons();
		
		if(this.mSS.isViewingFrame)
	    	this.showFramePreview(this.mFrameAdapter.getItemPath(this.mSS.mFocusPosition));
		
		if(GSSettings.getAreCreateToolsOpen())
	    	this.openCreateTools();
		
		//Handle dialogs
		if(this.mSS.inSettings)
	    	this.navToPreview();
	    else if(this.mSS.inImageSourceSelect)
	    	this.showChooseImageSourceAlert();
	    else if(this.mSS.inFrameOptions)
	    	this.showFrameOptions();
	    else if(this.mSS.inConfirmDelete)
	    	this.showConfirmDeleteDialog();
	    else if(this.mSS.inConfirmClear)
	    	this.showConfirmClearDialog();
	    else if(this.mSS.inConfirmExit)
	    	this.showConfirmExitDialog();
	    else{
	    	DialogHelper.showHint(this, this.getResources().getString(R.string.hint_tap_camera), false);
		}
		
	}
	
	private void handleTakePhotoClick(){
		if(mFrameAdapter.getCount() >= this.getResources().getInteger(R.integer.max_frames)){
			
			if(!this.mInFrameLimitDialog){
				this.mInFrameLimitDialog = true;
				AlertDialog d = DialogHelper.getErrorDialog(this, null, this.getResources().getString(R.string.error_max_frames));
				d.setOnDismissListener(new OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						mInFrameLimitDialog = false;
					}
				});
				d.show();
			}
		}else{
			this.mCameraSurface.startCapture();
		}
	}
	
	
	//TODO: THERE'S GOT TO BE A SMARTER WAY!!!!!!
	private void initButtons(){
		this.mLayoutTools = (RelativeLayout)this.findViewById(R.id.layout_tools);
		
		this.mDeleteFrameButton = (Button)this.findViewById(R.id.button_delete_frame);
		this.mDeleteFrameButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mSS.isViewingFrame)
					showConfirmDeleteDialog();
			}});
		
		Button buildButton = (Button)this.findViewById(R.id.button_preview);
		buildButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mFrameAdapter.getCount() > 0)
					navToPreview();
				else
					DialogHelper.showHint(GatherFramesActivity.this, getResources().getString(R.string.hint_frames_required), true);
			}});
		
		Button clearButton = (Button)this.findViewById(R.id.button_clear);
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showConfirmClearDialog();
			}});
		

		ToggleButton showToolsTB = (ToggleButton)this.findViewById(R.id.button_options);
		showToolsTB.setChecked(GSSettings.getAreCreateToolsOpen());
		showToolsTB.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) openCreateTools();
				else closeCreateTools();
			}});

		Button addFrameButon= (Button)this.findViewById(R.id.button_add);
		addFrameButon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showChooseImageSourceAlert();
			}});
		
		ToggleButton autoFocusButton = (ToggleButton)this.findViewById(R.id.button_autofocus);
		autoFocusButton.setChecked(GSSettings.getShouldAutoFocus());
		autoFocusButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setAutoFocusEnabled(isChecked);
			}});
		
		ToggleButton onionButton = (ToggleButton)this.findViewById(R.id.button_onionskin);
		onionButton.setChecked(GSSettings.getShouldOnionSkin());
		onionButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) enableOnionSkin();
				else disableOnionSkin();
			}});
		this.updateOnionSkin();
	
	}
	
	private void fireFlash(){
		if(this.mFlashFrame.getVisibility() != ViewGroup.VISIBLE){
			Animation flashAnimation = new AlphaAnimation(0.8f, 0.0f);
			flashAnimation.setDuration(400);
			flashAnimation.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
					mFlashFrame.setVisibility(ViewGroup.VISIBLE);
				}
				
				@Override
				public void onAnimationRepeat(Animation animation) {}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					mFlashFrame.setVisibility(ViewGroup.GONE);
				}
			});
			this.mFlashFrame.startAnimation(flashAnimation);
		}
	}
	
	private void openEffectTools(){
		if(this.mLayoutEffects != null && this.mLayoutEffects.getVisibility() == ViewGroup.GONE){
			float transDiff = this.getResources().getDimension(R.dimen.tab_button_size_v);
			TranslateAnimation tAnim = new TranslateAnimation(0f, 0f, -transDiff, 0f);
			tAnim.setDuration(this.getResources().getInteger(R.integer.tab_animation_duration));
			tAnim.setInterpolator(new BounceInterpolator());
			tAnim.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
					mLayoutEffects.setVisibility(Button.VISIBLE);
				}
				@Override
				public void onAnimationRepeat(Animation animation){}
				@Override
				public void onAnimationEnd(Animation animation){}
			});
			this.mLayoutEffects.startAnimation(tAnim);
		}
		GSSettings.setAreEffectsToolOpen(true);
	}
	
	
	private void closeEffectTools(){
		if(this.mLayoutEffects != null && this.mLayoutEffects.getVisibility() == ViewGroup.VISIBLE){
			float transDiff = this.getResources().getDimension(R.dimen.tab_button_size_v);
			TranslateAnimation tAnim = new TranslateAnimation(0f, 0f, 0f, -transDiff);
			tAnim.setDuration(this.getResources().getInteger(R.integer.tab_animation_duration));
			tAnim.setInterpolator(new BounceInterpolator());
			tAnim.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {}
				@Override
				public void onAnimationRepeat(Animation animation){}
				@Override
				public void onAnimationEnd(Animation animation){
					mLayoutEffects.setVisibility(ViewGroup.GONE);
				}
			});
			this.mLayoutEffects.startAnimation(tAnim);
		}
		//Cheap way to keep in sync.
		if(GSSettings.getAreCreateToolsOpen())
			GSSettings.setAreEffectsToolOpen(false);
	}
	
	private void openCreateTools(){
		if(this.mLayoutTools.getVisibility() == ViewGroup.GONE){
			float transDiff = this.getResources().getDimension(R.dimen.tab_button_size_v);
			TranslateAnimation tAnim = new TranslateAnimation(-transDiff, 0f, 0f, 0f);
			tAnim.setDuration(this.getResources().getInteger(R.integer.tab_animation_duration));
			tAnim.setInterpolator(new BounceInterpolator());
			tAnim.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
					mLayoutTools.setVisibility(Button.VISIBLE);
				}
				@Override
				public void onAnimationRepeat(Animation animation){}
				@Override
				public void onAnimationEnd(Animation animation){}
			});
			this.mLayoutTools.startAnimation(tAnim);
			if(GSSettings.getAreEffectsToolsOpen())
				this.openEffectTools();
		}
		GSSettings.setAreCreateToolsOpen(true);
	}
	
	private void closeCreateTools(){
		if(this.mLayoutTools.getVisibility() == ViewGroup.VISIBLE){
			float transDiff = this.getResources().getDimension(R.dimen.tab_button_size_v);
			TranslateAnimation tAnim = new TranslateAnimation(0f, -transDiff, 0f, 0f);
			tAnim.setDuration(this.getResources().getInteger(R.integer.tab_animation_duration));
			tAnim.setInterpolator(new BounceInterpolator());
			tAnim.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {}
				@Override
				public void onAnimationRepeat(Animation animation){}
				@Override
				public void onAnimationEnd(Animation animation){
					mLayoutTools.setVisibility(ViewGroup.GONE);
				}
			});
			this.mLayoutTools.startAnimation(tAnim);
		}
		GSSettings.setAreCreateToolsOpen(false);
		this.closeEffectTools();
	}
	
	private void showDeleteFrameButton(){
		if(this.mDeleteFrameButton.getVisibility() == Button.GONE){
			float transDiff = this.getResources().getDimension(R.dimen.tab_button_size_v);
			TranslateAnimation tAnim = new TranslateAnimation(transDiff, 0f, 0f, 0f);
			tAnim.setDuration(this.getResources().getInteger(R.integer.tab_animation_duration));
			tAnim.setInterpolator(new BounceInterpolator());
			tAnim.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {
					mDeleteFrameButton.setVisibility(Button.VISIBLE);
				}
				@Override
				public void onAnimationRepeat(Animation animation){}
				@Override
				public void onAnimationEnd(Animation animation){}
			});
			this.mDeleteFrameButton.startAnimation(tAnim);
		}
	}
	
	private void hideDeleteFrameButton(){
		if(this.mDeleteFrameButton.getVisibility() == Button.VISIBLE){
			float transDiff = this.getResources().getDimension(R.dimen.tab_button_size_v);
			TranslateAnimation tAnim = new TranslateAnimation(0f, transDiff, 0f, 0f);
			tAnim.setDuration(this.getResources().getInteger(R.integer.tab_animation_duration));
			tAnim.setInterpolator(new BounceInterpolator());
			tAnim.setAnimationListener(new AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {}
				@Override
				public void onAnimationRepeat(Animation animation){}
				@Override
				public void onAnimationEnd(Animation animation){
					mDeleteFrameButton.setVisibility(Button.GONE);
				}
			});
			this.mDeleteFrameButton.startAnimation(tAnim);
		}
	}
	
	private void enableOnionSkin(){
		GSSettings.setShouldOnionSkin(true);
		this.updateOnionSkin();
		DialogHelper.showHint(this, this.getResources().getString(R.string.hint_onionskin_enabled), true);
	}
	
	private void disableOnionSkin(){
		GSSettings.setShouldOnionSkin(false);
		this.updateOnionSkin();
		DialogHelper.showHint(this, this.getResources().getString(R.string.hint_onionskin_disabled), true);
	}
	
	private void updateOnionSkin(){
		if(GSSettings.getShouldOnionSkin() && this.mFrameAdapter.getCount() > 0){
			if(!this.mSS.isViewingFrame){
				int desiredSize = this.mDisplayMetrics.widthPixels > this.mDisplayMetrics.heightPixels ? this.mDisplayMetrics.widthPixels : this.mDisplayMetrics.heightPixels;
				String imagePath = this.mFrameAdapter.getItemPath(this.mFrameAdapter.getCount()-1);
				this.mWorkerImageView.setImageBitmap(ImageHelper.getDesirableBitmap(imagePath, desiredSize));
				this.mWorkerImageView.setAlpha(128); //TODO: Make this a setting for user.
				this.mWorkerImageView.setVisibility(ImageView.VISIBLE);
			}
		}else if(!this.mSS.isViewingFrame || this.mFrameAdapter.getCount() <= 0){
			this.mWorkerImageView.setVisibility(ImageView.GONE);
			this.mWorkerImageView.setImageBitmap(null);
		}
			
	}
	
	private void setAutoFocusEnabled(boolean enabled){
		GSSettings.setShouldAutoFocus(enabled);
		this.mCameraSurface.setAutoFocusEnabled(enabled);
		int message_id = enabled ? R.string.hint_autofocus_enabled : R.string.hint_autofocus_disabled;
		DialogHelper.showHint(this, this.getResources().getString(message_id), true);
	}
	
	private void showFramePreview(String path){
		this.mSS.isViewingFrame = true;
		int desiredSize = this.mDisplayMetrics.widthPixels > this.mDisplayMetrics.heightPixels ? this.mDisplayMetrics.widthPixels : this.mDisplayMetrics.heightPixels; 
		this.mWorkerImageView.setImageBitmap(ImageHelper.getDesirableBitmap(path, desiredSize));
		this.mWorkerImageView.setVisibility(ImageView.VISIBLE);
		this.mWorkerImageView.setAlpha(255);  //TODO: This might be diff for later versions of API.  Docs are confusing on that.
		this.showDeleteFrameButton();
	}
	
	private void hideFramePreview(){
		this.mSS.isViewingFrame = false;
		this.hideDeleteFrameButton();
		this.updateOnionSkin();
	}
	
	private void handleAddFrame(String path){
		this.mFrameAdapter.onAdd(path);
		this.mFrameAdapter.notifyDataSetChanged();
		this.mFrameGallery.setSelection(mFrameGallery.getCount()-1);
		this.updateOnionSkin();
	}
	
	private void handleRemoveFrame(int position){
		if(position > -1){
			this.mFrameAdapter.onRemove(position);
			this.mFrameAdapter.notifyDataSetChanged();
			this.mFrameGallery.setSelection(position > 0 ? position-1 : 0);
			this.hideFramePreview();
		}
	}
	
	private void clearAllFrames(){
		this.mFrameAdapter.removeAllItems();
		this.mFrameAdapter.notifyDataSetChanged();
		IOHelper.clearTmpDirectory();
		this.updateOnionSkin();
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		this.mSS.mFocusPosition = position;
		this.showFramePreview(this.mFrameAdapter.getItemPath(this.mSS.mFocusPosition));
		this.mFrameGallery.setSelection(this.mSS.mFocusPosition);
	}
	
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		this.mSS.mFocusPosition = position;
		this.showFrameOptions();
		return true;
	}
	
	public void onClick(DialogInterface dialog, int which) {
		if(this.mFrameOptionsDialog == dialog){
			this.mSS.inFrameOptions = false;
			switch(which){
			case 0:
				this.showConfirmDeleteDialog();
			}
		}else if(this.mConfirmDeleteDialog == dialog){
			this.mSS.inConfirmDelete = false;
			if(which == DialogInterface.BUTTON_POSITIVE)
				this.handleRemoveFrame(this.mSS.mFocusPosition);
		}else if(this.mConfirmClearDialog == dialog){
			this.mSS.inConfirmClear = false;
			if(which == DialogInterface.BUTTON_POSITIVE)
				this.clearAllFrames();
		}else if(this.mConfirmExitDialog == dialog){
			this.mSS.inConfirmExit = false;
			if(which == DialogInterface.BUTTON_POSITIVE)
				this.finish();
		}		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (resultCode == RESULT_CANCELED) {
			return;
		}
		
		switch(requestCode){
		case SELECT_IMAGE_ACTIVITY:
			String imgPath = this.getSelectedMediaPath(this, intent.getData());
			this.handleAddFrame(imgPath);
			return;
		default:
			super.onActivityResult(requestCode, resultCode, intent);
		}
	}
	
	
	//Gets the path to the image from the return camera/gallery intent data.
	private String getSelectedMediaPath(Activity a, Uri uri) {
	    String[] projection = { MediaColumns.DATA };
	    Cursor cursor = a.managedQuery(uri, projection, null, null, null);
	    a.startManagingCursor(cursor);
	    int column_index = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
	    cursor.moveToFirst();
	    return cursor.getString(column_index);
	}
	
	
	/*
	 * OPTIONS MENU STUFF
	 */
	@Override
	public void onBackPressed(){
		if(this.mFrameAdapter.getCount() > 0)
			this.showConfirmExitDialog();
		else
			super.onBackPressed();
	};
	
	
	/*
	 * DIALOG STUFF
	 */
	private void navToPreview(){
		Intent previewIntent = new Intent(this, PreviewActivity.class);
		previewIntent.putExtra("gifframes", this.mFrameAdapter.getItems());
		this.startActivity(previewIntent);
	}
	
	private void showChooseImageSourceAlert(){
		if(mFrameAdapter.getCount() >= this.getResources().getInteger(R.integer.max_frames)){
			DialogHelper.getErrorDialog(this, null, this.getResources().getString(R.string.error_max_frames)).show();
		}else{
			Intent gallIntent = new Intent(Intent.ACTION_GET_CONTENT);
			gallIntent.setType("image/*");
			startActivityForResult(gallIntent, SELECT_IMAGE_ACTIVITY);
		}
	}
	
	
	private void showConfirmExitDialog(){
		this.mSS.inConfirmExit = true;
		this.mConfirmExitDialog = DialogHelper.getConfirmExitCreate(this, this);
		this.mConfirmExitDialog.show();
	}
	private void showConfirmDeleteDialog(){
		this.mSS.inConfirmDelete = true;
		this.mConfirmDeleteDialog = DialogHelper.getConfirmDeleteDialog(this, this);
		this.mConfirmDeleteDialog.show();
	}
	
	private void showConfirmClearDialog(){
		this.mSS.inConfirmClear = true;
		this.mConfirmClearDialog = DialogHelper.getConfirmClearFrames(this, this);
		this.mConfirmClearDialog.show();
	}
	
	private void showFrameOptions(){		
		this.mSS.inFrameOptions = true;
		this.mFrameOptionsDialog = DialogHelper.getFrameOptionsDialog(this, this);
		this.mFrameOptionsDialog.show();
	}
	
	
	private void setColorEffect(String effect){
		mCameraSurface.setColorEffect(effect);
		this.mSS.mLastEffect = effect;
	}
	
	
	
	/*
	 * I totally threw this in last minute. Need to look over for sanity.
	 */
	
	private class EffectInfo{
		public String name;
		public int buttonId;
		public String effect;
		
		public EffectInfo(String name, int buttonId, String effect){
			this.name = name;
			this.buttonId = buttonId;
			this.effect = effect;
		}
		
	}

	
	private void initEffectsButtons(){
		
		ArrayList<EffectInfo> effectsList = new ArrayList<EffectInfo>();
		
		Resources res = this.getResources();
		effectsList.add(new EffectInfo(res.getString(R.string.ce_name_none), R.id.button_effect_none, Camera.Parameters.EFFECT_NONE));
		effectsList.add(new EffectInfo(res.getString(R.string.ce_name_mono), R.id.button_effect_mono, Camera.Parameters.EFFECT_MONO));
		effectsList.add(new EffectInfo(res.getString(R.string.ce_name_sepia), R.id.button_effect_sepia, Camera.Parameters.EFFECT_SEPIA));
		effectsList.add(new EffectInfo(res.getString(R.string.ce_name_aqua), R.id.button_effect_aqua, Camera.Parameters.EFFECT_AQUA));
		effectsList.add(new EffectInfo(res.getString(R.string.ce_name_solarize), R.id.button_effect_solarize, Camera.Parameters.EFFECT_SOLARIZE));
		effectsList.add(new EffectInfo(res.getString(R.string.ce_name_posterize), R.id.button_effect_posterize, Camera.Parameters.EFFECT_POSTERIZE));
		effectsList.add(new EffectInfo(res.getString(R.string.ce_name_negative), R.id.button_effect_negative, Camera.Parameters.EFFECT_NEGATIVE));
		
		ToggleButton showEffectTB = (ToggleButton)this.findViewById(R.id.button_effects_toggle);
		showEffectTB.setChecked(GSSettings.getAreEffectsToolsOpen());
		showEffectTB.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked) openEffectTools();
				else closeEffectTools();
			}});
		
		List<String> supportedEffects = this.mCameraSurface.getSupportedEffects();
		if(supportedEffects.size() > 1){
			for(final EffectInfo effectItem : effectsList){
				Button effectButton = (Button)this.findViewById(effectItem.buttonId);
				
				if(supportedEffects.contains(effectItem.effect)){
					effectButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							DialogHelper.showHint(GatherFramesActivity.this, effectItem.name, true);
							setColorEffect(effectItem.effect);
						}
					});
				}
				else effectButton.setVisibility(Button.GONE);
			}			
			
			
			this.mLayoutEffects = (HorizontalScrollView)this.findViewById(R.id.layout_effects);
			if(GSSettings.getAreCreateToolsOpen() && GSSettings.getAreEffectsToolsOpen())
				this.openEffectTools();

		}else{
			showEffectTB.setVisibility(ToggleButton.GONE);
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		this.mCameraSurface.pause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.mCameraSurface.resume();
	}

	private final class ShutterCallback implements Camera.ShutterCallback {
        public void onShutter() {
        	fireFlash();
        }
    }
	
	private final class PictureCallback implements Camera.PictureCallback {
		public void onPictureTaken(final byte [] jpegData, final Camera camera) {
			String tmpFilePath = ImageHelper.saveTmpImage(jpegData);
			handleAddFrame(tmpFilePath);
		}
	}


	//TODO: See if there's an smarter way to do this.
	@Override
	public Object onRetainNonConfigurationInstance() {
		this.mSS.currentFrames = this.mFrameAdapter.getItems();
		return this.mSS;
	}
	
	private class SavedState extends Object{
		public ArrayList<String> currentFrames = new ArrayList<String>();
		public boolean inSettings = false;
		public boolean inImageSourceSelect = false;
		public boolean inConfirmDelete = false;
		public boolean inConfirmClear = false;
		public boolean inConfirmExit = false;
		public boolean inFrameOptions = false;
		public boolean isViewingFrame = false;
		private int mFocusPosition = -1;
		private String mLastEffect = Camera.Parameters.EFFECT_NONE;
	}
}
