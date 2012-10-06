/**
 * @file
 * Copyright (c) 2011 Marshall Farrier
 */
package com.codemelon.chesswithhumans;

import static com.codemelon.chesswithhumans.Constants.*;

// import com.codemelon.chesswithfriends.R;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * @author Marshall Farrier
 * @version 0.1 2/12/11
 * This activity is called when the game is actually over.
 * Shows a screen with a custom message depending on game result.
 */

public class GameOver extends Activity implements OnClickListener {
	private static final String TAG = "cwfGameOver";
	// Cf. respective constants
	// Result with respect to color
	private int colorResult;
	// Result with respect to user
	private int selfResult;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        
        colorResult = getIntent().getIntExtra(COLOR_RESULT, -1);
        selfResult = getIntent().getIntExtra(SELF_RESULT, -1);
        
        Log.d(TAG, "inflating view");
        setContentView(R.layout.game_over);
        
        findViewById(R.id.game_over_confirm).setOnClickListener(this);
    }
	
	public int colorResult() {
		return colorResult;
	}
	public int selfResult() {
		return selfResult;
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.game_over_confirm) {
			setResult(RESULT_OK);
			finish();
		}		
	}
}
