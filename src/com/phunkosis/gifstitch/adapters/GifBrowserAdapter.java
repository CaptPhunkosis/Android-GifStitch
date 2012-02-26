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
import android.widget.Gallery;
import android.widget.Gallery.LayoutParams;
import android.widget.ImageView;

public class GifBrowserAdapter extends BaseStoredGifsAdapter {

	private ImageLoader mImageLoader;
	private Activity mActivity;
	
	public GifBrowserAdapter(Activity a) {
		super(a);
		this.mActivity = a;
		this.mImageLoader = new ImageLoader(this.mActivity);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(convertView == null)
			convertView = new ImageView(this.mContext);
		
		int desiredSize = parent.getResources().getDimensionPixelSize(R.dimen.browser_thumb_size);
		String imagePath = this.mImageIds.get(position);

		ImageView iv = (ImageView)convertView;
		iv.setAdjustViewBounds(true);
		iv.setLayoutParams(new Gallery.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		this.mImageLoader.DisplayImage(imagePath, this.mActivity, iv, desiredSize, desiredSize, true);
		return iv;
	}

}
