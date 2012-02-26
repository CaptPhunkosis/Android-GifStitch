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
import com.phunkosis.gifstitch.adapters.GifHomeGridAdapter;
import com.phunkosis.gifstitch.dialogs.DialogHelper;
import com.phunkosis.gifstitch.helpers.ImageHelper;
import com.phunkosis.gifstitch.helpers.ShareHelper;
import com.phunkosis.gifstitch.helpers.UploadGifTask;
import com.phunkosis.gifstitch.helpers.UploadGifTask.UploadCompleteListener;
import com.phunkosis.gifstitch.helpers.Utils;
import com.phunkosis.gifstitch.settings.GSSettings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class HomeGalleryActivity extends Activity implements OnClickListener, OnItemClickListener, OnItemLongClickListener{
	private GridView mGifGrid;
	private GifHomeGridAdapter mAdapter;
	
	private SavedState mSS;
	private AlertDialog mGifFileOptionsDialog;
	private AlertDialog mConfirmDeleteDialog;
	private ProgressDialog mUploadingDialog;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
				
		if(!Utils.isSDCardPresent(this)){
			DialogHelper.displayFatalSDError(this);
		}
		else{
			GSSettings.InitializeSettings(this.getApplicationContext());
			Utils.app_launched(this);

			Button createButt = (Button)findViewById(R.id.button_create_new);
			createButt.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					HomeGalleryActivity.this.startCreateNewGif();}});
			
			this.mAdapter = new GifHomeGridAdapter(this);
			
			this.mGifGrid = (GridView)findViewById(R.id.gifGrid);
			this.mGifGrid.setOnItemClickListener(this);
			this.mGifGrid.setOnItemLongClickListener(this);
			this.mGifGrid.setAdapter(this.mAdapter);
			
			this.mSS = (SavedState)this.getLastNonConfigurationInstance();
			if(this.mSS == null)
				this.mSS = new SavedState();
			
			this.mGifGrid.scrollTo(0, this.mSS.scrollPosition);
			if(this.mSS.inImageOptionsDialog)
				this.showImageOptionsDialog();
			else if(this.mSS.inConfirmDeleteDialog)
				this.showConfirmDeleteDialog();
			else if(this.mSS.mIsUploading)
				this.showUploadingDialog();
			
			if(this.mSS.mUploadGifTask != null)
				this.mSS.mUploadGifTask.setmUploadCompleteListener(this.mUploadCompleteListener);
			
			if(this.mAdapter.getCount() <= 0){
				this.startCreateNewGif();
			}
		}
		
	}
	
	UploadCompleteListener mUploadCompleteListener = new UploadCompleteListener() {
		@Override
		public void onUploadComplete(String result) {
			mSS.mIsUploading = false;
			mSS.mUploadGifTask = null;
			if(mUploadingDialog != null)
				mUploadingDialog.dismiss();
			if(result == null || result == ""){
				DialogHelper.getErrorDialog(HomeGalleryActivity.this, null, 
						getResources().getString(R.string.error_upload_failed)).show();
			}else{
				ShareHelper.startShareLinkIntent(HomeGalleryActivity.this, result);
			}
				
		}
	};

	private void refreshGrid(){
		this.mAdapter.refreshItems();
		this.mGifGrid.invalidateViews();
	}

	private void startCreateNewGif(){
		//Intent createGifIntent = new Intent(this, GatherFramesActivity.class);
		Intent createGifIntent = new Intent(this, GatherFramesActivity.class);
		this.startActivity(createGifIntent);
	}

	private void startViewGif(int position){
		Intent gifBrowseIntent = new Intent(this, BrowseGifsActivity.class);
		gifBrowseIntent.putExtra("position", position);
		this.startActivity(gifBrowseIntent);
	}
	
	private void handleDeleteGif(int position){
		String imgPath = this.mAdapter.getItemPath(position);
		ImageHelper.deleteGif(imgPath, this);
		this.refreshGrid();
	}
	
	private void showImageOptionsDialog(){
		this.mSS.inImageOptionsDialog = true;
		this.mGifFileOptionsDialog = DialogHelper.getGifFileOptionsDialog(this, this);
		this.mGifFileOptionsDialog.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if(keyCode == KeyEvent.KEYCODE_BACK)
					mSS.inImageOptionsDialog = false;
				return false;
			}
		});
		this.mGifFileOptionsDialog.show();
	}
	
	private void showConfirmDeleteDialog(){
		this.mConfirmDeleteDialog = DialogHelper.getConfirmDeleteDialog(this, this);
		this.mConfirmDeleteDialog.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if(keyCode == KeyEvent.KEYCODE_BACK)
					mSS.inConfirmDeleteDialog = false;
				return false;
			}
		});
		this.mConfirmDeleteDialog.show();
	}
	
	private void showUploadingDialog(){
		this.mUploadingDialog = DialogHelper.getUploadingGifProgressDialog(this);
		this.mUploadingDialog.show();
	}

	/*
	 * Event Handlers
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if(dialog == this.mGifFileOptionsDialog){
			this.mSS.inImageOptionsDialog = false;
			//TODO: I think there's a better way todo this switch.
			switch(which){
			case 0:
				if(this.mSS.mSelectedGifFilePosition >= 0){
					this.mSS.mIsUploading = true;
					String imagePath = HomeGalleryActivity.this.mAdapter.getItemPath(this.mSS.mSelectedGifFilePosition);
					this.mSS.mUploadGifTask = new UploadGifTask(this);
					this.mSS.mUploadGifTask.setmUploadCompleteListener(this.mUploadCompleteListener);
					this.mSS.mUploadGifTask.execute(imagePath);
					this.showUploadingDialog();
				}
				break;
			case 1:
				if(this.mSS.mSelectedGifFilePosition >= 0){
					String imagePath = HomeGalleryActivity.this.mAdapter.getItemPath(this.mSS.mSelectedGifFilePosition);
					Intent intent = new Intent(Intent.ACTION_SEND);
					intent.setType("image/gif");
					intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+imagePath));
				    startActivity(Intent.createChooser(intent, "ShareGif"));
				    this.mSS.mSelectedGifFilePosition = -1;
				    return;
				}
				break;
			case 2:
				if(this.mSS.mSelectedGifFilePosition >= 0){
					this.mSS.inConfirmDeleteDialog = true;
					this.showConfirmDeleteDialog();
					return;
				}
				break;
			}
		//HANDLE Delete Confirm Dialog Results
		}else if(dialog == this.mConfirmDeleteDialog){
			this.mSS.inConfirmDeleteDialog = false;
			switch(which){
			case DialogInterface.BUTTON_POSITIVE:
				this.handleDeleteGif(this.mSS.mSelectedGifFilePosition);
				break;
			}
			this.mSS.mSelectedGifFilePosition = -1;
		}
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = this.getMenuInflater();
		mi.inflate(R.menu.menu_home, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case R.id.mi_create_new_gif:
			this.startCreateNewGif();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		this.startViewGif(position);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		this.mSS.mSelectedGifFilePosition = position;
		this.showImageOptionsDialog();
		return true;
	}
	

	@Override
	protected void onRestart(){
		super.onRestart();
		this.refreshGrid();
	}
	
	
	/*
	 * State Managment
	 */
	
	//TODO: See if there's an smarter way to do this.
	@Override
	public Object onRetainNonConfigurationInstance() {
		this.mSS.scrollPosition = this.mGifGrid.getScrollY();
		if(this.mSS.mUploadGifTask != null)
			this.mSS.mUploadGifTask.setmUploadCompleteListener(null);
		return this.mSS;
	}
	
	private class SavedState extends Object{
		public int scrollPosition = -1;
		public boolean inImageOptionsDialog;
		public boolean inConfirmDeleteDialog;
		public boolean mIsUploading;
		public int mSelectedGifFilePosition;
		public UploadGifTask mUploadGifTask;
	}

}