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
package com.phunkosis.gifstitch.settings;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;


public class GSSettings {
	private static final String DEFAULTROOT = new File(Environment.getExternalStorageDirectory(), "gifstitch").getAbsolutePath();
	private static final int DEFAULTGIFNAMECOUNT = 1;
	private static final int DEFAULTFRAMEDELAY = 500;
	
	public static final int MAX_FRAMEDELAY = 2000;
	public static final int MIN_FRAMEDELAY = 10;
	
	private static final int GIFSIZE_SMALL = 192;
	private static final int GIFSIZE_MED = 384;
	private static final int GIFSIZE_LARGE = 512;
	
	public enum Setting{
		STR_ROOTDIR,
		STR_DEVICEID,
		INT_GIFNAMECOUNT,
		INT_GIFQUALITY,
		INT_GIFOUTPUTSIZE,
		INT_GIFFRAMEDELAY,
		LONG_FIRSTLAUNCH,
		LONG_LAUNCHCOUNT,
		LONG_GIFCREATECOUNT,
		BOOL_SHOULDAUTOFOCUS,
		BOOL_SHOULDONIONSKIN,
		BOOL_ARECREATETOOLSOPEN,
		BOOL_AREEFFECTSTOOLSOPEN,
		BOOL_SHOWRATEDIALOG;
	}
	
	//I have no idea if this is how to use java enums correctly.
	//More of a python dict guy myself...
	//TODO: Google this shit.
	public enum GIFOUTPUTSIZE{
		SMALL(GIFSIZE_SMALL),
		MEDIUM(GIFSIZE_MED),
		LARGE(GIFSIZE_LARGE);
		
		private int size;
		private GIFOUTPUTSIZE(int size) {
			this.size = size;
		}
		public int size(){return this.size;}
		public static GIFOUTPUTSIZE find(int size){
			for(GIFOUTPUTSIZE gos : GIFOUTPUTSIZE.values()){
				if(gos.size == size)
					return gos;
			}
			return SMALL; //DEFAULT
		}
	}
	
	
	
	private static Context mContext;
	
	public static void InitializeSettings(Context c){
		mContext = c;
	}
		
	public static void deleteDirectory(File f){
		if(f.isDirectory()){
			for(File c : f.listFiles())
				deleteDirectory(c);
		}
		
		f.delete();
	}
	
	
	public static void copyFolder(File srcFolder, File destFolder) throws IOException{
        if (srcFolder.isDirectory()) {
            if (! destFolder.exists()) {
            	destFolder.mkdir();
            }
 
            String[] oChildren = srcFolder.list();
            for (int i=0; i < oChildren.length; i++) {
                copyFolder(new File(srcFolder, oChildren[i]), new File(destFolder, oChildren[i]));
            }
        } 
        else 
        {
            if(destFolder.isDirectory()){
                copyFile(srcFolder, new File(destFolder, srcFolder.getName()));
            }
            else{ 
                copyFile(srcFolder, destFolder);
            }
        }
    }
	
	
	public static void copyFile(File srcFile, File destFile) throws IOException 
	{
	        InputStream oInStream = new FileInputStream(srcFile);
	        OutputStream oOutStream = new FileOutputStream(destFile);
	 
	        // Transfer bytes from in to out
	        byte[] oBytes = new byte[1024];
	        int nLength;
	        BufferedInputStream oBuffInputStream = new BufferedInputStream( oInStream );
	        while ((nLength = oBuffInputStream.read(oBytes)) > 0){
	        	oOutStream.write(oBytes, 0, nLength);
	        }
	        oInStream.close();
	        oOutStream.close();
	}
	
	
	
	//ARRRGHGhGH  GETTERS/SETTERS!!  Gotta be a better way.
	public static String getRoot(){
		//stupidRootFix();
		String value = getStringSetting(Setting.STR_ROOTDIR);
		if(value == null){
			addStringSetting(Setting.STR_ROOTDIR, DEFAULTROOT);
			value = DEFAULTROOT;
		}
		return value;
	}
	public static void setRoot(String value){
		addStringSetting(Setting.STR_ROOTDIR, value);
	}
	
	public static String getDeviceId(){
		String value = getStringSetting(Setting.STR_DEVICEID);
		
		if(value == null){
			String newDeviceId = UUID.randomUUID().toString().replace("-", "");
			addStringSetting(Setting.STR_DEVICEID, newDeviceId);
			return newDeviceId;
		}
		return value;
	}
	
	public static int getGifFilenameCount(){
		int value = getIntSettings(Setting.INT_GIFNAMECOUNT, -1);
		if(value == -1){
			addIntSetting(Setting.INT_GIFNAMECOUNT, DEFAULTGIFNAMECOUNT);
			value = DEFAULTGIFNAMECOUNT;
		}
		return value;
	}
	public static void setGifFilenameCount(int value){
		addIntSetting(Setting.INT_GIFNAMECOUNT, value);
	}
	
