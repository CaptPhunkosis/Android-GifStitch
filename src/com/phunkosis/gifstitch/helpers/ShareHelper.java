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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.phunkosis.gifstitch.R;
import com.phunkosis.gifstitch.settings.GSSettings;

public class ShareHelper {
	private static final String GSS = "";  //Sorry...this is secret.
	private static final String SHAREURL = "";  //Sorry...this is secret.
	
	public static void startShareLinkIntent(Activity activity, String url){
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_TEXT, activity.getResources().getString(R.string.share_link_body)+" "+url);
		intent.putExtra(Intent.EXTRA_SUBJECT, activity.getResources().getString(R.string.share_link_subject));
		intent.setType("text/plain");
		activity.startActivity(Intent.createChooser(intent, "Share "));
	}
	
	
	public static String uploadToSite(String filePath, Context c){
		URLStorageHelper storage = new URLStorageHelper(c);
		String url = storage.lookupUrl(filePath);
		if(url != null){
			return url;
		}
		
		String did = GSSettings.getDeviceId();
		File file = new File(filePath);
		String seed = ""+GSS+did+file.getName();
		String hash = generateSHA256(seed);
		
		HttpClient httpClient = new DefaultHttpClient();
		
		try{
			HttpPost httpPost = new HttpPost(SHAREURL);
			FileBody fileBody = new FileBody(file);
			StringBody didBody = new StringBody(did);
			StringBody hashBody = new StringBody(hash);
			StringBody filenameBody = new StringBody(file.getName());
			
			MultipartEntity mpe = new MultipartEntity();
			mpe.addPart("did", didBody);
			mpe.addPart("hash", hashBody);
			mpe.addPart("img", filenameBody);
			mpe.addPart("sharedgif", fileBody);
			
			httpPost.setEntity(mpe);
			HttpResponse response = httpClient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			if(entity != null){
				BufferedReader r = new BufferedReader(new InputStreamReader(entity.getContent()));
				String line = r.readLine();

				if(line != null && line.startsWith("http:")){
					storage.addRow(filePath, line);
					return line;
				}
				
				return line;
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            try { httpClient.getConnectionManager().shutdown(); } catch (Exception ignore) {}
        }
		return null;
	}
	
	private static String generateSHA256(String offThis){
		String result = "";
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(offThis.getBytes());
			byte mdBytes[] = md.digest();
			result = bytesToHex(mdBytes);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		return result;
	}
	
	
	public static String bytesToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            buf.append(byteToHex(data[i]));
        }
        return (buf.toString());
    }
	
	public static String byteToHex(byte data) {
        StringBuffer buf = new StringBuffer();
        buf.append(toHexChar((data >>> 4) & 0x0F));
        buf.append(toHexChar(data & 0x0F));
        return buf.toString();
    }
	
	public static char toHexChar(int i) {
        if ((0 <= i) && (i <= 9)) {
            return (char) ('0' + i);
        } else {
            return (char) ('a' + (i - 10));
        }
    }
	
	public static void deleteFileUrlReference(String filePath, Context c){
		URLStorageHelper storage = new URLStorageHelper(c);
		storage.deleteRow(filePath);
	}
	
	
	private static class URLStorageHelper extends SQLiteOpenHelper {
		
		private static final String DB_NAME = "gifstitch";
		private static final int DB_VERSION = 1;
	    private static final String TABLE_NAME = "sharedurls";
	    private static final String COL_GIFPATH = "GifPath";
	    private static final String COL_URL = "Url";
	    
	    public URLStorageHelper(Context context)
		{
			super(context, DB_NAME, null, DB_VERSION);
		}
	    
		public URLStorageHelper(Context context, String name, CursorFactory factory, int version) {
			super(context, name, factory, version);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			String query = "CREATE TABLE " + TABLE_NAME + " (" + COL_GIFPATH + " TEXT, " + COL_URL + " TEXT);";
			db.execSQL(query);
		}
		
		public String lookupUrl(String gifPath){
			Cursor cursor;
			try
			{
				SQLiteDatabase db = this.getReadableDatabase();
				cursor = db.query
				(
						TABLE_NAME,
						new String[] { COL_URL },
						COL_GIFPATH + "='" + gifPath +"'",
						null, null, null, null, null
				);
				
				if(cursor.moveToFirst())
					return cursor.getString(0);
			}
			catch (Exception e) 
			{
				Log.e("DB ERROR", e.toString());
				e.printStackTrace();
			}
			return null;
		}
		
		public void addRow(String gifPath, String url){
			ContentValues values = new ContentValues();
			values.put(COL_GIFPATH, gifPath);
			values.put(COL_URL, url);
			
			try{
				SQLiteDatabase db = this.getWritableDatabase();
				db.insert(TABLE_NAME, null, values);
			}
			catch(Exception ex){
				Log.e("DB ERROR", ex.toString());
				ex.printStackTrace();
			}
			
		}
		
		public void deleteRow(String gifPath){
			try{
				SQLiteDatabase db = this.getWritableDatabase();
				db.delete(TABLE_NAME, COL_GIFPATH + "='" + gifPath +"'", null);
			}
			catch(Exception ex){
				Log.e("DB ERROR", ex.toString());
				ex.printStackTrace();
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
		
		
	}
}
