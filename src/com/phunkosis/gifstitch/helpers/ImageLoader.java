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

import java.util.Collections;
import java.util.Map;
import java.util.Stack;
import java.util.WeakHashMap;

import com.phunkosis.gifstitch.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

/*
 * A Good Example for this can be seen at:  https://github.com/thest1/LazyList
 */

public class ImageLoader {
    
    private Map<ImageView, String> imageViews=Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
    
    public ImageLoader(Context context){
        //Make the background thread low priority. This way it will not affect the UI performance
        photoLoaderThread.setPriority(Thread.NORM_PRIORITY-1);
    }
    
    final int stub_id=R.drawable.stub;
    public void DisplayImage(String url, Activity activity, ImageView imageView, int width, int height, boolean ifExistsUseThumb)
    {
        imageViews.put(imageView, url);
        queuePhoto(url, activity, imageView, width, height, ifExistsUseThumb);
        imageView.setImageResource(stub_id);
            
    }
        
    private void queuePhoto(String url, Activity activity, ImageView imageView, int width, int height, boolean ifExistsUseThumb)
    {
        //This ImageView may be used for other images before. So there may be some old tasks in the queue. We need to discard them. 
        photosQueue.Clean(imageView);
        PhotoToLoad p=new PhotoToLoad(url, imageView, width, height, ifExistsUseThumb);
        synchronized(photosQueue.photosToLoad){
            photosQueue.photosToLoad.add(p);
            photosQueue.photosToLoad.notifyAll();
        }
        
        //start thread if it's not started yet
        if(photoLoaderThread.getState()==Thread.State.NEW)
            photoLoaderThread.start();
    }
    
    private Bitmap getBitmap(String url, int w, int h, boolean ifExistsUseThumb) 
    {
        try {
        	Bitmap bitmap = null;
        	if(ifExistsUseThumb){
        		bitmap = ImageHelper.getGifThumbnail(url, true);
        	}
        	
        	if(bitmap == null){
        		int desired_size = w > h ? w : h;
        		bitmap = ImageHelper.getDesirableBitmap(url, desired_size);
        	}
        	
            return bitmap;
        } catch (Exception e) {}
        return null;
    }
    
    //Task for the queue
    private class PhotoToLoad
    {
        public String url;
        public ImageView imageView;
        public int width;
        public int height;
        public boolean useThumb;
        public PhotoToLoad(String u, ImageView i, int w, int h, boolean ifExistsUseThumb){
            url=u; 
            imageView=i;
            width = w;
            height = h;
            useThumb = ifExistsUseThumb;
        }
    }
    
    PhotosQueue photosQueue=new PhotosQueue();
    
    public void stopThread()
    {
        photoLoaderThread.interrupt();
    }
    
    //stores list of photos to download
    class PhotosQueue
    {
        private Stack<PhotoToLoad> photosToLoad=new Stack<PhotoToLoad>();
        
        //removes all instances of this ImageView
        public void Clean(ImageView image)
        {
        	try{
	            for(int j=0 ;j<photosToLoad.size();){
	                if(photosToLoad.get(j).imageView==image)
	                    photosToLoad.remove(j);
	                else
	                    ++j;
	            }
        	}catch(Exception ex){
	        	ex.printStackTrace();
	        }
        }
    }
    
    class PhotosLoader extends Thread {
        public void run() {
            try {
                while(true)
                {
                    //thread waits until there are any images to load in the queue
                    if(photosQueue.photosToLoad.size()==0)
                        synchronized(photosQueue.photosToLoad){
                            photosQueue.photosToLoad.wait();
                        }
                    if(photosQueue.photosToLoad.size()!=0)
                    {
                        PhotoToLoad photoToLoad;
                        synchronized(photosQueue.photosToLoad){
                            photoToLoad=photosQueue.photosToLoad.pop();
                        }
                        Bitmap bmp=getBitmap(photoToLoad.url, photoToLoad.width, photoToLoad.height, photoToLoad.useThumb);
                        String tag=imageViews.get(photoToLoad.imageView);
                        if(tag!=null && tag.equals(photoToLoad.url)){
                            BitmapDisplayer bd=new BitmapDisplayer(bmp, photoToLoad.imageView);
                            Activity a=(Activity)photoToLoad.imageView.getContext();
                            a.runOnUiThread(bd);
                        }
                    }
                    if(Thread.interrupted())
                        break;
                }
            } catch (InterruptedException e) {
                //allow thread to exit
            }
        }
    }
    
    PhotosLoader photoLoaderThread=new PhotosLoader();
    
    //Used to display bitmap in the UI thread
    class BitmapDisplayer implements Runnable
    {
        Bitmap bitmap;
        ImageView imageView;
        public BitmapDisplayer(Bitmap b, ImageView i){bitmap=b;imageView=i;}
        public void run()
        {
            if(bitmap!=null)
                imageView.setImageBitmap(bitmap);
            else
                imageView.setImageResource(stub_id);
        }
    }
}
