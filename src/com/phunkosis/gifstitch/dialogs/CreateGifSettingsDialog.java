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
package com.phunkosis.gifstitch.dialogs;

import java.text.DecimalFormat;

import com.phunkosis.gifstitch.R;
import com.phunkosis.gifstitch.settings.GSSettings;
import com.phunkosis.gifstitch.settings.GSSettings.GIFOUTPUTSIZE;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class CreateGifSettingsDialog extends Dialog implements OnSeekBarChangeListener, android.view.View.OnClickListener {

	TextView mFrameDelayTextView;
	SeekBar mFrameDelaySeekBar;
	
	private boolean mCanceled = false;
	
	public CreateGifSettingsDialog(Context context) {
		super(context);
		this.setContentView(R.layout.dialog_build_settings);
		
		this.mFrameDelayTextView = (TextView)this.findViewById(R.id.text_gif_framedelay);
		this.mFrameDelaySeekBar = (SeekBar)this.findViewById(R.id.sb_framedelay);
		this.mFrameDelaySeekBar.setOnSeekBarChangeListener(this);
		
		((Button)this.findViewById(R.id.butt_build_gif)).setOnClickListener(this);
		((Button)this.findViewById(R.id.butt_cancel_build)).setOnClickListener(this);
		
		this.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				CreateGifSettingsDialog.this.mCanceled = true; //This feels weird...
			}
		});
		this.setTitle(context.getResources().getString(R.string.dialog_build_title));
		this.setInitialValues();
	}
	
	public boolean isCanceled(){
		return this.mCanceled;
	}
	
	private void setInitialValues(){
		int sizeRadioId;
		switch(GSSettings.getGifOutputSize()){
		case LARGE:
			sizeRadioId = R.id.radio_img_large;
			break;
		case MEDIUM:
			sizeRadioId = R.id.radio_img_med;
			break;
		default:
			sizeRadioId = R.id.radio_img_small;
		}
		((RadioButton)this.findViewById(sizeRadioId)).setChecked(true);
		
		this.mFrameDelaySeekBar.setProgress(this.convertRealValueToSBValue(GSSettings.getGifFrameDelay()));
		this.setFrameDelayTextValue(this.convertSBValueToRealValue(this.mFrameDelaySeekBar.getProgress()));
	}
	
	private void saveCurrentValues(){
		RadioGroup sizeRG = (RadioGroup)this.findViewById(R.id.radiogroup_img_size);
		switch(sizeRG.getCheckedRadioButtonId()){
		case R.id.radio_img_large:
			GSSettings.setGifOutputSize(GIFOUTPUTSIZE.LARGE);
			break;
		case R.id.radio_img_med:
			GSSettings.setGifOutputSize(GIFOUTPUTSIZE.MEDIUM);
			break;
		default:
			GSSettings.setGifOutputSize(GIFOUTPUTSIZE.SMALL);
		}
		
		int seekBarProgress = this.mFrameDelaySeekBar.getProgress();
		GSSettings.setGifFrameDelay(this.convertSBValueToRealValue(seekBarProgress));
	}

	private void setFrameDelayTextValue(int value){
		double textValue = ((double)value)/1000.0;
		DecimalFormat rounder = new DecimalFormat("#.##");
		this.mFrameDelayTextView.setText(rounder.format(textValue));
	}
	
	private int convertSBValueToRealValue(int value){
		//Whats the equiv to python hashmapping this?
		if(value >= 90)
			return 2000;
		else if(value >= 80)
			return 1500;
		else if(value >= 70)
			return 1000;
		else if(value >= 60)
			return 750;
		else if(value >= 50)
			return 660;
		else if(value >= 40)
			return 500;
		else if(value >= 30)
			return 330;
		else if(value >= 20)
			return 250;
		else if(value >= 10)
			return 100;
		else
			return 50;
	}
	
	private int convertRealValueToSBValue(int value){
		if(value >= 2000)
			return 90;
		else if(value >= 1500)
			return 80;
		else if(value >= 1000)
			return 70;
		else if(value >= 750)
			return 60;
		else if(value >= 660)
			return 50;
		else if(value >= 500)
			return 40;
		else if(value >= 330)
			return 30;
		else if(value >= 250)
			return 20;
		else if(value >= 100)
			return 10;
		else
			return 0;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case(R.id.butt_build_gif):
			this.saveCurrentValues();
			this.dismiss();
			break;
		case(R.id.butt_cancel_build):
			this.cancel();
			break;
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		this.setFrameDelayTextValue(this.convertSBValueToRealValue(progress));
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		this.setFrameDelayTextValue(this.convertSBValueToRealValue(seekBar.getProgress()));
	}
	
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {}

}
