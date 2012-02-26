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

import java.io.File;
import java.util.ArrayList;

import com.phunkosis.gifstitch.helpers.IOHelper;

import android.content.Context;
import android.widget.BaseAdapter;

public abstract class BaseStoredGifsAdapter extends BaseAdapter {
	protected Context mContext;
	protected ArrayList<String> mImageIds = new ArrayList<String>();

	public BaseStoredGifsAdapter(Context c){
		this.mContext = c;
		this.refreshItems();
	}
	
	public void refreshItems(){
		this.mImageIds = new ArrayList<String>();
		for(File file: IOHelper.getGalleryGifs())
			this.mImageIds.add(file.getAbsolutePath());
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
		if(position >= 0 && position < this.mImageIds.size())
			return this.mImageIds.get(position);
		else
			return null;
	}
	
	public void removeItem(int position){
		this.mImageIds.remove(position);
	}
}
