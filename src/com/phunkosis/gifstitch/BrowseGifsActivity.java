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

import com.phunkosis.gifstitch.R;
import com.phunkosis.gifstitch.adapters.GifBrowserAdapter;
import com.phunkosis.gifstitch.dialogs.DialogHelper;
import com.phunkosis.gifstitch.helpers.ImageHelper;
import com.phunkosis.gifstitch.helpers.ShareHelper;
import com.phunkosis.gifstitch.helpers.UploadGifTask;
import com.phunkosis.gifstitch.helpers.UploadGifTask.UploadCompleteListener;
import com.phunkosis.gifstitch.views.GifView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Gallery;
import android.widget.ViewSwitcher;

public class BrowseGifsActivity extends Activity implements AdapterView.OnItemSelectedListener, OnLongClickListener, OnClickListener{
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 150;
	
	private SavedState mSS;
	private GestureDetector mGestureDetector;
    private View.OnTouchListener mTouchListener;
	private GifBrowserAdapter mAdapter;
	private ViewSwitcher mSwitcher;
	private Gallery mGallery;
	private AlertDialog mGifFileOptionsDialog;
	private AlertDialog mConfirmDeleteDialog;
	private AlertDialog mShareOptionsDialog;
	private ProgressDialog mUploadingDialog;
	private int mPausedPosition = -1;
	
