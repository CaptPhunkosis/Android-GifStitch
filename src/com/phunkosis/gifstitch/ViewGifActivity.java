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
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ViewGifActivity extends Activity implements OnClickListener{

	private SavedState mSS;
	private String mGifPath;
	private GifView mGifView;
	private AlertDialog mConfirmDeleteDialog;
	private AlertDialog mShareOptionsDialog;
	private ProgressDialog mUploadingDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    this.setContentView(R.layout.activity_viewgif);
	    
	    this.mSS = (SavedState)this.getLastNonConfigurationInstance();
	    
	    Bundle extras = this.getIntent().getExtras();
		if(extras != null)
			this.mGifPath = extras.getString("gifPath");
		
		if(this.mSS == null)
			this.mSS = new SavedState();
	    
	    this.mGifView = (GifView)this.findViewById(R.id.viewedGif);
	    this.mGifView.setBackgroundColor(this.getResources().getColor(R.color.dark_background));
	    
	    Button homeButton = (Button)this.findViewById(R.id.button_nav_home);
	    homeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {navHome();}
		});
	    
	    Button backButton = (Button)this.findViewById(R.id.button_nav_back);
	    backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {navBack();}
		});
	    
	    Button shareButton = (Button)this.findViewById(R.id.button_share);
	    shareButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {showShareOptionsDialog();}
		});
	    
	    /*Button deleteButton = (Button)this.findViewById(R.id.button_delete);
	    deleteButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {showConfirmDeleteDialog();}
		});*/
	    
	    
        if(this.mSS.mUploadGifTask != null)
			this.mSS.mUploadGifTask.setmUploadCompleteListener(this.mUploadCompleteListener);
        
	    if(this.mSS.inConfirmDeleteDialog)
	    	this.showConfirmDeleteDialog();
	    else if(this.mSS.inShareOptionsDialog)
        	this.showShareOptionsDialog();
        else if(this.mSS.mIsUploading)
			this.showUploadingDialog();
	}
	
	private void navHome(){
		Intent i = new Intent(this, HomeGalleryActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		this.startActivity(i);
		
	}
	
	private void navBack(){
		this.finish();
	}
	
	private void showShareOptionsDialog(){
		this.mSS.inShareOptionsDialog = true;
		this.mShareOptionsDialog = DialogHelper.getGifFileShareDialog(this, this);
		this.mShareOptionsDialog.show();
	}
	
	private void showUploadingDialog(){
		this.mSS.mIsUploading = true;
		this.mUploadingDialog = DialogHelper.getUploadingGifProgressDialog(this);
		this.mUploadingDialog.show();
	}
	
	
	private void shareCurrentImage(){
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("image/gif");
		intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+this.mGifPath));
	    startActivity(Intent.createChooser(intent, "Share Gif"));
	    return;
	}
	
	private void showConfirmDeleteDialog(){
		this.mSS.inConfirmDeleteDialog = true;
		this.mConfirmDeleteDialog = DialogHelper.getConfirmDeleteDialog(this, this);
		this.mConfirmDeleteDialog.show();
	}
	
	private void deleteImage(){
		ImageHelper.deleteGif(this.mGifPath, this);
		this.navBack();
	}

	
	private void startShareLinkTask(){
		this.mSS.mUploadGifTask = new UploadGifTask(this);
		this.mSS.mUploadGifTask.setmUploadCompleteListener(this.mUploadCompleteListener);
		this.mSS.mUploadGifTask.execute(this.mGifPath);
		this.showUploadingDialog();
	}
	
	UploadCompleteListener mUploadCompleteListener = new UploadCompleteListener() {
		@Override
		public void onUploadComplete(String result) {
			mSS.mIsUploading = false;
			mSS.mUploadGifTask = null;
			if(mUploadingDialog != null)
				mUploadingDialog.dismiss();
			if(result == null || result == ""){
				DialogHelper.getErrorDialog(ViewGifActivity.this, null, 
						getResources().getString(R.string.error_upload_failed)).show();
			}else{
				ShareHelper.startShareLinkIntent(ViewGifActivity.this, result);
			}
				
		}
	};
	
	@Override
	protected void onPause() {
		super.onPause();
		this.mGifView.unloadGif();
	}

	@Override
	protected void onResume() {
		if(this.mGifPath != null)
			this.mGifView.loadGif(this.mGifPath);
		super.onResume();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if(dialog == this.mConfirmDeleteDialog){
			this.mSS.inConfirmDeleteDialog = false;
			if(which == DialogInterface.BUTTON_POSITIVE)
				this.deleteImage();
		}else if(dialog == this.mShareOptionsDialog){
			this.mSS.inShareOptionsDialog = false;
			switch(which){
			case 0:
				startShareLinkTask();
				break;
			case 1:
				shareCurrentImage();
				break;
			}
		}
	}
	
	//TODO: See if there's an smarter way to do this.	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return this.mSS;
	}
	
	private class SavedState extends Object{
		public boolean inConfirmDeleteDialog = false;
		public boolean inShareOptionsDialog = false;
		public boolean mIsUploading = false;
		public UploadGifTask mUploadGifTask;
	}

}
