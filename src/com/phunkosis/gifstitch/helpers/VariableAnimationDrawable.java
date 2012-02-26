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

import android.graphics.drawable.AnimationDrawable;
import android.os.SystemClock;

public class VariableAnimationDrawable extends AnimationDrawable {
	private final static int DEFAULTDURATION = 250;
	private volatile int duration;
	private int currentFrame;
	
	public VariableAnimationDrawable(){
		this.currentFrame = 0;
		this.duration = DEFAULTDURATION;
	}
	
	@Override
	public void run(){
		int n = this.getNumberOfFrames();
		this.currentFrame++;
		if(this.currentFrame >= n) this.currentFrame = 0;
		
		this.selectDrawable(currentFrame);
		this.scheduleSelf(this, SystemClock.uptimeMillis() + this.duration);
	}
	
	public void setDuration(int duration){
		this.duration = duration;
		this.unscheduleSelf(this);
		this.selectDrawable(this.currentFrame);
		this.scheduleSelf(this, SystemClock.uptimeMillis() + this.duration);
	}
}
