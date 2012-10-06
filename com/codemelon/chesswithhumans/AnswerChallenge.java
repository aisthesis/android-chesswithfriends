/**
 * Copyright (c) 2011 Marshall Farrier
 */
package com.codemelon.chesswithhumans;

import static com.codemelon.chesswithhumans.Constants.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.CursorWrapper;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

/**
 * @author Marshall Farrier
 * @version 0.1 3/10/11
 */
public class AnswerChallenge extends Activity {
	private static final String TAG = "cwhAnswerChallenge";
	private MatrixCursor challenges;
	private ChessDataBaseAdapter db;
	private int selfId;
	private String selfHandle;
	private View exitButton;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.answer_challenge);
        
        db = new ChessDataBaseAdapter(this);        
        initializeSelfData();
        
        // Listener for exit button
        exitButton = findViewById(R.id.exit_answer_challenges);
        exitButton.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		finish();
        	}
        });
    }	
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "starting RefreshChallengesTask");
		String[] params = {String.valueOf(selfId)};
		new RefreshChallengesTask().execute(params);
	}
	
	private void populateChallengesList() {
		Log.d(TAG, "populating challenger list");
		challenges.moveToFirst();
		Log.d(TAG, "first challenger: " + challenges.getString(challenges.getColumnIndex(CHALLENGE_SOURCE_HANDLE_COL)));
		String[] from = {CHALLENGE_SOURCE_HANDLE_COL};
		int[] to = {R.id.challenger_text_view};
		
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, 
				R.layout.challenger_text_view, challenges, from, to);
		ListView lv = (ListView) findViewById(R.id.view_challenges);
		lv.setAdapter(adapter);
		final CursorWrapper c = new CursorWrapper(challenges);
		
		lv.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        		// Move the cursor to the selected item
        		c.moveToPosition(pos);
        		
        		int cid = c.getInt(c.getColumnIndex("_id"));
        		int sourceId = c.getInt(c.getColumnIndex(CHALLENGE_SOURCE_ID_COL));
        		String sourceHandle = c.getString(c.getColumnIndex(CHALLENGE_SOURCE_HANDLE_COL));
        		int targetId = c.getInt(c.getColumnIndex(CHALLENGE_TARGET_COL));
        		challengeDialog(cid, sourceId, sourceHandle, targetId);
        	}
        });
	}
	
	private void challengeDialog(int cid, int sourceId, String sourceHandle, int targetId) {
		// challengeId = cid;
		switch (targetId) {
		case PLAYER_ANY_ID:
			challengeAnyDialog(cid, sourceId, sourceHandle);
			break;
		default:
			challengeMeDialog(cid, sourceId, sourceHandle);	
		}
		// challengeId = -1;
	}
	
	private void challengeAnyDialog(int cid, int sourceId, String sourceHandle) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final int challengeId = cid;
		final int srcId = sourceId;
		final String srcHandle = sourceHandle;
		builder.setMessage("Challenge from '" + sourceHandle + "' to anyone")
			.setCancelable(false)
			.setPositiveButton("Accept", new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					acceptChallenge(challengeId, srcId, srcHandle);
					dialog.dismiss();
				}
			})
			.setNegativeButton("Back", new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void challengeMeDialog(int cid, int sourceId, String sourceHandle) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final int challengeId = cid;
		final int srcId = sourceId;
		final String srcHandle = sourceHandle;
		
		builder.setMessage("Challenge from '" + sourceHandle + "' to '" + selfHandle + "'")
			.setCancelable(false)
			.setPositiveButton("Accept", new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					acceptChallenge(challengeId, srcId, srcHandle);
					dialog.dismiss();
				}
			})
			.setNeutralButton("Decline", new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					declineChallenge(challengeId);
					dialog.dismiss();
				}
			})
			.setNegativeButton("Back", new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private void initializeSelfData() {
		db.open();
		selfId = db.getSelfId();
		selfHandle = db.getSelfHandle();
		db.close();
	}
	
	private class RefreshChallengesTask extends AsyncTask<String, Void, LinkedList<String> > {
		private final ProgressDialog dialog = new ProgressDialog(AnswerChallenge.this);
		
		@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		dialog.setMessage("Loading challenges...");
    		dialog.show();
    	}
		
		@Override
		protected LinkedList<String> doInBackground(String... params) {
			LinkedList<String> response = new LinkedList<String>();
			try {
				Log.d(TAG, "selfId is " + params[0]);
				String postContent = "target_id=" + URLEncoder.encode(params[0], "UTF-8");
				response = ChessConnectionAdapter.getPhpPostResponse(VIEW_CHALLENGES_SCRIPT, postContent);
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
			else if (response.getFirst().equals("failure")) {
				Log.e(TAG, "failure querying open challenges from server");
			}
			else if (response.getFirst().equals("empty")) {
				showMessage("There are no challenges available at this time.");
			}
			else {
				String[] respStr = response.toArray(new String[0]);
				createChallengesCursor(respStr);
				populateChallengesList();
			}	
		}
	}
	
	private void showMessage(String msg) {
		Toast t = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}
	
	private void createChallengesCursor(String[] response) {
		Log.d(TAG, "creating challenges array");
		Challenge[] challengeArr = ChallengeHelper.parseChallengeList(response, CHALLENGE_OPEN);
		// _id is CHALLENGE_ID_COL
		String[] columnNames = {"_id", CHALLENGE_SOURCE_ID_COL, 
				CHALLENGE_SOURCE_HANDLE_COL, CHALLENGE_TARGET_COL};
		challenges = new MatrixCursor(columnNames, challengeArr.length);
		MatrixCursor.RowBuilder builder;
		Log.d(TAG, "creating challenges cursor");
		for (Challenge chal : challengeArr) {
			builder = challenges.newRow();
			builder.add(chal.challengeId());
			builder.add(chal.sourceId());
			builder.add(chal.sourceHandle());
			builder.add(chal.targetId());
		}
		Log.d(TAG, "challenges cursor created with " + challenges.getCount() + " row(s)");
	}
	
	private class SendChallengeAnswerTask extends AsyncTask<String, Void, LinkedList<String>> {
		private final ProgressDialog dialog = new ProgressDialog(AnswerChallenge.this);
		
		@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		dialog.setMessage("Sending answer to server...");
    		dialog.show();
    	}
		
		@Override
		protected LinkedList<String> doInBackground(String... params) {
			LinkedList<String> response = new LinkedList<String>();
			try {
				String postContent = "challenge_id=" + URLEncoder.encode(params[0], "UTF-8") +
				"&status=" + URLEncoder.encode(params[1], "UTF-8") +
				"&opponent_id=" + URLEncoder.encode(params[2], "UTF-8");
				response = ChessConnectionAdapter.getPhpPostResponse(ANSWER_CHALLENGE_SCRIPT, postContent);
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
				showMessage("Could not connect to server. Your answer was not entered.");
			}
			else if (response.getFirst().equals("failure")) {
				Log.e(TAG, "error with update challenges query on server");
				showMessage("Server error! Your answer was not entered.");
			}
			else if (response.getFirst().equals("declined")){
				// TODO				
				Log.d(TAG, "Server response: " + response.getFirst());
				showMessage("Challenge has been declined!");
				challenges.close();
		    	Log.d(TAG, "starting RefreshChallengesTask");
		    	String[] params = {String.valueOf(selfId)};
				new RefreshChallengesTask().execute(params);
				populateChallengesList();
			}
			else if (response.getFirst().matches("[0-9]+,[0-9]+,[0-9]+,.+")) {
				Log.d(TAG, "Server response: " + response.getFirst());
				createGame(response.getFirst());
				showMessage("Challenge accepted, and new game created!");
				challenges.close();
		    	Log.d(TAG, "starting RefreshChallengesTask");
		    	String[] params = {String.valueOf(selfId)};
				new RefreshChallengesTask().execute(params);
				populateChallengesList();
			}
			else {	// This isn't supposed to happen
				Log.e(TAG, "Unexpected server response: " + response.getFirst());
			}
		}
	}
	
	private void declineChallenge(int challengeId) {
		Log.d(TAG, "declining challenge " + challengeId);		
		// Send answer to server
		String[] params = {String.valueOf(challengeId), String.valueOf(CHALLENGE_DECLINED), String.valueOf(selfId)};
		new SendChallengeAnswerTask().execute(params);		
	}
	
	private void acceptChallenge(int challengeId, int srcId, String srcHandle) {
		Log.d(TAG, "accepting challenge " + challengeId);
		
		// Update remote database
		String[] params = {String.valueOf(challengeId), String.valueOf(CHALLENGE_ACCEPTED), String.valueOf(selfId)};
		new SendChallengeAnswerTask().execute(params);
	}
	
	private void createGame(String gameString) {
		String[] valueStrings = gameString.split(",");
		int gid = Integer.parseInt(valueStrings[0].trim());
		int white = Integer.parseInt(valueStrings[1].trim());
		int black = Integer.parseInt(valueStrings[2].trim());
		String srcHandle = valueStrings[3].trim();
		int oppId = white;
		if (oppId == selfId) oppId = black;
		db.open();
		if (db.findPlayer(oppId) == -1) {
			db.addPlayer(oppId, srcHandle);
		}
		db.newGame(gid, white, black);
		db.close();
	}
}
