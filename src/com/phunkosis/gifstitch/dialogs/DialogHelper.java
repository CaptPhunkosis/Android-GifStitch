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
package com.phunkosis.gifstitch.dialogs;

import com.phunkosis.gifstitch.R;
import com.phunkosis.gifstitch.settings.GSSettings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.widget.Toast;

public class DialogHelper {
	
	public static void showHint(final Context c, String hintText, boolean makeItAQuickOne){
		int duration = makeItAQuickOne ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
		Toast t = Toast.makeText(c, hintText, duration);
		t.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
		t.show();
	}
	
	public static AlertDialog getConfirmExitCreate(final Context c, OnClickListener listener){
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(c)
			.setMessage(c.getResources().getString(R.string.dialog_exitcreate_confirm))
			.setPositiveButton(c.getResources().getString(R.string.yes), listener)
			.setNegativeButton(c.getResources().getString(R.string.no), listener);
			
		return alertBuilder.create();
	}
	
	public static AlertDialog getConfirmClearFrames(final Context c, OnClickListener listener){
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(c)
			.setMessage(c.getResources().getString(R.string.dialog_clear_confirm_title))
			.setPositiveButton(c.getResources().getString(R.string.yes), listener)
			.setNegativeButton(c.getResources().getString(R.string.no), listener);
			
		return alertBuilder.create();
	}

	public static AlertDialog getGifFileOptionsDialog(final Context c, OnClickListener listener){
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(c);
		String shareLink = c.getResources().getString(R.string.dialog_giffile_option_share_link);
		String shareImage = c.getResources().getString(R.string.dialog_giffile_option_share_file);
		String deleteImage = c.getResources().getString(R.string.dialog_giffile_option_delete);
		CharSequence[] choices = {shareLink, shareImage, deleteImage};
		
		alertBuilder.setItems(choices, listener);
		return alertBuilder.create();
	}
	
	public static AlertDialog getGifFileShareDialog(final Context c, OnClickListener listener){
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(c);
		String shareLink = c.getResources().getString(R.string.dialog_giffile_option_share_link);
		String shareImage = c.getResources().getString(R.string.dialog_giffile_option_share_file);
		CharSequence[] choices = {shareLink, shareImage};
		
		alertBuilder.setItems(choices, listener);
		return alertBuilder.create();
	}
	
	
	public static AlertDialog getConfirmDeleteDialog(final Context c, OnClickListener listener){
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(c)
			.setMessage(c.getResources().getString(R.string.dialog_delete_confirm_title))
			.setPositiveButton(c.getResources().getString(R.string.yes), listener)
			.setNegativeButton(c.getResources().getString(R.string.no), listener);
			
		return alertBuilder.create();
	}
	
	
	public static AlertDialog getImageSourceDialog(final Context c, OnClickListener listener){
		String camChoice = c.getResources().getString(R.string.dialog_imgsrc_option_camera);
		String galChoice = c.getResources().getString(R.string.dialog_imgsrc_option_gallery);
		CharSequence[] choices = {camChoice, galChoice};
		
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(c)
			.setTitle(c.getResources().getString(R.string.dialog_imgsrc_title))
			.setItems(choices, listener);
		
		return alertBuilder.create();
	}
	
	
	public static ProgressDialog getEncodingProgressDialog(final Context c){
		ProgressDialog mProgDiag = new ProgressDialog(c);
		mProgDiag.setMessage(c.getResources().getString(R.string.dialog_encoding_message));
		mProgDiag.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgDiag.setCancelable(false);
		return mProgDiag;
	}
	
	public static ProgressDialog getUploadingGifProgressDialog(final Context c){
		ProgressDialog mProgDiag = new ProgressDialog(c);
		mProgDiag.setMessage(c.getResources().getString(R.string.dialog_uploading_message));
		return mProgDiag;
	}
	
	public static AlertDialog getErrorDialog(final Context c, OnClickListener listener, String error){
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(c)
		.setMessage(error)
		.setTitle(c.getResources().getString(R.string.dialog_error_title))
		.setPositiveButton(c.getResources().getString(R.string.ok), listener);
		return alertBuilder.create();
	}
	
	
	public static AlertDialog getFrameOptionsDialog(final Context c, OnClickListener listener){
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(c);
		String delete = c.getResources().getString(R.string.dialog_giffile_option_delete);
		CharSequence[] choices = {delete};
		alertBuilder.setItems(choices, listener);
		return alertBuilder.create();
	}
	
	
	public static void showRateDialog(final Context c) {
		
		String title = c.getString(R.string.dialog_rate_app_title);
		String message = c.getString(R.string.dialog_rate_app_message);
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(c)
		.setIcon(android.R.drawable.btn_star)
		.setMessage(message)
		.setTitle(title)
		.setPositiveButton(c.getString(R.string.heckyeah), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				GSSettings.setShowRatedDialog(false);
				c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.phunkosis.gifstitch")));
				dialog.dismiss();
			}
		})
		.setNegativeButton(c.getString(R.string.heckno), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				GSSettings.setShowRatedDialog(false);
			}
		});
		
		alertBuilder.create().show();
    }
	
	
	
	
	public static void displayFatalSDError(final Activity a){
		String title = a.getString(R.string.error_sd_card_title);
		String desc = a.getString(R.string.error_sd_card_desc);
		showFatalErrorAndFinish(a, title, desc);
	}
	
	public static void displayFatalCameraError(final Activity a){
		String title = a.getString(R.string.error_camera_title);
    	String desc = a.getString(R.string.error_camera_desc);
    	showFatalErrorAndFinish(a, title, desc);
	}
	
	public static void showFatalErrorAndFinish(final Activity activity, String title, String message) {
        DialogInterface.OnClickListener buttonListener =
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                activity.finish();
            }
        };
        new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(R.string.ok, buttonListener)
                .show();
    }
}
