/**
 * @file
 */
package com.codemelon.chesswithhumans;

import static com.codemelon.chesswithhumans.Constants.*;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

/**
 * Copyright (c) 2011 Marshall Farrier
 * @author Marshall Farrier
 * @version 0.1 1/28/11
 * Cf. Burnette, p. 175
 * Steele-To, p. 232
 * Adapter class for ChessDbHelper
 */

public class ChessDataBaseAdapter {
	private static final String TAG = "ChessDBAdapter";
	private SQLiteDatabase db;
	private final Context context;
	private final ChessDBHelper dbHelper;
	
	public ChessDataBaseAdapter(Context ctx) {
		context = ctx;
		dbHelper = new ChessDBHelper(context);
	}
	
	public void close() {
		db.close();
	}
	
	public void open() throws SQLiteException {
		try {
			db = dbHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			Log.v(TAG, e.getMessage());
			db = dbHelper.getReadableDatabase();
		}
	}
	
	public Cursor getSelfCursor() {
		Log.d(TAG, "retrieving self data");
		return db.query(SELF_TABLE, null, null, null, null, null, null);
	}
	
	public int getSelfId() {
		Log.d(TAG, "retrieving self ID");
		Cursor c = db.query(SELF_TABLE, new String[] {PLAYER_ID_COL}, null, null, null, null, null);
		int result = -1;
		if (c.moveToFirst()) {
			result = c.getInt(c.getColumnIndex(PLAYER_ID_COL));
		}
		c.close();
		return result;
	}
	
	public String getSelfHandle() {
		Log.d(TAG, "retrieving self handle");
		Cursor c = db.query(SELF_TABLE, new String[] {PLAYER_NAME_COL}, null, null, null, null, null);
		String result = "";
		if (c.moveToFirst()) {
			result = c.getString(c.getColumnIndex(PLAYER_NAME_COL));
		}
		c.close();
		return result;
	}
	
	/**
	 * Returns playerId if player is in database, otherwise -1
	 * @param handle
	 * @return
	 */
	public int findPlayer(String handle) {
		String selection = PLAYER_NAME_COL + " = '" + handle + "'";
		int result = -1;
		Cursor c = db.query(PLAYER_TABLE, null, selection, null, null, null, null);
		if (c.moveToFirst()) {
			result = c.getInt(c.getColumnIndex(PLAYER_ID_COL));
		}
		c.close();
		return result;
	}
	
	public int findPlayer(int pid) {
		String selection = PLAYER_ID_COL + " = '" + pid + "'";
		int result = -1;
		Cursor c = db.query(PLAYER_TABLE, null, selection, null, null, null, null);
		if (c.moveToFirst()) {
			result = c.getInt(c.getColumnIndex(PLAYER_ID_COL));
		}
		c.close();
		return result;
	}
	
	public boolean addPlayer(int pid, String handle) {
		ContentValues values = new ContentValues(2);
		values.put(PLAYER_ID_COL, pid);
		values.put(PLAYER_NAME_COL, handle);
		long rowKey = db.insert(PLAYER_TABLE, null, values);
		Log.d(TAG, "playerId:" + pid + " rowKey:" + rowKey + " inserted into player table");
		if (rowKey == -1) return false;
		return true;
	}
	