	private Animation mInLeftAnim;
	private Animation mOutRightAnim;
	private Animation mInRightAnim;
	private Animation mOutLeftAnim;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_gifbrowser);
		
		int passedPosition = 0;
		Bundle extras = this.getIntent().getExtras();
		if(extras != null)
			passedPosition = extras.getInt("position");
		
		this.mSS = (SavedState)this.getLastNonConfigurationInstance();
		if(this.mSS == null){
			this.mSS = new SavedState();
			this.mSS.mPosition = passedPosition;
		}
				
		this.mGestureDetector = new GestureDetector(new MyGestureDetector());
        this.mTouchListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (mGestureDetector.onTouchEvent(event)) {
                    return true;
                }
                return false;
            }
        };
		
        
		//Setup the GifViews
		GifView gv1 = new GifView(this);
		gv1.setOnTouchListener(this.mTouchListener);
		gv1.setBackgroundColor(Color.TRANSPARENT);
        gv1.setLayoutParams(
        		new ViewSwitcher.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        		android.view.ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        gv1.setOnLongClickListener(this);

        GifView gv2 = new GifView(this);
        gv2.setOnTouchListener(this.mTouchListener);
        gv2.setBackgroundColor(Color.TRANSPARENT);
        gv2.setLayoutParams(gv1.getLayoutParams());
        gv2.setOnLongClickListener(this);
       
        
        this.mInLeftAnim = AnimationUtils.loadAnimation(this, R.anim.anim_slide_left_in);
        this.mOutRightAnim = AnimationUtils.loadAnimation(this, R.anim.anim_slide_right_out);
        this.mInRightAnim = AnimationUtils.loadAnimation(this, R.anim.anim_slide_right_in);
        this.mOutLeftAnim = AnimationUtils.loadAnimation(this, R.anim.anim_slide_left_out);
        
        this.mOutLeftAnim.setAnimationListener(new GifViewAnimationListener());
        this.mOutRightAnim.setAnimationListener(new GifViewAnimationListener());
        
		this.mSwitcher = (ViewSwitcher)this.findViewById(R.id.switcher);
		this.mSwitcher.setOnTouchListener(this.mTouchListener);
		
        this.mSwitcher.setInAnimation(this.mInRightAnim);
        this.mSwitcher.setOutAnimation(this.mOutLeftAnim);
        this.mSwitcher.addView(gv1);
        this.mSwitcher.addView(gv2);
        
        ViewGroup layout = (ViewGroup)this.findViewById(R.id.layout_browse);
        layout.setOnTouchListener(this.mTouchListener);
		
		this.mAdapter = new GifBrowserAdapter(this);
		
		this.mGallery = (Gallery)this.findViewById(R.id.gallery);
        this.mGallery.setAdapter(this.mAdapter);
        this.mGallery.setOnItemSelectedListener(this);
        
        //Is this bad or just lazy?
        if(this.mAdapter.getCount() <= 0)
        	this.finish();
        
        if(this.mSS.mUploadGifTask != null)
			this.mSS.mUploadGifTask.setmUploadCompleteListener(this.mUploadCompleteListener);
        
        if(this.mSS.inConfirmDeleteDialog)
        	this.showConfirmDeleteDialog();
        else if(this.mSS.inImageOptionsDialog)
        	this.showImageOptionsDialog();
        else if(this.mSS.inShareOptionsDialog)
        	this.showShareOptionsDialog();
        else if(this.mSS.mIsUploading)
			this.showUploadingDialog();
	}
	
	private class GifViewAnimationListener implements AnimationListener{
		@Override
		public void onAnimationStart(Animation animation) {}
		@Override
		public void onAnimationRepeat(Animation animation) {}
		@Override
		public void onAnimationEnd(Animation animation) {
			BrowseGifsActivity.this.unloadHiddenViews();
		}
		
	}
	
	public void browseNext(){
		if(this.mSS.mPosition < this.mAdapter.getCount())
			this.browseToItem(this.mSS.mPosition+1);
	}
	
	public void browsePrev(){
		if(this.mSS.mPosition > 0)
			this.browseToItem(this.mSS.mPosition-1);
	}
	
	public void browseToItem(int position){
		if(position >= 0 && position < this.mAdapter.getCount())
			this.mGallery.setSelection(position);
	}
	
	public void unloadHiddenViews(){
		GifView nv = (GifView)this.mSwitcher.getNextView();
		nv.unloadGif();
	}
	
	private void showImageOptionsDialog(){
		this.mSS.inImageOptionsDialog = true;
		this.mGifFileOptionsDialog = DialogHelper.getGifFileOptionsDialog(this, this);
		this.mGifFileOptionsDialog.show();
	}
	
	private void showConfirmDeleteDialog(){
		this.mSS.inConfirmDeleteDialog = true;
		this.mConfirmDeleteDialog = DialogHelper.getConfirmDeleteDialog(this, this);
		this.mConfirmDeleteDialog.show();
	}
	
	private void showShareOptionsDialog(){
		this.mSS.inShareOptionsDialog = true;
		this.mShareOptionsDialog = DialogHelper.getGifFileShareDialog(this, this);
		this.mShareOptionsDialog.show();
	}
	
	UploadCompleteListener mUploadCompleteListener = new UploadCompleteListener() {
		@Override
		public void onUploadComplete(String result) {
			mSS.mIsUploading = false;
			mSS.mUploadGifTask = null;
			if(mUploadingDialog != null)
				mUploadingDialog.dismiss();
			if(result == null || result == ""){
				DialogHelper.getErrorDialog(BrowseGifsActivity.this, null, 
						getResources().getString(R.string.error_upload_failed)).show();
			}else{
				ShareHelper.startShareLinkIntent(BrowseGifsActivity.this, result);
			}
				
		}
	};
	
	private void handleDeleteGif(int position){
		String imgPath = this.mAdapter.getItemPath(position);
		ImageHelper.deleteGif(imgPath, this);
		this.mAdapter.refreshItems();
		
		//TODO: Figure out different way to do this. Dangerous
		if(this.mAdapter.getCount() <= 0)
			this.finish();
		else{
			this.mAdapter.notifyDataSetChanged();
			if(this.mSS.mPosition < this.mAdapter.getCount())
				this.onItemSelected(null, null, this.mSS.mPosition, 0);
			else
				this.onItemSelected(null, null, 0, 0);
		}
	}
	
	private void shareCurrentImage(){
		String imagePath = this.mAdapter.getItemPath(this.mSS.mPosition);
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("image/gif");
		intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+imagePath));
	    startActivity(Intent.createChooser(intent, "ShareGif"));
	}
	
	
	private void startShareLinkTask(){
		String imagePath = this.mAdapter.getItemPath(this.mSS.mPosition);
		this.mSS.mUploadGifTask = new UploadGifTask(this);
		this.mSS.mUploadGifTask.setmUploadCompleteListener(this.mUploadCompleteListener);
		this.mSS.mUploadGifTask.execute(imagePath);
		this.showUploadingDialog();
	}
	
	private void showUploadingDialog(){
		this.mSS.mIsUploading = true;
		this.mUploadingDialog = DialogHelper.getUploadingGifProgressDialog(this);
		this.mUploadingDialog.show();
	}
	
	/*
	 * Event Handlers
	 */
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = this.getMenuInflater();
		mi.inflate(R.menu.menu_browse, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.mi_browse_share:
			this.showShareOptionsDialog();
			return true;
		case R.id.mi_browse_delete:
			this.showConfirmDeleteDialog();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		//Because those gifs are going to be chewing on your CPU long after you're gone.
		((GifView)this.mSwitcher.getCurrentView()).unloadGif();
		((GifView)this.mSwitcher.getNextView()).unloadGif();
		this.mPausedPosition = this.mSS.mPosition;
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if(this.mPausedPosition > -1){
			GifView currentView = (GifView)this.mSwitcher.getCurrentView();
			currentView.loadGif(this.mAdapter.getItemPath(this.mPausedPosition));			
		}else
			this.browseToItem(this.mSS.mPosition);
		this.mPausedPosition = -1;
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id){
		if(this.mSS.mPosition <= position){
			this.mSwitcher.setInAnimation(this.mInLeftAnim);
	        this.mSwitcher.setOutAnimation(this.mOutLeftAnim);
		}
		else{
			this.mSwitcher.setInAnimation(this.mInRightAnim);
	        this.mSwitcher.setOutAnimation(this.mOutRightAnim);
		}
		
		//Handle the possibility of wackyness
		if(this.mAdapter.getCount() <= 0)
			this.finish();
		else if(position > 0 && position >= this.mAdapter.getCount())
			position = this.mAdapter.getCount()-1;
		
		GifView nv = (GifView)this.mSwitcher.getNextView();
		nv.loadGif(this.mAdapter.getItemPath(position));
		this.mSS.mPosition = position;
		nv.setBackgroundColor(Color.TRANSPARENT);	
		this.mSwitcher.showNext();
	}
	
	@Override
	public boolean onLongClick(View arg0) {
		this.showImageOptionsDialog();
		return true;
	}
	
	

	@Override
	public void onClick(DialogInterface dialog, int which) {
		//HANDLE Gif File Options Dialog Results
		if(dialog == this.mGifFileOptionsDialog){
			this.mSS.inImageOptionsDialog = false;
			//TODO: I think there's a better way todo this switch.
			switch(which){
			case 0:
				if(this.mSS.mPosition >= 0)
					startShareLinkTask();
				break;
			case 1:
				if(this.mSS.mPosition >= 0)
					shareCurrentImage();
				break;
			case 2:
				if(this.mSS.mPosition >= 0){
					this.showConfirmDeleteDialog();
					return;
				}
				break;
			}
		//HANDLE Delete Confirm Dialog Results
		}else if(dialog == this.mShareOptionsDialog){
			this.mSS.inShareOptionsDialog = false;
			switch(which){
			case 0:
				if(this.mSS.mPosition >= 0)
					startShareLinkTask();
				break;
			case 1:
				if(this.mSS.mPosition >= 0)
					shareCurrentImage();
				break;
			}
		}else if(dialog == this.mConfirmDeleteDialog){
		
			this.mSS.inConfirmDeleteDialog = false;
			switch(which){
			case DialogInterface.BUTTON_POSITIVE:
				this.handleDeleteGif(this.mSS.mPosition);
				break;
			}
			//this.mSelectedGifFilePosition = -1;
		}
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {}
	
	private class MyGestureDetector extends SimpleOnGestureListener {
		
		@Override
		public boolean onDown(MotionEvent event) {
			return true;
	    }
		
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;
                
                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    browseNext();
                }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    browsePrev();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

    }
	
	
	/*
	 * State Managment
	 */
	
	//TODO: See if there's an smarter way to do this.	
	@Override
	public Object onRetainNonConfigurationInstance() {
		if(this.mSS.mUploadGifTask != null)
			this.mSS.mUploadGifTask.setmUploadCompleteListener(null);
		return this.mSS;
	}
	
	private class SavedState extends Object{
		public int mPosition = -1;
		public boolean mIsUploading = false;
		public boolean inImageOptionsDialog = false;
		public boolean inConfirmDeleteDialog = false;
		public boolean inShareOptionsDialog = false;
		public UploadGifTask mUploadGifTask;
	}

}
