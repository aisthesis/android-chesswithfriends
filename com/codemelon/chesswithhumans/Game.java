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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

/**
 * 
 * @author Marshall Farrier
 * @version 0.1 2/10/11
 *
 */
public abstract class Game extends Activity {
	private static final String TAG = "Game";
	private int opponentId;
	private int selfId;
	private int gameId;
	/* Moves will be counted starting with 0 so as
	 * to match easily with the ArrayList of moves in the Position class.
	 * This will always "point to" the next move to be made except
	 * at the moment when that move is being entered into the db.
	 */
	private int moveNumber;
	private boolean white;	
	private int colorResult;
	private int selfResult;
	
	protected ChessDataBaseAdapter db;
	protected BoardView boardView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        
        opponentId = getIntent().getIntExtra(OPPONENT_ID, -1);
        moveNumber = 0;
        white = true; 
        gameId = 0;
        db = new ChessDataBaseAdapter(this);
        colorResult = UNFINISHED_GAME;
        selfResult = UNFINISHED_GAME;
        selfId = -1;
        selfId = getSelfId();
        Log.d(TAG, "selfId is " + selfId);
        if (selfId <= 0) {
        	finish();
        }
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Intent i;
		int provisionalResultCode;
		if (requestCode == CHOOSE_ACTION_ACTIVITY) {
			switch (resultCode) {
			case RESULT_PAUSE_GAME:
				finish();
				break;
			case RESULT_RESIGN:
				i = new Intent(this, GameOver.class);
				i.putExtra(SELF_RESULT, YOU_RESIGNED);
				db.open();
				if (white) {
					provisionalResultCode = WHITE_RESIGNED;
					i.putExtra(COLOR_RESULT, BLACK_WINS);
					Log.d(TAG, db.setGameResult(gameId, provisionalResultCode) + " row updated to conclude game");
					sendResult(provisionalResultCode);
				}
				else {
					provisionalResultCode = BLACK_RESIGNED;
					i.putExtra(COLOR_RESULT, WHITE_WINS);
					Log.d(TAG, db.setGameResult(gameId, provisionalResultCode) + " row updated to conclude game");
					sendResult(provisionalResultCode);
				}
				db.close();
				startActivityForResult(i, GAME_OVER_ACTIVITY);
				break;
			case RESULT_CALL_DRAW:
				handleCallDraw();
				break;
			case RESULT_OFFER_DRAW:
				offerDraw();
				break;
			}
		}
		else if (requestCode == GAME_OVER_ACTIVITY && resultCode == RESULT_OK) {
			finish();
		}
		else super.onActivityResult(requestCode, resultCode, data);
	}
	
	// Setters
	public void setWhite(boolean w) {
		white = w;
	}	
	public void setGameId(int gid) {
		gameId = gid;
	}	
	public void setOpponentId(int oppId) {
		opponentId = oppId;
	}	
	public void setMoveNumber(int moveNum) {
		moveNumber = moveNum;
	}
	// Getters
	public boolean white() {
		return white;
	}
	public int opponentId() {
		return opponentId;
	}
	public int gameId() {
		return gameId;
	}
	
	// Enter move into local database
	public long move(int from, int to, int piece) {
		long rowId = -1;
		db.open();
		try {
			rowId = db.move(gameId, moveNumber, from, to, piece);
		} catch (SQLiteConstraintException e) {
			Log.v(TAG, "error inserting new game row: " + e.getMessage());
		} catch (SQLiteException e) {
			Log.v(TAG, "error inserting new game row: " + e.getMessage());
		} finally {		
			db.close();
		}
		++moveNumber;
		return rowId;
	}
	
	public void sendMove(int from, int to, int piece, int result) {
		int numberToSend = moveNumber - 1;	// moveNumber has already been updated locally
		String move = gameId + "," + numberToSend + "," + from + "," + to + "," + piece;
		String gameResult = gameId + "," + result;
		String[] params = {move, gameResult};
		new SendMoveTask().execute(params);
	}
	
	private void sendResult(int gameResult) {
		String message = gameId + "," + gameResult + "," + RESULT_SIGNAL;
		String[] params = {message};
		new SendMoveTask().execute(params);
	}
	
	public int colorResult() {
		return colorResult;
	}
	public int selfResult() {
		return selfResult;
	}
	
	// Start ChooseAction activity
	public void startChooseAction() {
		Intent i = new Intent(this, ChooseAction.class);
		startActivityForResult(i, CHOOSE_ACTION_ACTIVITY);
	}
	
	public void checkmate(int from, int to, int piece, int colorResultCode) {
		int provisionalResultCode = WHITE_WINS_BY_CHECKMATE;
		if (colorResultCode == BLACK_WINS) {
			provisionalResultCode = BLACK_WINS_BY_CHECKMATE;
		}
		db.open();
		try {
			db.setGameResult(gameId, provisionalResultCode);
		} finally {
			db.close();
		}
		sendMove(from, to, piece, provisionalResultCode);
		setResultsOnCheckmate(colorResultCode);
		Intent i = new Intent(this, GameOver.class);
		i.putExtra(COLOR_RESULT, colorResult);
		i.putExtra(SELF_RESULT, selfResult);
		startActivityForResult(i, GAME_OVER_ACTIVITY);
	}
	
	public void stalemate(int from, int to, int piece) {
		colorResult = DRAW;
		selfResult = STALEMATE;
		int provisionalResultCode = DRAW_BY_STALEMATE;
		db.open();
		try {
			db.setGameResult(gameId, provisionalResultCode);
		} finally {
			db.close();
		}
		sendMove(from, to, piece, provisionalResultCode);
		Intent i = new Intent(this, GameOver.class);
		i.putExtra(COLOR_RESULT, colorResult);
		i.putExtra(SELF_RESULT, selfResult);
		startActivityForResult(i, GAME_OVER_ACTIVITY);
	}
	
	private void setResultsOnCheckmate(int colorResultCode) {
		colorResult = colorResultCode;
		if (white) {
			if (colorResult == WHITE_WINS) {
				selfResult = OPPONENT_GOT_CHECKMATED;
			}
			else {
				selfResult = YOU_GOT_CHECKMATED;
			}
		}
		else {	// You are black
			if (colorResult == BLACK_WINS) {
				selfResult = OPPONENT_GOT_CHECKMATED;
			}
			else {
				selfResult = YOU_GOT_CHECKMATED;
			}
		}
	}
	
	/**
	 * Calls GameOver activity with appropriate result code.
	 * Assumes that selfResult and colorResult are current.
	 * @param resultCode
	 */
	private void gameOver() {
		Intent i = new Intent(this, GameOver.class);
		i.putExtra(SELF_RESULT, selfResult);
		i.putExtra(COLOR_RESULT, colorResult);
		startActivityForResult(i, GAME_OVER_ACTIVITY);
	}
	
	private void handleCallDraw() {
		if (moveNumber == 0) {
			noDrawPossibleMessage();
			return;
		}
		int drawStatus = boardView.drawStatus();
		Log.d(TAG, "drawStatus retrieved. Code = " + drawStatus);
		if (drawStatus == NO_DRAW || (drawStatus == DRAW_BY_REPETITION && !boardView.myMove())) {
			noDrawPossibleMessage();
		}
		else {
			handleDraw(drawStatus);
		}	
	}
	
	public void handleDraw(int drawStatus) {
		colorResult = DRAW;
		selfResult = drawStatus;
		int provisionalResultCode = DRAW_BY_REPETITION_FOR_DB;
		switch (drawStatus) {
		case DRAW_BY_AGREEMENT:
			provisionalResultCode = DRAW_OFFER_ACCEPTED;
			break;
		case DRAW_BY_NO_MORE_CHECKMATE:
			provisionalResultCode = DRAW_BY_NO_MORE_CHECKMATE_FOR_DB;
			break;
		case DRAW_BY_REPETITION:
			provisionalResultCode = DRAW_BY_REPETITION_FOR_DB;
			break;
		case DRAW_BY_FIFTY_MOVE_RULE:
			provisionalResultCode = DRAW_BY_FIFTY_FOR_DB;
			break;
		}
		db.open();
		db.setGameResult(gameId, provisionalResultCode);
		db.close();
		sendResult(provisionalResultCode);
		Log.d(TAG, "database updated to finish game");
		gameOver();
	}
	
	public void handleDraw(int from, int to, int piece, int drawStatus) {
		colorResult = DRAW;
		selfResult = drawStatus;
		int provisionalResultCode = DRAW_BY_NO_MORE_CHECKMATE_FOR_DB;
		if (drawStatus != DRAW_BY_NO_MORE_CHECKMATE) {
			provisionalResultCode = drawStatus;
		}
		db.open();
		db.setGameResult(gameId, provisionalResultCode);
		db.close();
		Log.d(TAG, "database updated to finish game");
		sendMove(from, to, piece, provisionalResultCode);
		gameOver();
	}
	
	public int selfId() {
		return selfId;
	}
	
	public void exitGame() {
		finish();
	}
	
	private void noDrawPossibleMessage() {
		Toast noDrawToast = Toast.makeText(this, R.string.no_draw_label, Toast.LENGTH_SHORT);
		noDrawToast.setGravity(Gravity.CENTER, 0, 0);
		noDrawToast.show();
	}
	
	private int getSelfId() {
		db.open();
		int result = db.getSelfId();
		db.close();
		return result;
	}
	
	// TODO
	private class SendMoveTask extends AsyncTask<String, Void, LinkedList<String> > {
    	private final ProgressDialog dialog = new ProgressDialog(Game.this);
    	
    	@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		dialog.setMessage("Sending move...");
    		dialog.show();
    	}
		@Override
		protected LinkedList<String> doInBackground(String... params) {
			LinkedList<String> response = new LinkedList<String>();
			try {
				Log.d(TAG, "sending move: " + params[0]);
				Log.d(TAG, "sending result: " + params[1]);
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
				Log.e(TAG, "Could not connect to server");
				/*
				showMessage("Could not connect to server! Move will be " +
						"added later.");
				//*/
			}
			else if (response.getFirst().equals("failure")) {
				Log.e(TAG, "failed to insert move");
				/* 
				showMessage("Could not connect to server! Move will be " +
					"added later.");
				//*/
			}
			else if (response.getFirst().equals(":success")){
				Log.d(TAG, "move successfully inserted");
				/*
				showMessage("Move has been sent!");
				//*/
			}
			else {
				Log.e(TAG, response.getFirst());
			}
		}
    }
	
	private void showMessage(String msg) {
		Toast t = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}
	
	protected void processInitialGameResult(int result) {
		// TODO
	}
	
	// Creates an AlertDialog through which one can respond to a draw offer
	private void answerDrawOffer() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Your opponent offers a draw")
			.setCancelable(false)
			.setPositiveButton("Accept", new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO
					dialog.dismiss();
				}
			})
			.setNegativeButton("Decline", new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	private class AnswerDrawOfferTask extends AsyncTask<String, Void, LinkedList<String> > {
		private final ProgressDialog dialog = new ProgressDialog(Game.this);
		
		@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		dialog.setMessage("Sending answer...");
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
				
			}	
		}
	}
	
	private class OfferDrawTask extends AsyncTask<String, Void, LinkedList<String> > {
    	private final ProgressDialog dialog = new ProgressDialog(Game.this);
    	
    	@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		dialog.setMessage("Sending draw offer...");
    		dialog.show();
    	}
		@Override
		protected LinkedList<String> doInBackground(String... params) {
			LinkedList<String> response = new LinkedList<String>();
			try {
				Log.d(TAG, "sending move: " + params[0]);
				Log.d(TAG, "sending result: " + params[1]);
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
				Log.e(TAG, "Could not connect to server");
			}
			else {
				Log.d(TAG, "response from server: " + response.getFirst());
				showMessage("Draw offer sent!");
			}
		}
    }
	
	// TODO
	private void offerDraw() {
		String[] params = {"", gameId + "," + DRAW_OFFERED};
		new OfferDrawTask().execute(params);
	}
}