	/**
	 * Enters registration data into both self table and player table
	 * @param selfId
	 * @param handle
	 * @param device
	 * @return
	 */
	public boolean enterRegistrationData(int selfId, String handle, String device) {
		final int COLUMNS = 3;
		long rowKey;
		// Delete any old registration data
		db.beginTransaction();
		try {
			db.delete(SELF_TABLE, null, null);
			ContentValues regValues = new ContentValues(COLUMNS);
			regValues.put(PLAYER_ID_COL, selfId);
			regValues.put(PLAYER_NAME_COL, handle);
			rowKey = db.insert(PLAYER_TABLE, null, regValues);
			Log.d(TAG, "playerId " + rowKey + " inserted into player table");
			regValues.put(DEVICE_ID_COL, device);			
			if (rowKey > 0) {
				rowKey = db.insert(SELF_TABLE, null, regValues);
				Log.d(TAG, "playerId " + rowKey + " inserted into self table");
				if (rowKey > 0) {
					db.setTransactionSuccessful();
					Log.d(TAG, "registration successful");
					return true;
				}
			}
			Log.d(TAG, "registration failed");
			return false;
		}
		finally {
			db.endTransaction();
		}
	}
	/*
	public boolean enterNewChallenge(int challengeId, int targetId, int status) {
		Date now = new Date();
		long initiated = now.getTime() / 1000;
		ContentValues values = new ContentValues();
		values.put(CHALLENGE_ID_COL, challengeId);
		values.put(CHALLENGE_TARGET_COL, targetId);
		values.put(CHALLENGE_DATE_COL, initiated);
		values.put(CHALLENGE_STATUS_COL, status);
		long rowId = db.insert(CHALLENGE_TABLE, null, values);
		Log.d(TAG, "challenge inserted with rowId " + rowId);
		if (rowId == -1) return false;
		return true;
	}
	*/
	/**
	 * Determines whether a challenge to targetId exists
	 * in the database
	 * @param sourceId
	 * @param targetId
	 * @return -1 if challenge not found, otherwise returns status
	 */
	/*
	public int verifyChallenge(int targetId) {
		Log.d(TAG, "verifying challenge for targetId " + targetId);
		String[] columns = {CHALLENGE_ID_COL, CHALLENGE_STATUS_COL};
		String selection = CHALLENGE_TARGET_COL + " = " + targetId;
		Cursor c = db.query(CHALLENGE_TABLE, columns, selection, null, null, null, null);
		int result = -1;
		if (c.moveToFirst()) {
			result = c.getInt(c.getColumnIndex(CHALLENGE_STATUS_COL));
		}
		c.close();
		Log.d(TAG, "verifyChallenge() return value: " + result);
		return result;
	}
	*/
	
	public void deleteRegistration() {
		db.delete(SELF_TABLE, null, null);
		db.delete(PLAYER_TABLE, null, null);
	}
	
	public Cursor getNewGamePlayers() {
		Log.d(TAG, "retrieving players from db");
		return db.query(NEW_GAME_OPPONENT_VIEW, null, null, null, null, null, PLAYER_NAME_COL);
	}
	
	// TODO delete this method. It is very deprecated!
	public int newGame(int white, int black) throws SQLiteConstraintException {
		/*
		int gameId, gamesStored;
		// Find a valid game number (first available for now)
		String[] cols = {GAME_ID_COL};
		Cursor gameIdCursor = db.query(GAME_TABLE, cols, null, null, null, null, GAME_ID_COL);
		gamesStored = gameIdCursor.getCount();
		gameId = 1;	// Default, kept only if no games stored
		if (gamesStored != 0) {
			int columnIndex = gameIdCursor.getColumnIndex(GAME_ID_COL);
			gameIdCursor.moveToFirst();
			while (gameId == gameIdCursor.getInt(columnIndex)) {
				++gameId;
				if (!gameIdCursor.isLast()) {
					gameIdCursor.moveToNext();
				}
			}
		}
		gameIdCursor.close();
		
		// Set the values to be inserted
		final int COLUMNS = 5;
		ContentValues gameValues = new ContentValues(COLUMNS);
		gameValues.put(GAME_ID_COL, gameId);
		gameValues.put(DATE_STARTED_COL, System.currentTimeMillis() / 1000);
		gameValues.put(WHITE_COL, white);
		gameValues.put(BLACK_COL, black);
		gameValues.put(RESULT_COL, UNFINISHED_GAME);
		
		Log.d(TAG, "creating new game with id " + gameId);
		db.insert(GAME_TABLE, null, gameValues);
		return gameId;
		*/
		return -1;
	}
	
	/**
	 * Returns -1 on failure
	 * @param gid
	 * @param white
	 * @param black
	 * @return
	 */
	public long newGame(int gid, int white, int black) {
		// Set the values to be inserted
		final int COLUMNS = 5;
		ContentValues gameValues = new ContentValues(COLUMNS);
		gameValues.put(GAME_ID_COL, gid);
		gameValues.put(DATE_STARTED_COL, System.currentTimeMillis() / 1000);
		gameValues.put(WHITE_COL, white);
		gameValues.put(BLACK_COL, black);
		gameValues.put(RESULT_COL, UNFINISHED_GAME);
		
		Log.d(TAG, "creating new game with id " + gid);
		return db.insert(GAME_TABLE, null, gameValues);
	}
	
