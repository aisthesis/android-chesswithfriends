/**
 * Copyright (c) 2011 Marshall Farrier
 */
package com.codemelon.chesswithhumans;

import static com.codemelon.chesswithhumans.Constants.*;

// import com.codemelon.chesswithfriends.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * @author Marshall Farrier
 * @version 0.1 3/12/11
 *
 */
public class SelectSavedGame extends Activity implements OnClickListener {
	private static final String TAG = "cwhSelectSavedGame";
	private ResumeRadioGroup gameRadioGroup;
	private View continueButton;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.resume);        
        init();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		finish();
	}
	
	public void onClick(View v) {
		if (v.getId() == R.id.continue_button) {
			Log.d(TAG, "continuing game number " + gameRadioGroup.selectedGame());
			if (gameRadioGroup.selectedGame() > 0) {	// Otherwise do nothing
				Intent i = new Intent(this, ResumeGame.class);
				i.putExtra(OPPONENT_ID, gameRadioGroup.selectedPlayer());
				i.putExtra(GAME_ID, gameRadioGroup.selectedGame());
				startActivity(i);
				// finish();	// Close current activity
			}
		}
	}
	
	private void init() {
		continueButton = findViewById(R.id.continue_button);
		gameRadioGroup = (ResumeRadioGroup) findViewById(R.id.resume_radio);
		
		continueButton.setOnClickListener(this);
	}
}
