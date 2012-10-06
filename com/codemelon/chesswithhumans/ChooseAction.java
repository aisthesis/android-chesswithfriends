/**
 * Copyright (c) 2011 Marshall Farrier
 */

package com.codemelon.chesswithhumans;

import static com.codemelon.chesswithhumans.Constants.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;


/**
 * @author Marshall Farrier
 * @version 0.1 2/12/11
 * 
 * Problems: 
 */

public class ChooseAction extends Activity implements OnClickListener {
	private static final String TAG = "cwhChooseAction";
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_action);
        
        // Set up listeners for the buttons
    	findViewById(R.id.pause_button).setOnClickListener(this);
    	findViewById(R.id.resign_button).setOnClickListener(this);
    	findViewById(R.id.offer_draw_button).setOnClickListener(this);
    	findViewById(R.id.call_draw_button).setOnClickListener(this);
    	findViewById(R.id.back_to_game_button).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.pause_button:
			setResult(RESULT_PAUSE_GAME);
			finish();
			break;
		case R.id.resign_button:
			setResult(RESULT_RESIGN);
			finish();
			break;
		case R.id.offer_draw_button:
			offerDraw();
			break;
		case R.id.call_draw_button:
			setResult(RESULT_CALL_DRAW);
			finish();
			break;
		case R.id.back_to_game_button:
			finish();	// Close current activity
			break;
		
		}
	}
	
	private void offerDraw() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Confirm draw offer")
		.setCancelable(false)
		.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {				
			@Override
			public void onClick(DialogInterface dialog, int which) {				
				dialog.dismiss();
				onDrawOfferConfirmed();
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {				
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void onDrawOfferConfirmed() {
		setResult(RESULT_OFFER_DRAW);
		finish();
	}
}