	public Cursor getActiveGamePlayers() {
		Log.d(TAG, "retrieving active games from db");
		Cursor c = db.query(RESUME_GAME_OPPONENT_VIEW, null, null, null, null, null, PLAYER_NAME_COL);
		
		String[] columnNames = {PLAYER_ID_COL, PLAYER_NAME_COL, GAME_ID_COL, SELF_TO_MOVE_COL};
		int initialCapacity = c.getCount();
		MatrixCursor result = new MatrixCursor(columnNames, initialCapacity);
		MatrixCursor.RowBuilder builder;
		if (c.moveToFirst()) {
			builder = result.newRow();
			builder.add(c.getInt(c.getColumnIndex(PLAYER_ID_COL)));
			builder.add(c.getString(c.getColumnIndex(PLAYER_NAME_COL)));
			builder.add(c.getInt(c.getColumnIndex(GAME_ID_COL)));
			builder.add(selfToMove(c.getInt(c.getColumnIndex(GAME_ID_COL))));
			while (c.moveToNext()) {
				builder = result.newRow();
				builder.add(c.getInt(c.getColumnIndex(PLAYER_ID_COL)));
				builder.add(c.getString(c.getColumnIndex(PLAYER_NAME_COL)));
				builder.add(c.getInt(c.getColumnIndex(GAME_ID_COL)));
				builder.add(selfToMove(c.getInt(c.getColumnIndex(GAME_ID_COL))));
			}
		}
		c.close();
		return result;
	}
	
	/**
	 * Returns 1 if self has next move, 0 if opponent has next move
	 * -1 if gameId not found
	 * @param gameId
	 * @return
	 */
	private int selfToMove(int gameId) {
		int selfId = getSelfId();
		// Get data from game table
		String[] columns1 = {WHITE_COL, BLACK_COL};
		String selection1 = GAME_ID_COL + " = " + gameId;
		Cursor gameCursor = db.query(GAME_TABLE, columns1, selection1, null, null, null, null);
		if (!gameCursor.moveToFirst()) {
			gameCursor.close();
			return -1;
		}
		int offset = 1;	// self assumed to be white
		if (gameCursor.getInt(gameCursor.getColumnIndex(BLACK_COL)) == selfId) {
			offset = 0;
		}
		gameCursor.close();
		String[] columns2 = {MOVE_NUM_COL};
		String selection2 = GAME_ID_COL + " = " + gameId;
		String orderBy2 = MOVE_NUM_COL + " DESC";
		String limit2 = "1";
		Cursor moveCursor = db.query(MOVE_TABLE, columns2, selection2, null, null, null, orderBy2, limit2);
		if (!moveCursor.moveToFirst()) {	// white's move
			moveCursor.close();
			return offset;
		}
		int result = (moveCursor.getInt(moveCursor.getColumnIndex(MOVE_NUM_COL)) + 1 + offset) % 2;
		moveCursor.close();
		return result;
	}
	
	public int setGameResult(int gameId, int result) {
		ContentValues gameResult = new ContentValues(1);
		gameResult.put(RESULT_COL, result);
		String whereClause = GAME_ID_COL + " = " + gameId;
		return db.update(GAME_TABLE, gameResult, whereClause, null);
		// For verification that update works properly:
		/*
		int tmp = db.update(GAME_TABLE, gameResult, whereClause, null);
		Cursor checkResultCursor = db.query(GAME_TABLE, null, whereClause, null, null, null, null);
		checkResultCursor.moveToFirst();
		int whiteCol = checkResultCursor.getColumnIndex(WHITE_COL);
		int blackCol = checkResultCursor.getColumnIndex(BLACK_COL);
		Log.d(TAG, "game finished betwee player " + checkResultCursor.getInt(whiteCol) + " and " + checkResultCursor.getInt(blackCol));
		checkResultCursor.close();
		return tmp;
		*/
	}
	
