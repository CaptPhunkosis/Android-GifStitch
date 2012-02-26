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

import com.phunkosis.gifstitch.R;
import com.phunkosis.gifstitch.helpers.ImageLoader;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;

public class GifHomeGridAdapter extends BaseStoredGifsAdapter {
	private ImageLoader mImageLoader;
	private Activity mActivity;
	
	public GifHomeGridAdapter(Activity a) {
		super(a);
		this.mActivity = a;
		mImageLoader = new ImageLoader(this.mActivity);
	}	
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(convertView == null)
			convertView = new ImageView(this.mContext);
		
		int desiredSize = parent.getResources().getDimensionPixelSize(R.dimen.home_item_size);
		String imagePath = this.mImageIds.get(position);

		ImageView iv = (ImageView)convertView;
		iv.setLayoutParams(new GridView.LayoutParams(desiredSize,desiredSize));
		iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
		this.mImageLoader.DisplayImage(imagePath, this.mActivity, iv, desiredSize, desiredSize, true);
		return iv;
	}

}
