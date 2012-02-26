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
package com.phunkosis.gifstitch.helpers;

import android.content.Context;
import android.os.AsyncTask;

public class UploadGifTask extends AsyncTask<String, Integer, String> {
	private UploadCompleteListener mUploadCompleteListener;
	private Context context;
	
	public UploadGifTask(Context c) {
		super();
		this.context = c;
	}

	public void setmUploadCompleteListener(UploadCompleteListener mUploadCompleteListener) {
		this.mUploadCompleteListener = mUploadCompleteListener;
	}

	@Override
	protected String doInBackground(String... arg0) {
		try{
			String imagePath = arg0[0];
			String url = ShareHelper.uploadToSite(imagePath, this.context);
			return url;
			
		}catch(Exception ex){
			return null;
		}
	}
	
	@Override
	protected void onPostExecute(String result) {
		if(this.mUploadCompleteListener != null)
			this.mUploadCompleteListener.onUploadComplete(result);
	}
	
	public interface UploadCompleteListener
	{
		public void onUploadComplete(String result);
	}
}
