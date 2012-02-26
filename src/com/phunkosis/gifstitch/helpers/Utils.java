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

import com.phunkosis.gifstitch.R;
import com.phunkosis.gifstitch.dialogs.DialogHelper;
import com.phunkosis.gifstitch.settings.GSSettings;


import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.view.OrientationEventListener;

public class Utils {

	public static boolean isSDCardPresent(final Activity a){
		String storageState = Environment.getExternalStorageState();
		return storageState.equals(Environment.MEDIA_MOUNTED);
	}

	public static float translateRanges(float value, float minLeft, float maxLeft, float minRight, float maxRight){
		float leftSpan = maxLeft - minLeft;
		float rightSpan = maxRight - minRight;
		float scale = (value - minLeft) / leftSpan;
		return minRight + (scale * rightSpan);
	}
	
	public static int roundOrientation(int orientationInput) {
        int orientation = orientationInput;

        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            orientation = 0;
        }

        orientation = orientation % 360;
        int retVal;
        if (orientation < (0 * 90) + 45) {
            retVal = 0;
        } else if (orientation < (1 * 90) + 45) {
            retVal = 90;
        } else if (orientation < (2 * 90) + 45) {
            retVal = 180;
        } else if (orientation < (3 * 90) + 45) {
            retVal = 270;
        } else {
            retVal = 0;
        }

        return retVal;
    }
	
	
	public static void app_launched(Context c){
		long launch_count = GSSettings.getLaunchCount() + 1;
		GSSettings.setLaunchCount(launch_count);
		
		Long date_firstLaunch = GSSettings.getFirstLaunch();
		if(date_firstLaunch == 0)
			GSSettings.setFirstLaunch(System.currentTimeMillis());
	
		if(GSSettings.getShowRatedDialog()){
			long gifsMade = GSSettings.getGifCreateCount();
			int requiredGifCount = c.getResources().getInteger(R.integer.gifs_made_before_rate_prompt);
			int requiredDaysCount = c.getResources().getInteger(R.integer.days_til_rate_prompt);
			long daysCheck = date_firstLaunch + (requiredDaysCount * 24 * 60 * 60 * 1000);
			if(System.currentTimeMillis() >= daysCheck && gifsMade >= requiredGifCount)
				DialogHelper.showRateDialog(c);
		}
		
		
	}
	
	
	//Because there are a few places...like between preview animation and encoding
	//where gc hasn't had a chance to catch up...I'm testing this panic hack.
	public static void memoryPanic(){
		try{
    		System.gc();
    		Thread.sleep(250);
		}catch(Exception ex){}
	}
	
}
