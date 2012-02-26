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
package com.phunkosis.gifstitch.encoder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import com.phunkosis.gifstitch.PreviewActivity;
import com.phunkosis.gifstitch.R;
import com.phunkosis.gifstitch.helpers.IOHelper;
import com.phunkosis.gifstitch.helpers.ImageHelper;
import com.phunkosis.gifstitch.helpers.Utils;
import com.phunkosis.gifstitch.settings.GSSettings;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.util.Log;


/*
 * A little weird but right now this only works with the GatherFramesActivity Activity.  
 * Just need to clean up dependancies.  
 */
public class EncodeGifTask extends AsyncTask<List<String>, Integer, Boolean> {
	private static final String TAG = "EncodeGifTask";
	private PreviewActivity activity=null;
	private String mOutputPath;
	
	
	
	public EncodeGifTask(PreviewActivity activity) {
		this.attach(activity);
	}
	
	@Override
	protected Boolean doInBackground(List<String>... imgPaths) {
		FileOutputStream dst = null;
		try{
			this.mOutputPath = IOHelper.getNextNewGifName();
			GifEncoder ge = new GifEncoder();
			dst = new FileOutputStream(new File(this.mOutputPath));
			ge.start(dst);
			ge.setDelay(GSSettings.getGifFrameDelay());
			ge.setQuality(this.activity.getResources().getInteger(R.integer.gif_quality));
			ge.setRepeat(0);
			
			int first_width = -1;
			int first_height = -1;
			int processed_count = 0;
			int gifOutputSize = GSSettings.getGifOutputSize().size();
			for(String imgPath : imgPaths[0]){
				//Scale image down to size chosen by user.
	        	Bitmap testBitmap = ImageHelper.getDesirableBitmap(imgPath, gifOutputSize);
	        	
	        	//Testing hack.
	        	if(testBitmap == null){
	        		Utils.memoryPanic();
	        		testBitmap = ImageHelper.getDesirableBitmap(imgPath, gifOutputSize);
	        		if(testBitmap == null) break;
	        	}
	        	
	        	//This will force all images to same width/height as first frame.
	        	if(first_width < 0){
	        		if(testBitmap.getWidth() > testBitmap.getHeight()){
	        			first_height = (int)(testBitmap.getHeight() * ((double)gifOutputSize / testBitmap.getWidth()));
	        			first_width = gifOutputSize;
	        		}
	        		else{
	        			first_width = (int)(testBitmap.getWidth() * ((double)gifOutputSize / testBitmap.getHeight()));
	        			first_height = gifOutputSize;
	        		}
	        	}
	        	Matrix matrix = new Matrix();
	        	float scaleW = ((float)first_width/testBitmap.getWidth());
	        	float scaleH = ((float)first_height/testBitmap.getHeight());
	        	matrix.setScale(scaleW, scaleH);
	        	
	        	Bitmap properBitmap = Bitmap.createBitmap(testBitmap, 0, 0, testBitmap.getWidth(), testBitmap.getHeight(), matrix, true);
	        	//Testing Hack
	        	if(properBitmap == null){
	        		Utils.memoryPanic();
	        		properBitmap = Bitmap.createBitmap(testBitmap, 0, 0, testBitmap.getWidth(), testBitmap.getHeight(), matrix, true);
	        		if(properBitmap == null) break;
	        	}
	        	testBitmap.recycle();
	        	
	        	ge.addFrame(properBitmap);
	        	this.publishProgress(++processed_count);
	        	properBitmap.recycle();
			}
			ge.finish();
			dst.close();
		}
		catch(Exception ex){
			if(dst != null) try{ dst.close(); }catch(Exception e){}
			try{ ImageHelper.deleteGif(this.mOutputPath, this.activity); }catch(Exception e){}
			Log.d(TAG, ""+ex.getMessage());
			return false;
		}
		return true;
	}
	
	@Override
	protected void onProgressUpdate (Integer... values){
		if(this.activity != null)
			this.activity.updateEncoderProgress(values[0]);
	}

	@Override
	protected void onPostExecute(Boolean result) {
		if(this.activity != null)
			activity.finishedDecodingGif(this.mOutputPath);
	}
	
	public void detach() {
		this.activity=null;
	}

	public void attach(PreviewActivity activity) {
		this.activity=activity;
	}
}
