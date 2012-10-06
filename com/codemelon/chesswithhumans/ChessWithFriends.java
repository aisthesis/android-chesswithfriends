/**
 * @file
 * Copyright (c) 2011 Marshall Farrier
 */
package com.codemelon.chesswithhumans;

/**
 * @author Marshall Farrier
 * @version 0.1 3/12/11
 * 
 * Issues 3/12/11:
 * Synchronization with database:
 * 	1) 	A challenge you made has been accepted:
 * 		- create player locally if necessary
 * 		- create game locally
 * 		- delete challenge on server
 * 	2)	A challenge you made has been declined:
 * 		- delete challenge on server
 * 	3)	Get your open games from remote database
 * 		and add games as needed (don't delete local games, though)
 * 
 * 	Synchronize moves when preparing to play game (resume/play button)
 *  3/18/11 remaining:
 *  - Offer draw
 *  - Screen showing that opponent resigned, called draw, etc. Call this when opening the game. Similar aesthetic to dialog
 *    for resign, call draw, etc.
 * 
 * 	3/13/11 remaining to implement:
 * 	- Done: Update moves from server before entering SelectSavedGame activity
 *  - Done: Send any moves to server that were not received (also before entering SelectSavedGame).
 *    !! Note that the game result locally (in case of checkmate, resign, etc.) 
 *    should be entered only AFTER the server
 *    has received the notification. Otherwise, if the connection was bad on the checkmate move,
 *    we'll be left with a dangling game
 *    Game result should be entered into remote database when the player who has received the
 *    move opens that game--i.e., when a remote move causes checkmate, stalemate, etc.
 *  - Done: Implement sending moves to server as they are made (from Game activity or possibly BoardView)
 *  - Implement offering a draw
 *  - Done: Put "Your move!" or "Opponent's move" message in BoardView
 *  - Done: Put "Press for options" in large light text at bottom of BoardView when confirm-abort isn't present
 *  - Process games where opponent resigned or a draw was called in local database
 *  - Done: implement a time-out for server connection
 *  
 *  - Draw offer accepted: A sends draw offer with DRAW_OFFERED as local and remote result. B then enters DRAW_OFFERED
 *    as local result (to be processed later when opening game). Now if B accepts the draw offer, DRAW is entered locally and remotely.
 *    So, A now enters DRAW_OFFER_ACCEPTED locally to see result.
 *    
 * 	Problems:
 *  - After a game has been finished, it seems to appear in play list (called by main "Play" button). Correction attempt 3/18/11:
 *    putting finish() in the onPause() method for SelectSavedGame class, but I haven't verified whether the problem was solved.
 *    Same game is not showing up as finished locally. 3/18/11: Problem seems to have been resolved: Views weren't restrictive enough,
 *    so I distinguished between UNCONFIRMED_GAME_VIEW (final result entered because confirmation not
 *    received that remote database has message) and UNFINISHED_GAME_VIEW (game must be strictly open).
 */