	public long move(int gameId, int moveNum, int from, int to, int piece) throws SQLiteConstraintException {
		final int COLUMNS = 5;
		ContentValues moveValues = new ContentValues(COLUMNS);
		moveValues.put(GAME_ID_COL, gameId);
		moveValues.put(MOVE_NUM_COL, moveNum);
		moveValues.put(FROM_SQUARE_COL, from);
		moveValues.put(TO_SQUARE_COL, to);
		moveValues.put(PIECE_COL, piece);
		
		Log.d(TAG, "adding move " + moveNum + " to database");
		return db.insert(MOVE_TABLE, null, moveValues);
	}
	
	/**
	 * Returns true if self is white, false
	 * if self is black.
	 * Throws IllegalArgumentException if self is neither
	 * or if gameId is not present in db.
	 * @param gameId
	 * @return
	 * @throws IllegalArgumentException
	 */
	public boolean white(int selfId, int gameId) throws IllegalArgumentException {
		String selection = GAME_ID_COL + " = " + gameId;
		Cursor gameCursor = db.query(GAME_TABLE, null, selection, null, null, null, null);
		try {
			// Throw an exception if row not found
			if (gameCursor.getCount() == 0) throw new IllegalArgumentException(TAG + ": Game does not exist");
			int whiteCol = gameCursor.getColumnIndex(WHITE_COL);
			int blackCol = gameCursor.getColumnIndex(BLACK_COL);
			gameCursor.moveToFirst();
			if (gameCursor.getInt(whiteCol) == selfId) return true;
			if (gameCursor.getInt(blackCol) == selfId) return false;
			Log.d(TAG, "Self neither white nor black");
			throw new IllegalArgumentException(TAG + ": Self neither white nor black");
		} finally {
			gameCursor.close();
		}
	}
	
	public ArrayList<Move> moveList(int gameId) {
		String selection = GAME_ID_COL + " = " + gameId;
		Cursor moveListCursor = db.query(MOVE_TABLE, null, selection, null, null, null, MOVE_NUM_COL);
		try {
			int movesMade = moveListCursor.getCount();
			ArrayList<Move> result = new ArrayList<Move>(movesMade);
			int fromCol = moveListCursor.getColumnIndex(FROM_SQUARE_COL);
			int toCol = moveListCursor.getColumnIndex(TO_SQUARE_COL);
			int pieceCol = moveListCursor.getColumnIndex(PIECE_COL);
			moveListCursor.moveToFirst();
			while (!moveListCursor.isAfterLast()) {
				result.add(new Move(moveListCursor.getInt(pieceCol), 
						moveListCursor.getInt(fromCol), moveListCursor.getInt(toCol)));
				moveListCursor.moveToNext();
			}
			return result;
		} finally {
			moveListCursor.close();
		}
	}
	
	/**
	 * Returns a usable input for insert_moves.php
	 * Outputs only the first move for those open games where
	 * only 1 move has been made.
	 * @return
	 */
	public String activeGameMoveInitializer() {
		StringBuilder lostMoves = new StringBuilder();
		
		LinkedList<Integer> games = getOpenGames();
		boolean first = true;
		for (int game : games) {
			if (getLastMove(game) == 0) {
				if (!first) lostMoves.append(";");
				else first = false;
				lostMoves.append(getLastMoveString(game));
			}
		}
		return lostMoves.toString();
	}
	
	public void processMovesFromServer(LinkedList<String> serverMoves) {
		if (serverMoves.isEmpty()) return;
		String[] move;
		final int gameIdCol = 0;
		final int moveIdCol = 1;
		final int fromCol = 2;
		final int toCol = 3;
		final int pieceCol = 4;
		ListIterator<String> it = serverMoves.listIterator();
		while (it.hasNext()) {
			move = it.next().split(",");
			if (getLastMove(Integer.parseInt(move[gameIdCol])) < Integer.parseInt(move[moveIdCol])) {
				move(Integer.parseInt(move[gameIdCol]), Integer.parseInt(move[moveIdCol]), 
						Integer.parseInt(move[fromCol]), Integer.parseInt(move[toCol]), 
						Integer.parseInt(move[pieceCol]));
				Log.d(TAG, "Entering move " + move[moveIdCol] + " for game " + move[gameIdCol]);
			}
		}
	}
	
