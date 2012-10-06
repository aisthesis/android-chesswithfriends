/**
 * @file
 * Copyright (c) 2011 Marshall Farrier
 */
package com.codemelon.chesswithhumans;

import static com.codemelon.chesswithhumans.Constants.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;

// import com.codemelon.chesswithfriends.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioGroup;
import android.widget.Toast;

/**
 * @author Marshall Farrier
 * @version 0.1 2/4/11
 *
 */
// Cf. Burnette, p. 48
public class InvitePlayer extends Activity implements OnClickListener {
	private static final String TAG = "InvitePlayer";
	private RadioGroup playerRadioGroup;
	private View challengeSelectedButton;
	private View challengeAnyButton;
	private int selectedPlayer;
	private int selfId;
	private ChessDataBaseAdapter db;
	private Integer tmp;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.invite);        
        init();
	}
	
	// Cf. Burnette, p. 48
	public void onClick(View v) {
		if (v.getId() == R.id.challenge_any_button) {
			try {
				challenge(PLAYER_ANY_ID);
			}
			catch (UnsupportedEncodingException e) {
				Log.e(TAG, e.getMessage());
			}
		}
		else if (v.getId() == R.id.challenge_selected_button) {
			Log.d(TAG, "challenging player number " + selectedPlayer);
			if (selectedPlayer > 0) {	// Otherwise do nothing
				try {
					challenge(selectedPlayer);
				}
				catch (UnsupportedEncodingException e) {
					Log.e(TAG, e.getMessage());
				}
				/*
				Intent i = new Intent(this, NewGame.class);
				i.putExtra(OPPONENT_ID, selectedPlayer);
				startActivity(i);
				finish();	// Close current activity
				*/
			}
		}
	}
	
	private void init() {
		selectedPlayer = -1;
		challengeAnyButton = findViewById(R.id.challenge_any_button);
		challengeSelectedButton = findViewById(R.id.challenge_selected_button);
		playerRadioGroup = (RadioGroup) findViewById(R.id.players_radio);
		db = new ChessDataBaseAdapter(this);
		db.open();
		selfId = db.getSelfId();
		db.close();
		
		challengeAnyButton.setOnClickListener(this);
		challengeSelectedButton.setOnClickListener(this);
		
		playerRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				tmp = (Integer) findViewById(checkedId).getTag();
				if (tmp != null) selectedPlayer = tmp;
			}			
		});
	}
	
	private class CreateChallengeTask extends AsyncTask<String, Void, LinkedList<String> > {
		private final ProgressDialog dialog = new ProgressDialog(InvitePlayer.this);
		
		@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		dialog.setMessage("Sending challenge...");
    		dialog.show();
    	}
		
		/**
		 * params[0] is source_id (self)
		 * params[1] is target_id
		 */
		@Override
		protected LinkedList<String> doInBackground(String... params) {
			LinkedList<String> response = new LinkedList<String>();
			try {
				Log.d(TAG, "sending challenge to server");
				String postContent = "source_id=" + URLEncoder.encode(params[0], "UTF-8") + 
					"&target_id=" + URLEncoder.encode(params[1], "UTF-8");
				response = ChessConnectionAdapter.getPhpPostResponse(CREATE_CHALLENGE_SCRIPT, postContent);
			}
			catch (UnsupportedEncodingException e) {
				Log.e(TAG, e.getMessage());
			}
			return response;
		}
		

		@Override
    	protected void onPostExecute(LinkedList<String> response) {
			if (dialog.isShowing()) dialog.dismiss();
			if (response.isEmpty()) {
				showMessage("Could not connect to server. Please try again later.");
			}
			else if (response.getFirst().equals("insertion_failure")) {
				showMessage("Challenge already exists!");
			}
			else {
				showMessage("Your challenge has been entered!");
			}
		}
	}
	
	private void challenge(int playerId) throws UnsupportedEncodingException {	
		Log.d(TAG, "creating challenge");
		String[] params = {String.valueOf(selfId), String.valueOf(playerId)};
		new CreateChallengeTask().execute(params);		
	}
	
	private void showMessage(String msg) {
		Toast t = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}
}
