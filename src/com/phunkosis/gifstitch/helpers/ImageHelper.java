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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;

public class ImageHelper {
    private static final String TAG = "ImageHelper";
    private static final int THUMBNAILSIZE = 128;
    
    public static String saveTmpImage(byte[] data){
    	if(data != null){
    		try{
    			String tmpPath = IOHelper.getNextTmpImageName();
    			OutputStream fos = new FileOutputStream(tmpPath);
    			fos.write(data);
    			fos.close();
    			return tmpPath;
    			
    		}catch(IOException ex){
    			ex.printStackTrace(); //TODO: HANDLE
    		}
    	}
    	return null;
    }
    
    public static boolean deleteGif(String gifPath, Context c){
    	String thumbPath = generateGifThumbPath(gifPath); 
    	boolean success = false;
    	
    	File gifFile = new File(gifPath);
    	if(gifFile.exists() && gifFile.isFile())
    		success = new File(gifPath).delete();
    
    	if(thumbPath != null) new File(thumbPath).delete();
    	
    	ShareHelper.deleteFileUrlReference(gifPath, c);
    	return success;
    }
    
    public static String createGifThumbnail(String originalPath){
    	try{
	    	String thumbPath = generateGifThumbPath(originalPath);
	    	Bitmap originalBitmap = getDesirableBitmap(originalPath, THUMBNAILSIZE);
	    	
	    	int oWidth = originalBitmap.getWidth();
	    	int oHeight = originalBitmap.getHeight();
	    	int thumbSize = THUMBNAILSIZE;
	    	if(oWidth < thumbSize)
	    		thumbSize = oWidth;
	    	if(oHeight < thumbSize)
	    		thumbSize = oHeight;

	    	int thumbWidth = thumbSize;
	    	int thumbHeight = thumbSize;

	    	if(oWidth < oHeight){
	    		thumbHeight = (int)(oHeight * (double)thumbWidth / oWidth);
	    	}
	    	else{
	    		thumbWidth = (int)(oWidth * (double)thumbHeight / oHeight);
	    	}
	    	
	    	Bitmap thumbBitmap = Bitmap.createScaledBitmap(originalBitmap, thumbWidth, thumbHeight, false);
	    	originalBitmap.recycle();
	    	FileOutputStream fos = new FileOutputStream(thumbPath);
	    	thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
	    	fos.flush();
	    	fos.close();
	    	return thumbPath;
    	}
    	catch(Exception ex){
    		ex.printStackTrace();
    		//TODO: Handle
    	}
    	
    	return null;
    }
    
    public static Bitmap getGifThumbnail(String originalPath, boolean createIfMissing){
    	String thumbPath = generateGifThumbPath(originalPath);
    	File thumbFile = new File(thumbPath);
    	if(!thumbFile.exists() && createIfMissing){
    		createGifThumbnail(originalPath);
    	}
    	
    	if(!thumbFile.exists()) return null;
    	
    	return BitmapFactory.decodeFile(thumbPath);
    }
    
    public static String generateGifThumbPath(String imagePath){
    	return generateGifThumbPath(new File(imagePath));
    }
    
    public static String generateGifThumbPath(File imageFile){
    	if(imageFile == null) return null;
    	String filename = imageFile.getName();
    	return new File(IOHelper.getThumbnailsDirectory(), filename).getAbsolutePath()+".jpg";
    }
    
    
    public static Bitmap getDesirableBitmap(String imgPath, int desiredSize){
    	try{
			int scale = getDesirableBitmapSampleSize(imgPath, desiredSize);
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = scale;
			Bitmap originalBitmap = BitmapFactory.decodeFile(imgPath, options);
			
			int rotation = getExifOrientation(imgPath);
			if(rotation != 0){
				Matrix matrix = new Matrix();
				matrix.postRotate(rotation);
				Bitmap exifBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
				originalBitmap.recycle();
				return exifBitmap;
			}
			return originalBitmap;
    	}catch(Exception ex){
    		ex.printStackTrace();
    		//TODO: Handle
    	}
    	catch(OutOfMemoryError e){
    		e.printStackTrace();
    		//Hmmm...what to do...
    	}
    	return null;
	}
    
    
    public static int getDesirableBitmapSampleSize(String imgPath, int desiredSize){
		int scale=1;
        try{
        	BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
        	BitmapFactory.decodeStream(new FileInputStream(imgPath),null,options);
            //Find the correct scale value. It should be the power of 2.
            int width_tmp=options.outWidth, height_tmp=options.outHeight;
            while(true){
                if(width_tmp/2<desiredSize || height_tmp/2<desiredSize)
                    break;
                width_tmp/=2;
                height_tmp/=2;
                scale*=2;
            }
            return scale;

        }catch(Exception e){
        	return scale;
        }        
	}
    
	
	public static int getExifOrientation(String filepath) {
        int degree = 0;
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filepath);
        } catch (IOException ex) {
            Log.e(TAG, "cannot read exif", ex);
        }
        if (exif != null) {
            int orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, -1);
            if (orientation != -1) {
                // We only recognize a subset of orientation tag values.
                switch(orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                }

            }
        }
        return degree;
    }
}