	/**
	 * Simply enters the results returned from server--so the server must provide
	 * valid results to enter
	 * @param serverResults
	 */
	public void processResultsFromServer(LinkedList<String> serverResults) {
		if (serverResults.isEmpty()) return;
		final int gameIdCol = 0;
		final int resultCol = 1;
		String[] gameRes;
		ListIterator<String> it = serverResults.listIterator();
		while (it.hasNext()) {
			gameRes = it.next().split(",");
			if (gameRes[resultCol].matches("[0-9]+")) {
				setGameResult(Integer.parseInt(gameRes[gameIdCol]), 
						Integer.parseInt(gameRes[resultCol]));
				Log.d(TAG, "updating result for game " + gameRes[gameIdCol] +
						" to " + gameRes[resultCol]);
			}
		}
	}
	
	// TODO	
	/**
	 * 3/27: This needs to be overhauled.
	 * The return string will be usable input for insert_moves.php
	 * if there are any local moves that are newer than the database
	 * moves. There really should be at most 1 such move (when your last
	 * move couldn't be entered due to failed connection)
	 * @param response
	 * @return
	 */
	public String processNewMovesOld(LinkedList<String> response) {
		StringBuilder lostMoves = new StringBuilder();
		final int gameIndex = 0;
		final int moveIndex = 1;
		final int fromIndex = 2;
		final int toIndex = 3;
		final int pieceIndex = 4;
		final int resultIndex = 5;
		String[] move;
		int gameId, remoteMoveNum, localMoveNum, resultCode;
		boolean first = true;
		LinkedList<Integer> localActiveGames = getOpenGames();
		String moveToAdd = "";
		int localResult;
		
		for (String line : response) {
			move = line.split(",");
			gameId = Integer.parseInt(move[gameIndex]);
			resultCode = Integer.parseInt(move[resultIndex]);
			localActiveGames.remove(Integer.valueOf(gameId));
			remoteMoveNum = Integer.parseInt(move[moveIndex]);
			localMoveNum = getLastMove(gameId);
			if (remoteMoveNum > localMoveNum) {
				move(gameId, remoteMoveNum, Integer.parseInt(move[fromIndex]), 
						Integer.parseInt(move[toIndex]), Integer.parseInt(move[pieceIndex]));
			}
			else if (remoteMoveNum < localMoveNum) {
				if (!first) lostMoves.append(";");
				else first = false;
				lostMoves.append(getLastMoveString(gameId));
			}
			localResult = gameResult(gameId);
			if (resultCode != UNFINISHED_GAME) {				
				if (localResult > UNFINISHED_GAME && localResult != DRAW_OFFER_DECLINED) {	// Self was source of original message
					// Don't send anything back to server, just finalize game locally
					switch (resultCode) {
					case DRAW_OFFERED:	// Do nothing
						break;
					case DRAW_CALLED:
						setGameResult(gameId, DRAW);
						break;
					case WHITE_RESIGNED:
						setGameResult(gameId, BLACK_WINS);
						break;
					case BLACK_RESIGNED:
						setGameResult(gameId, WHITE_WINS);
						break;
					case DRAW_OFFER_ACCEPTED:
						// This should actually never happen
						setGameResult(gameId, DRAW);
						break;
					case DRAW_OFFER_DECLINED:
						setGameResult(gameId, UNFINISHED_GAME);						
						// Clear entry on server
						if (!first) lostMoves.append(";");
						else first = false;
						lostMoves.append(gameId + "," + UNFINISHED_GAME + "," + RESULT_SIGNAL);						
						break;
					case WHITE_WINS_BY_CHECKMATE:
						setGameResult(gameId, WHITE_WINS);
						break;
					case BLACK_WINS_BY_CHECKMATE:
						setGameResult(gameId, BLACK_WINS);
						break;
					case DRAW_BY_STALEMATE:
						setGameResult(gameId, DRAW);
						break;
					case DRAW_BY_NO_MORE_CHECKMATE_FOR_DB:
						setGameResult(gameId, DRAW);
						break;
					case DRAW_BY_REPETITION_FOR_DB:
						setGameResult(gameId, DRAW);
						break;
					case DRAW_BY_FIFTY_FOR_DB:
						setGameResult(gameId, DRAW);
						break;
					case WHITE_WINS:
						setGameResult(gameId, WHITE_WINS);
						break;
					case BLACK_WINS:
						setGameResult(gameId, BLACK_WINS);
						break;
					case DRAW:
						setGameResult(gameId, DRAW_OFFER_ACCEPTED);
						break;
					}
				}
				else {	// Message originated with other player
					switch (resultCode) {
					case DRAW_OFFERED:
						if (localResult != DRAW_OFFER_DECLINED) {
							setGameResult(gameId, DRAW_OFFERED_BY_OPPONENT);
						}
						else {
							if (!first) lostMoves.append(";");
							else first = false;
							lostMoves.append(gameId + "," + DRAW_OFFER_DECLINED + "," + RESULT_SIGNAL);
						}
						break;
					case DRAW_CALLED:
						setGameResult(gameId, resultCode);
						if (!first) lostMoves.append(";");
						else first = false;					
						lostMoves.append(gameId + "," + DRAW + "," + RESULT_SIGNAL);
						break;
					case WHITE_RESIGNED:
						setGameResult(gameId, resultCode);
						if (!first) lostMoves.append(";");
						else first = false;
						lostMoves.append(gameId + "," + BLACK_WINS + "," + RESULT_SIGNAL);
						break;
					case BLACK_RESIGNED:
						setGameResult(gameId, resultCode);
						if (!first) lostMoves.append(";");
						else first = false;
						lostMoves.append(gameId + "," + WHITE_WINS + "," + RESULT_SIGNAL);
						break;
					case WHITE_WINS_BY_CHECKMATE:
						setGameResult(gameId, resultCode);
						break;
					case BLACK_WINS_BY_CHECKMATE:
						setGameResult(gameId, resultCode);
						break;
					case DRAW_BY_STALEMATE:
						setGameResult(gameId, resultCode);
						break;
					case DRAW_BY_NO_MORE_CHECKMATE_FOR_DB:
						setGameResult(gameId, resultCode);
						break;
					case DRAW_BY_REPETITION_FOR_DB:
						setGameResult(gameId, resultCode);
						break;
					case DRAW_BY_FIFTY_FOR_DB:
						setGameResult(gameId, resultCode);
						break;
					}
				}
			}
			// I declined and remote database is now showing UNFINISHED_GAME
			else if (localResult == DRAW_OFFER_DECLINED) {
				setGameResult(gameId, UNFINISHED_GAME);
			}
			// if removeMoveNum == localMoveNum there is nothing to do
		}
		// Now all games in the response string have been removed
		if (!localActiveGames.isEmpty()) {
			for (int game : localActiveGames) {
				moveToAdd = getLastMoveString(game);
				if (moveToAdd.length() > 0) {
					if (!first) lostMoves.append(";");
					else first = false;
					lostMoves.append(moveToAdd);
				}
			}
		}		
		return lostMoves.toString();
	}
	
