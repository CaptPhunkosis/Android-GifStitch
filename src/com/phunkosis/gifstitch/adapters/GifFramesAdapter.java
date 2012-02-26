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
package com.phunkosis.gifstitch.adapters;

import java.util.ArrayList;

import com.phunkosis.gifstitch.R;
import com.phunkosis.gifstitch.helpers.ImageHelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class GifFramesAdapter extends BaseAdapter {

	protected ArrayList<String> mImageIds = new ArrayList<String>();
	private LayoutInflater mInflater;

	public GifFramesAdapter(Context c, ArrayList<String> imagePaths){
		this.mInflater = LayoutInflater.from(c);
		this.mImageIds = imagePaths; 
	}
		
	@Override
	public int getCount() {
		if(this.mImageIds == null)
			return 0;
		return this.mImageIds.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	public String getItemPath(int position) {
		if(this.mImageIds == null)
			return "";
		return this.mImageIds.get(position);
	}
	
	public ArrayList<String> getItems(){
		return this.mImageIds;
	}
	
	public void removeAllItems(){
		this.mImageIds = new ArrayList<String>();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(convertView == null)
			convertView = this.mInflater.inflate(R.layout.view_frame_thumb, null);
		
		int desiredSize = convertView.getResources().getDimensionPixelSize(R.dimen.create_list_thumb_size);
		String imagePath = this.mImageIds.get(position);
		
		ImageView thumbImage = (ImageView)convertView.findViewById(R.id.img_item_icon);
		TextView itemTitle = (TextView)convertView.findViewById(R.id.tv_item_title);
		itemTitle.setText("" + (position+1));
		
		//In the event shit goes south on us memory wise layout everything preimage load.
		int outWidth = desiredSize;
		int outHeight = desiredSize;
		thumbImage.setLayoutParams(new RelativeLayout.LayoutParams(outWidth,outHeight));
		convertView.setLayoutParams(new Gallery.LayoutParams(outWidth,outHeight));
		
		Bitmap bitmap = ImageHelper.getDesirableBitmap(imagePath, desiredSize);
		if(bitmap != null){
			thumbImage.setImageBitmap(bitmap);
			
			if(bitmap.getHeight() > bitmap.getWidth()){
				outWidth = (int)(desiredSize * ((float)bitmap.getWidth() / bitmap.getHeight()));
			}else{
				outHeight = (int)(desiredSize * ((float)bitmap.getHeight() / bitmap.getWidth()));
			}
			
			thumbImage.setLayoutParams(new RelativeLayout.LayoutParams(outWidth,outHeight));
			convertView.setLayoutParams(new Gallery.LayoutParams(outWidth,outHeight));
		}
		return convertView;
	}


	public void onRemove(int which) {
		if (which < 0 || which > this.mImageIds.size()) return;		
		this.mImageIds.remove(which);
	}

	public void onDrop(int from, int to) {
		String temp = this.mImageIds.get(from);
		this.mImageIds.remove(from);
		this.mImageIds.add(to,temp);
	}
	
	public void onAdd(String newThing){
		this.mImageIds.add(newThing);
	}
	
}
