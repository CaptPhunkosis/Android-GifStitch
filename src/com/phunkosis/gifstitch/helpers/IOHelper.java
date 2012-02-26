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

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.phunkosis.gifstitch.settings.GSSettings;


public class IOHelper {
	private static final String GIFNAMEPREFIX = "gifstitch_";
	private static final String TMPIMAGEPREFIX = "tmp_";
	
	public static File getRootDirectory(){
		File rootDir = new File(GSSettings.getRoot());
		if(!rootDir.exists())
			rootDir.mkdirs();
		return rootDir;
	}
	
	
	/*
	 * FILENAME FILTERS
	 */
	public static FilenameFilter gifFileFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String filename) {
			if(filename.toLowerCase().endsWith(".gif"))
				return true;
			return false;
		}
	};
	
	public static FilenameFilter imageFileFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String filename) {
			String lower = filename.toLowerCase();
			if(lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".bmp") || lower.endsWith(".gif"))
				return true;
			return false;
		}
	};
	
	
	private static Comparator<File> newToOld = new Comparator<File>() {
		public int compare(File file1, File file2) {
			if(file1.lastModified() < file2.lastModified())
				return 1;
			else if(file1.lastModified() > file2.lastModified())
				return -1;
			else
				return file1.getName().compareToIgnoreCase(file2.getName());
		}
	};
	
	private static Comparator<File> oldToNew = new Comparator<File>() {
		public int compare(File file1, File file2) {
			if(file1.lastModified() > file2.lastModified())
				return 1;
			else if(file1.lastModified() < file2.lastModified())
				return -1;
			else
				return file1.getName().compareToIgnoreCase(file2.getName());
		}
	};
	
	
	public static File getThumbnailsDirectory(){
		File thumbsDir = new File(getRootDirectory(), ".thumbnails/");
		if(!thumbsDir.exists())
			thumbsDir.mkdirs();
		return thumbsDir;
	}
	
	
	/*
	 * GALLERY METHODS
	 */
	public static File getGalleryDirectory(){
		File galDir = new File(getRootDirectory(), "GifGallery/");
		if(!galDir.exists())
			galDir.mkdirs();
		return galDir;
	}
	
	public static File[] getGalleryGifs(){
		List<File> files = Arrays.asList(getGalleryDirectory().listFiles(gifFileFilter));
		Collections.sort(files, newToOld);
		File[] fileArray = new File[files.size()];
		files.toArray(fileArray);
		return fileArray;
	}
	
	public static String getNextNewGifName(){
		int count = GSSettings.getGifFilenameCount();
		while(true){
			String countString = ""+count;
			if(count < 10)
				countString = "00"+count;
			else if(count < 100)
				countString = "0"+count;
			File nextGifName = new File(getGalleryDirectory(), GIFNAMEPREFIX+countString+".gif");
			if(!nextGifName.exists()){
				GSSettings.setGifFilenameCount(++count);
				return nextGifName.getAbsolutePath();
			}
			count++;
		}
	}
	
	
	public static File getTmpDirectory(){
		File tmpDir = new File(getRootDirectory(), "tmp/");
		if(!tmpDir.exists())
			tmpDir.mkdirs();
		return tmpDir;
	}
	
	public static File[] getTmpImages(){ 
		List<File> files = Arrays.asList(getTmpDirectory().listFiles(imageFileFilter));
		Collections.sort(files, oldToNew);
		File[] returnArray = new File[files.size()];
		files.toArray(returnArray);
		return returnArray;
	}
	
	public static String getNextTmpImageName(){
		int count = 0;
		while(true){
			File nextTmpName = new File(getTmpDirectory(), TMPIMAGEPREFIX+count+".jpg");
			if(!nextTmpName.exists())
				return nextTmpName.getAbsolutePath();
			count++;
		}
	}
	
	public static void clearTmpDirectory(){
		for(File file : getTmpDirectory().listFiles())
			file.delete();
	}
}