	/**
	 * Returns -1 if no move has been made in the given game;
	 * otherwise the number of the last move.
	 * @param gameId
	 * @return
	 */
	private int getLastMove(int gameId) {
		int result = -1;
		String[] columns = {MOVE_NUM_COL};
		String selection = GAME_ID_COL + " = " + gameId;
		String orderBy = MOVE_NUM_COL + " DESC";
		String limit = "1";		
		Cursor c = db.query(MOVE_TABLE, columns, selection, null, null, null, orderBy, limit);
		if (c.getCount() > 0) {
			c.moveToFirst();
			result = c.getInt(c.getColumnIndex(MOVE_NUM_COL));
		}
		c.close();
		return result;
	}
	
	private String getLastMoveString(int gameId) {
		String result = "";
		String selection = GAME_ID_COL + " = " + gameId;
		String orderBy = MOVE_NUM_COL + " DESC";
		String limit = "1";		
		Cursor c = db.query(MOVE_TABLE, null, selection, null, null, null, orderBy, limit);
		if (c.getCount() > 0) {
			c.moveToFirst();
			result = c.getInt(c.getColumnIndex(GAME_ID_COL)) + "," +
				c.getInt(c.getColumnIndex(MOVE_NUM_COL)) + "," +
				c.getInt(c.getColumnIndex(FROM_SQUARE_COL)) + "," +
				c.getInt(c.getColumnIndex(TO_SQUARE_COL)) + "," +
				c.getInt(c.getColumnIndex(PIECE_COL));
		}
		c.close();
		return result;
	}
	
