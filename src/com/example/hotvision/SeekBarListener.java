package com.example.hotvision;

import android.util.Log;
import android.widget.SeekBar;

public class SeekBarListener implements SeekBar.OnSeekBarChangeListener {

	public float val;
	private MainActivity caller;
	
	public SeekBarListener(MainActivity caller)
	{
		this.caller = caller;
	}
	
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        val = progress / 100.0f;
        caller.SeekBarChanged();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}