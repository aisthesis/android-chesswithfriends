/**
 * @file
 * Copyright (c) 2011 Marshall Farrier
 */
package com.codemelon.chesswithhumans;

import static com.codemelon.chesswithhumans.Constants.*;

import android.app.Activity;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;

/**
 * 
 * @author Marshall Farrier
 * @version 0.1 2/10/11
 *
 */
public class NewGame extends Game {
	private static final String TAG = "NewGame";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        
        super.setMoveNumber(0);
        super.setWhite(randomBoolean());
        
		db.open();
		try {
			if (white()) {
				setGameId(db.newGame(selfId(), opponentId()));
			}
			else {
				setGameId(db.newGame(opponentId(), selfId()));
			}
		} catch (SQLiteConstraintException e) {
			// TODO appropriate error window for user (then return to opening screen?)
			Log.v(TAG, "error inserting new game row: " + e.getMessage());
		} catch (SQLiteException e) {
			Log.v(TAG, "error inserting new game row: " + e.getMessage());
		} finally {		
			db.close();
		}
        
        boardView = new BoardView(this, BoardView.VIEW_GAME);      
        
        setContentView(boardView);
        boardView.requestFocus();
    }
	
	private static boolean randomBoolean() {
		double x = Math.random();
		if (x >= 0.5) return false;
		return true;
	}
}