	public String getAllLastMovesString() {
		StringBuilder builder = new StringBuilder();
		LinkedList<Integer> games = getUnconfirmedGames();
		if (!games.isEmpty()) {
			ListIterator<Integer> it = games.listIterator();
			builder.append(getLastMoveString(it.next()));
			while (it.hasNext()) {
				builder.append(";" + getLastMoveString(it.next()));
			}
		}
		return builder.toString();
	}
	
	public String getOpenGamesString() {
		StringBuilder result = new StringBuilder();
		LinkedList<Integer> games = getOpenGames();
		boolean first = true;
		for (int game : games) {
			if (!first) result.append(",");
			else first = false;
			result.append(String.valueOf(game));
		}
		return result.toString();
	}
	
	private LinkedList<Integer> getOpenGames() {
		LinkedList<Integer> result = new LinkedList<Integer>();
		String[] columns = {GAME_ID_COL};
		Cursor c = db.query(UNFINISHED_GAME_VIEW, columns, null, null, null, null, null);		
		if (c.getCount() > 0) {
			c.moveToFirst();
			do {
				result.add(c.getInt(c.getColumnIndex(GAME_ID_COL)));
			} while (c.moveToNext());
		}
		c.close();
		return result;
	}
	
	public String getUnconfirmedGamesString() {
		StringBuilder result = new StringBuilder();
		LinkedList<Integer> games = getUnconfirmedGames();
		boolean first = true;
		for (int game : games) {
			if (!first) result.append(",");
			else first = false;
			result.append(String.valueOf(game));
		}
		return result.toString();
	}
	
	private LinkedList<Integer> getUnconfirmedGames() {
		LinkedList<Integer> result = new LinkedList<Integer>();
		String[] columns = {GAME_ID_COL};
		Cursor c = db.query(UNCONFIRMED_GAME_VIEW, columns, null, null, null, null, null);		
		if (c.getCount() > 0) {
			c.moveToFirst();
			do {
				result.add(c.getInt(c.getColumnIndex(GAME_ID_COL)));
			} while (c.moveToNext());
		}
		c.close();
		return result;
	}
	
	public String getUnconfirmedGameResultsString() {
		StringBuilder builder = new StringBuilder();
		String[] columns = {GAME_ID_COL, RESULT_COL};
		Cursor c = db.query(UNCONFIRMED_GAME_VIEW, columns, null, null, null, null, null);	
		if (c.getCount() > 0) {
			c.moveToFirst();
			builder.append(c.getInt(c.getColumnIndex(GAME_ID_COL)) + "," +
					c.getInt(c.getColumnIndex(RESULT_COL)));
			while (c.moveToNext()) {
				builder.append(";" + c.getInt(c.getColumnIndex(GAME_ID_COL)) + "," +
					c.getInt(c.getColumnIndex(RESULT_COL)));
			}
		}
		c.close();
		return builder.toString();
	}
	
	/**
	 * Returns result for given game or -1 if game not found
	 * @param gameId
	 * @return
	 */
	public int gameResult(int gameId) {
		int result = -1;
		
		String[] columns = {RESULT_COL};
		String selection = GAME_ID_COL + " = " + gameId;
		Cursor c = db.query(GAME_TABLE, columns, selection, null, null, null, null);
		if (c.moveToFirst()) {
			result = c.getInt(c.getColumnIndex(RESULT_COL));
		}
		c.close();
		return result;
	}
}