	public static GIFOUTPUTSIZE getGifOutputSize(){
		int value = getIntSettings(Setting.INT_GIFOUTPUTSIZE, -1);
		return GIFOUTPUTSIZE.find(value);
	}
	public static void setGifOutputSize(GIFOUTPUTSIZE value){
		addIntSetting(Setting.INT_GIFOUTPUTSIZE, value.size());
	}
	
	public static int getGifFrameDelay(){
		int value = getIntSettings(Setting.INT_GIFFRAMEDELAY, -1);
		if(value == -1){
			addIntSetting(Setting.INT_GIFFRAMEDELAY, DEFAULTFRAMEDELAY);
			value = DEFAULTFRAMEDELAY;
		}else if(value < MIN_FRAMEDELAY)
			value = MIN_FRAMEDELAY;
		else if(value > MAX_FRAMEDELAY)
			value = MAX_FRAMEDELAY;
		return value;
	}
	public static void setGifFrameDelay(int value){
		addIntSetting(Setting.INT_GIFFRAMEDELAY, value);
	}
	
	public static boolean getShouldAutoFocus(){
		return getBooleanSetting(Setting.BOOL_SHOULDAUTOFOCUS);
	}
	
	public static void setShouldAutoFocus(boolean value){
		addBooleanSetting(Setting.BOOL_SHOULDAUTOFOCUS, value);
	}
	
	public static boolean getShouldOnionSkin(){
		return getBooleanSetting(Setting.BOOL_SHOULDONIONSKIN);
	}
	
	public static void setShouldOnionSkin(boolean value){
		addBooleanSetting(Setting.BOOL_SHOULDONIONSKIN, value);
	}
	
	public static boolean getAreCreateToolsOpen(){
		return !getBooleanSetting(Setting.BOOL_ARECREATETOOLSOPEN);
	}
	
	public static void setAreCreateToolsOpen(boolean value){
		addBooleanSetting(Setting.BOOL_ARECREATETOOLSOPEN, !value);
	}
	
	public static boolean getAreEffectsToolsOpen(){
		return !getBooleanSetting(Setting.BOOL_AREEFFECTSTOOLSOPEN);
	}
	
	public static void setAreEffectsToolOpen(boolean value){
		addBooleanSetting(Setting.BOOL_AREEFFECTSTOOLSOPEN, !value);
	}
	
	
	public static boolean getShowRatedDialog(){
		return getBooleanSetting(Setting.BOOL_SHOWRATEDIALOG);
	}
	
	public static void setShowRatedDialog(boolean value){
		addBooleanSetting(Setting.BOOL_SHOWRATEDIALOG, value);
	}
	
	public static long getGifCreateCount(){
		return getLongSettings(Setting.LONG_GIFCREATECOUNT, 0);
	}
	
	public static void setGifCreateCount(long value){
		addLongSetting(Setting.LONG_GIFCREATECOUNT, value);
	}
	
	public static long getLaunchCount(){
		return getLongSettings(Setting.LONG_LAUNCHCOUNT, 0);
	}
	
	public static void setLaunchCount(long value){
		addLongSetting(Setting.LONG_LAUNCHCOUNT, value);
	}
	
	public static long getFirstLaunch(){
		return getLongSettings(Setting.LONG_FIRSTLAUNCH, 0);
	}
	
	public static void setFirstLaunch(long value){
		addLongSetting(Setting.LONG_FIRSTLAUNCH, value);
	}
	
	
	
	
	private static void addBooleanSetting(Setting key, boolean value){
		SharedPreferences.Editor e = getPrefs().edit();
		e.putBoolean(key.name(), value);
		e.commit();
	}
	
	private static boolean getBooleanSetting(Setting key){
		return getPrefs().getBoolean(key.name(), true);
	}
	
	private static void addStringSetting(Setting key, String value){
		SharedPreferences.Editor e = getPrefs().edit();
		e.putString(key.name(), value);
		e.commit();
	}
	
	private static String getStringSetting(Setting key){
		return getPrefs().getString(key.name(), null);
	}
	
	private static void addIntSetting(Setting key, int value){
		SharedPreferences.Editor e = getPrefs().edit();
		e.putInt(key.name(), value);
		e.commit();
	}
	
	private static int getIntSettings(Setting key, int defValue){
		return getPrefs().getInt(key.name(), defValue);
	}
	
	private static long getLongSettings(Setting key, long defValue){
		return getPrefs().getLong(key.name(), defValue);
	}
	
	private static void addLongSetting(Setting key, long value){
		SharedPreferences.Editor e = getPrefs().edit();
		e.putLong(key.name(), value);
		e.commit();
	}
	private static SharedPreferences getPrefs(){
		return PreferenceManager.getDefaultSharedPreferences(mContext);
	}
}