import static com.codemelon.chesswithhumans.Constants.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.ListIterator;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class ChessWithFriends extends Activity implements OnClickListener {
	private static final String TAG = "ChessWithHumans";
	private ChessDataBaseAdapter db;
	private int selfId;
	boolean registered;
	LinkedList<String> challengesUpdate;
	private String gamesToCreate;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Set up listeners for the buttons
        View playButton = findViewById(R.id.play_button);
        View acceptChallengeButton = findViewById(R.id.accept_challenge_button);
        View resumeButton = findViewById(R.id.resume_button);
        View findPlayersButton = findViewById(R.id.find_players_button);
        View exitButton = findViewById(R.id.exit_button);
        playButton.setOnClickListener(this);
        acceptChallengeButton.setOnClickListener(this);
        resumeButton.setOnClickListener(this);
        findPlayersButton.setOnClickListener(this);
        exitButton.setOnClickListener(this);
        
        db = new ChessDataBaseAdapter(this);
        challengesUpdate = new LinkedList<String>();
        gamesToCreate = "";
        selfId = -1;
        /* Check registration data
         * If empty, prompt for registration
         * Otherwise, retrieve selfId and handle
         */
        register();
        db.open();
        selfId = db.getSelfId();
        db.close();
        if (selfId > 0) {
        	registered = true;
        	String[] params = {String.valueOf(selfId)};
        	new GetChallengeAnswersTask().execute(params);
        }
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	if (!registered) {
	    	db.open();
	        selfId = db.getSelfId();
	        db.close();
	        if (selfId > 0) registered = true;
    	}
    }
    
    public void onClick(View v) {
    	Intent i;
    	switch (v.getId()) {
    	case R.id.play_button:
    		if (registered) {
	    		i = new Intent(this, InvitePlayer.class);
	    		startActivity(i);
    		}
    		else {
    			startRegistrationActivity();
    		}
    		break;
    	case R.id.accept_challenge_button:
    		if (registered) {
	    		answerChallenge();
    		}
    		else {
    			startRegistrationActivity();
    		}
    		break;
    	case R.id.resume_button:
    		if (registered) {
    			playGame();
    		}
    		else {
    			startRegistrationActivity();
    		}
    		break;
    	case R.id.find_players_button:
    		if (registered) {
    			findPlayers();
    		}
    		else {
    			startRegistrationActivity();
    		}
    		break;
    	case R.id.exit_button:
    		finish();
    		break;
    	}
    }
    
    private void findPlayers() {
    	startActivity(new Intent(this, FindPlayer.class));
    }
    
    /**
     * Calls registration screen if necessary.
     * Otherwise just enters self data from database.
     */
    private void register() {
    	registered = false;
        
        db.open();
        Cursor c = db.getSelfCursor();       
        
        if (c.moveToFirst()) {
        	registered = true;
        	c.close();
        	db.close();
        }
        else {	// Player has not yet registered
        	c.close();
        	db.close();
        	startRegistrationActivity();      	
        }        
    }
    
    private void startRegistrationActivity() {
		startActivity(new Intent(this, Register.class));
    }
    
    // http://www.screaming-penguin.com/node/7746
    private class GetChallengeAnswersTask extends AsyncTask<String, Void, LinkedList<String> > {
    	private final ProgressDialog dialog = new ProgressDialog(ChessWithFriends.this);
    	
    	@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		dialog.setMessage("Retrieving challenge updates...");
    		dialog.show();
    	}
		@Override
		protected LinkedList<String> doInBackground(String... params) {
			LinkedList<String> response = new LinkedList<String>();
			try {
				Log.d(TAG, "retrieving challenge updates for " + params[0]);
				String postContent = "source_id=" + URLEncoder.encode(params[0], "UTF-8");
				response = ChessConnectionAdapter.getPhpPostResponse(VIEW_CHALLENGE_ANSWERS_SCRIPT, postContent);
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
				Log.e(TAG, "failure querying challenges from server");
			}
			else if (response.getFirst().equals("empty")) {
				showMessage("No challenges were accepted.");
			}
			else {
				Log.d(TAG, response.size() + " updates found");
				processAcceptedChallenges(response);
				/*
				challengesUpdate.addAll(response);
				addPlayersForAcceptedChallenges();
				*/
			}	
		}
    }
    
    private class CreateGamesTask extends AsyncTask<String, Void, LinkedList<String> > {
    	private final ProgressDialog dialog = new ProgressDialog(ChessWithFriends.this);
    	
    	@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		dialog.setMessage("Retrieving game info...");
    		dialog.show();
    	}
		@Override
		protected LinkedList<String> doInBackground(String... params) {
			LinkedList<String> response = new LinkedList<String>();
			try {
				Log.d(TAG, "retrieving game info for " + params[0]);
				String postContent = "game_list=" + URLEncoder.encode(params[0], "UTF-8");
				response = ChessConnectionAdapter.getPhpPostResponse(GET_GAME_PLAYERS_SCRIPT, postContent);
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
				Log.e(TAG, "failure querying challenges from server");
			}
			else if (response.getFirst().equals("empty")) {
				Log.e(TAG, "Games not found.");
			}
			else {
				if (response.size() == 1) {
					showMessage("1 challenge has been accepted!");
				}
				else {
					showMessage(response.size() + " challenges have been accepted!");
				}				
				createGamesForAcceptedChallenges(response);
			}	
		}
    }
    
    private void createGamesForAcceptedChallenges(LinkedList<String> response) {
    	if (response.isEmpty()) return;
    	String[] tmp;
    	db.open();
    	for (String line : response) {
    		tmp = line.split(",");
    		if (tmp.length != 3) {	// Corrupted value
    			StringBuilder badBuilder = new StringBuilder();
    			int len = tmp.length;
    			for (int i = 0; i < len; ++i) {
    				badBuilder.append(tmp[i]).append(",");
    			}
    			Log.e(TAG, "Bad string: " + badBuilder);
    			return;
    		}
    		db.newGame(Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]), Integer.parseInt(tmp[2]));
    	}
    	db.close();
    	Log.d(TAG, "new games entered into local database");
    	Log.d(TAG, "emptying challengesUpdate");
    	challengesUpdate.clear();
    }
    
    private void answerChallenge() {
    	Intent i = new Intent(this, AnswerChallenge.class);
    	startActivity(i);
    }
    
    private void showMessage(String msg) {
		Toast t = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}
    
    private void addPlayersForAcceptedChallenges() {
    	if (challengesUpdate.isEmpty()) {
    		Log.d(TAG, "addPlayersForAcceptedChallenges(): challengesUpdate is empty");
    		return;
    	}
    	Log.d(TAG, "adding players from challengesUpdate");
    	StringBuilder gameListBuilder = new StringBuilder();
    	ListIterator<String> it = challengesUpdate.listIterator();
    	String[] tmp;
    	boolean first = true;
    	// Create comma-separated list of games to add
    	// Enter players into db if necessary
    	db.open();
    	while (it.hasNext()) {
    		tmp = it.next().split(",");
    		if (tmp.length != 3) { // Corrupted data
    			StringBuilder badBuilder = new StringBuilder();
    			int len = tmp.length;
    			for (int i = 0; i < len; ++i) {
    				badBuilder.append(tmp[i]).append(",");
    			}
    			Log.e(TAG, "Bad string: " + badBuilder);
    			break;	
    		}
    		if (!first) gameListBuilder.append(",");
    		else first = false;
    		gameListBuilder.append(tmp[0]);
    		if (db.findPlayer(Integer.parseInt(tmp[1])) == -1) {
    			db.addPlayer(Integer.parseInt(tmp[1]), tmp[2]);
    		}
    	}
    	db.close();
    	gamesToCreate = gameListBuilder.toString();
    	Log.d(TAG, "gamesToCreate: " + gamesToCreate);
    	if (gamesToCreate.length() > 0) {
    		String[] params = {gamesToCreate};
    		new CreateGamesTask().execute(params);
    	}
    }
    
    private void processAcceptedChallenges(LinkedList<String> response) {
    	ListIterator<String> it = response.listIterator();
    	String line, oppHandle;
    	String[] values;
    	int gid, whiteId, blackId, oppId = 0;
    	
    	db.open();
    	while (it.hasNext()) {
    		line = it.next();
    		values = line.split(",");
    		gid = Integer.parseInt(values[0]);
    		whiteId = Integer.parseInt(values[1]);
    		blackId = Integer.parseInt(values[2]);
    		oppHandle = values[3];
    		if (selfId == whiteId) {
    			oppId = blackId;
    		}
    		else {
    			oppId = whiteId;
    		}
    		if (db.findPlayer(oppId) == -1) {
    			db.addPlayer(oppId, oppHandle);
    		}
    		db.newGame(gid, whiteId, blackId);
    	}
    	db.close();
    }
    
    // TODO
    /**
     * Called from playGame() to synchronize moves and game results
     */
    private class SynchronizeGamesTask extends AsyncTask<String, Void, LinkedList<String> > {
    	private final ProgressDialog dialog = new ProgressDialog(ChessWithFriends.this);
    	
    	@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		dialog.setMessage("Synchronizing with server ...");
    		dialog.show();
    	}
		@Override
		protected LinkedList<String> doInBackground(String... params) {
			LinkedList<String> response = new LinkedList<String>();
			try {
				Log.d(TAG, "sending move_list: " + params[0]);
				Log.d(TAG, "sending game_results: " + params[1]);
				String postContent = "move_list=" + URLEncoder.encode(params[0], "UTF-8") +
					"&game_results=" + URLEncoder.encode(params[1], "UTF-8");
				response = ChessConnectionAdapter.getPhpPostResponse(UPDATE_MOVES_AND_RESULTS_SCRIPT, postContent);
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
				Log.e(TAG, "SynchronizeGamesTask: Could not connect to server");
				startSelectSavedGame();
			}
			else {
				Log.d("SynchronizeGamesTask response line: ", response.getFirst());
				processMovesAndResults(response.getFirst());
			}			
		}
    }
    
    private class SendLostMovesTask extends AsyncTask<String, Void, LinkedList<String> > {
    	private final ProgressDialog dialog = new ProgressDialog(ChessWithFriends.this);
    	
    	@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		dialog.setMessage("Sending moves...");
    		dialog.show();
    	}
		@Override
		protected LinkedList<String> doInBackground(String... params) {
			LinkedList<String> response = new LinkedList<String>();
			try {
				Log.d(TAG, "sending lost moves: " + params[0]);
				String postContent = "move_list=" + URLEncoder.encode(params[0], "UTF-8");
				response = ChessConnectionAdapter.getPhpPostResponse(UPDATE_MOVES_AND_RESULTS_SCRIPT, postContent);
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
				Log.e(TAG, "Could not connect to server");
			}
			else if (response.getFirst().matches("[0-9][0-9]?:failure")) {
				Log.e(TAG, "failed to insert moves: " + response.getFirst());
			}
			else if (response.getFirst().matches("[0-9][0-9]?:success")){
				Log.d(TAG, "moves successfully inserted: " + response.getFirst());
			}
			else {
				Log.d(TAG, response.getFirst());
			}
			startSelectSavedGame();			
		}
    }
    
    private void playGame() {
    	// TODO
    	/*
    	 * This is insufficient input for update_moves_and_results.php
    	 * We need:
    	 * 1) a move string with all last moves
    	 * 2) a game string that also includes results
    	 */
    	db.open();
    	String lastMoves = db.getAllLastMovesString();
    	String gameResults = db.getUnconfirmedGameResultsString();
    	db.close();
    	if (gameResults.length() == 0) {
    		showMessage("No open games!");
    		return;
    	}
		String[] params = {lastMoves, gameResults};
		new SynchronizeGamesTask().execute(params);    	
    }
    
    /**
     * Called from SynchronizeGamesTask.onPostExecute()
     * and from processMovesAndResults()
     */
    private void startSelectSavedGame() {
    	Intent i = new Intent(this, SelectSavedGame.class);
		startActivity(i);
    }
    
    /**
     * Input is a response string from the server script UPDATE_MOVES_AND_RESULTS_SCRIPT
     * Method called in SynchronizeGamesTask.onPostExecute()
     * @param response
     */
    private void processMovesAndResults(String response) {
    	String[] movesAndResults = response.split(";");
    	int len = movesAndResults.length;
    	final int beginIndex = 2;
    	LinkedList<String> serverMoves = new LinkedList<String>();
    	LinkedList<String> serverResults = new LinkedList<String>();
    	for (int i = 0; i < len; ++i) {
    		if (movesAndResults[i].startsWith("m,")) {
    			serverMoves.add(movesAndResults[i].substring(beginIndex));
    		}
    		else if (movesAndResults[i].startsWith("g,")) {
    			serverResults.add(movesAndResults[i].substring(beginIndex));
    		}
    	}
    	db.open();
    	db.processMovesFromServer(serverMoves);
    	db.processResultsFromServer(serverResults);
    	db.close();
    	startSelectSavedGame();
    }
}