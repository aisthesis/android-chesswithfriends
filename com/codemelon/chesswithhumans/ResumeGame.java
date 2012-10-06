/**
 * @file
 * Copyright (c) 2011 Marshall Farrier
 */
package com.codemelon.chesswithhumans;

import static com.codemelon.chesswithhumans.Constants.*;

import java.util.ArrayList;

import android.os.Bundle;
import android.util.Log;

/**
 * @author Marshall Farrier
 * @version 0.1 2/10/11
 *
 */
public class ResumeGame extends Game {
	private static final String TAG = "ResumeGame";
	private int initialGameResult;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
	
		setGameId(getIntent().getIntExtra(GAME_ID, -1));
		Log.d(TAG, "gameId reported as " + getIntent().getIntExtra(GAME_ID, -1));
		initialGameResult = -1;
		
		// Retrieve additional parameters from db
		db.open();
		try {
			setWhite(db.white(selfId(), gameId()));
			Log.d(TAG, "isWhite set to " + white());
			ArrayList<Move> moveList = db.moveList(gameId());
			setMoveNumber(moveList.size());
			boardView = new BoardView(this, BoardView.PLAY_GAME, moveList);
			setContentView(boardView);
	        boardView.requestFocus();
	        initialGameResult = db.gameResult(gameId());
		} finally {
			db.close();
		}
		
		if (initialGameResult > UNFINISHED_GAME) {
			processInitialGameResult(initialGameResult);
		}
	}
}
