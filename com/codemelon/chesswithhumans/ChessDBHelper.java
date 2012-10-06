/**
 * @file
 * Copyright (c) 2011 Marshall Farrier 
 */
package com.codemelon.chesswithhumans;

import static com.codemelon.chesswithhumans.Constants.*;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * @author Marshall Farrier
 * @version 0.1 2/4/11
 * Cf. Burnette, p. 175
 * Steele-To, p. 232
 */
public class ChessDBHelper extends SQLiteOpenHelper {
	private static final String TAG = "ChessDBHelper";
	private static final int DATABASE_VERSION = 2;
	
	private static final String CREATE_TABLE_SELF = "CREATE TABLE " + SELF_TABLE + " (" +
		PLAYER_ID_COL + " INTEGER PRIMARY KEY, " +
		PLAYER_NAME_COL + " TEXT NOT NULL, " +
		DEVICE_ID_COL + " TEXT NOT NULL);";
	private static final String CREATE_TABLE_PLAYER = "CREATE TABLE " + PLAYER_TABLE + " (" + 
		PLAYER_ID_COL + " INTEGER PRIMARY KEY, " +
		PLAYER_NAME_COL + " TEXT NOT NULL);";
	// Insert player "any" into player_table
	private static final String INSERT_PLAYER_ANY = "INSERT INTO " + PLAYER_TABLE +
		" VALUES (" + PLAYER_ANY_ID + ", 'any');";
	private static final String CREATE_TABLE_GAME = "CREATE TABLE " + GAME_TABLE + " (" +
		GAME_ID_COL + " INTEGER PRIMARY KEY, " +
		DATE_STARTED_COL + " INTEGER NOT NULL, " + 
		WHITE_COL + " INTEGER NOT NULL, " + 
		BLACK_COL + " INTEGER NOT NULL, " + 
		RESULT_COL + " INTEGER NOT NULL, " +
		"FOREIGN KEY(" + WHITE_COL + ") REFERENCES " + PLAYER_TABLE + "(" + PLAYER_ID_COL + "), " +
		"FOREIGN KEY(" + BLACK_COL + ") REFERENCES " + PLAYER_TABLE + "(" + PLAYER_ID_COL + ")" +
		");";
	private static final String CREATE_TABLE_MOVE = "CREATE TABLE " + MOVE_TABLE + " (" +
		GAME_ID_COL + " INTEGER NOT NULL, " +
		MOVE_NUM_COL + " INTEGER NOT NULL, " +
		FROM_SQUARE_COL + " INTEGER NOT NULL, " +
		TO_SQUARE_COL + " INTEGER NOT NULL, " +
		PIECE_COL + " INTEGER NOT NULL, " +
		"PRIMARY KEY(" + GAME_ID_COL + ", " + MOVE_NUM_COL + ") ON CONFLICT ABORT, " +
		"FOREIGN KEY(" + GAME_ID_COL + ") REFERENCES " + GAME_TABLE + "(" + GAME_ID_COL + ")" +
		");";
	private static final String CREATE_UNCONFIRMED_GAME_VIEW = 
		"CREATE VIEW IF NOT EXISTS " + UNCONFIRMED_GAME_VIEW + " AS " +
		"SELECT * FROM " + GAME_TABLE +
		" WHERE " + RESULT_COL + " < " + WHITE_WINS + ";";
	private static final String CREATE_UNFINISHED_GAME_VIEW = 
		"CREATE VIEW IF NOT EXISTS " + UNFINISHED_GAME_VIEW + " AS " +
		"SELECT * FROM " + GAME_TABLE +
		" WHERE " + RESULT_COL + " IN ( " + 
		UNFINISHED_GAME + ", " + 
		DRAW_OFFERED + ", " +
		DRAW_DECLINED_CONFIRMED + ", " +
		DRAW_OFFER_DECLINED + ", " +
		DRAW_OFFERED_BY_OPPONENT + ");";
	private static final String CREATE_VIEW_ACTIVE_GAME_OPP_ID_WITH_SELF = 
		"CREATE VIEW IF NOT EXISTS " + ACTIVE_GAME_OPP_ID_WITH_SELF_VIEW + " AS " +
		"SELECT " + WHITE_COL + " " + PLAYER_ID_COL + ", " +
		GAME_ID_COL +
		" FROM " + UNFINISHED_GAME_VIEW + 
		" UNION " +
		"SELECT " + BLACK_COL + " " + PLAYER_ID_COL + ", " +
		GAME_ID_COL +
		" FROM " + UNFINISHED_GAME_VIEW + ";";
	private static final String CREATE_VIEW_ACTIVE_OPP_ID = 
		"CREATE VIEW IF NOT EXISTS " + ACTIVE_OPPONENT_ID_VIEW + " AS " +
		"SELECT " + PLAYER_ID_COL + 
		" FROM " + ACTIVE_GAME_OPP_ID_WITH_SELF_VIEW + 
		" EXCEPT " +
		"SELECT " + PLAYER_ID_COL +
		" FROM " + SELF_TABLE + ";";
	private static final String CREATE_VIEW_ACTIVE_GAME_OPP_ID = 
		"CREATE VIEW IF NOT EXISTS " + ACTIVE_GAME_OPPONENT_ID_VIEW + " AS " +
		"SELECT a." + PLAYER_ID_COL + " " + PLAYER_ID_COL + "," +
		" a." + GAME_ID_COL + " " + GAME_ID_COL +
		" FROM " + ACTIVE_GAME_OPP_ID_WITH_SELF_VIEW + " a," +
		" " + ACTIVE_OPPONENT_ID_VIEW + " b " +
		"WHERE a." + PLAYER_ID_COL + " = b." + PLAYER_ID_COL + ";";
	private static final String CREATE_VIEW_OPPONENTS =
		"CREATE VIEW IF NOT EXISTS " + OPPONENT_ID_VIEW + " AS " +
		"SELECT " + PLAYER_ID_COL + 
		" FROM " + PLAYER_TABLE + 
		" WHERE " + PLAYER_ID_COL + " != " + PLAYER_ANY_ID +
		" EXCEPT " +
		"SELECT " + PLAYER_ID_COL +
		" FROM " + SELF_TABLE + ";";
	private static final String CREATE_VIEW_NEW_GAME_OPP_ID = 
		"CREATE VIEW IF NOT EXISTS " + NEW_GAME_OPPONENT_ID_VIEW + " AS " +
		"SELECT " + PLAYER_ID_COL + 
		" FROM " + OPPONENT_ID_VIEW + 
		" EXCEPT " +
		"SELECT " + PLAYER_ID_COL +
		" FROM " + ACTIVE_GAME_OPPONENT_ID_VIEW + ";";
	private static final String CREATE_VIEW_NEW_GAME_OPPONENT = 
		"CREATE VIEW IF NOT EXISTS " + NEW_GAME_OPPONENT_VIEW + " AS " +
		"SELECT p." + PLAYER_ID_COL + " " + PLAYER_ID_COL + ", " +
		"p." + PLAYER_NAME_COL + " " + PLAYER_NAME_COL +
		" FROM " + PLAYER_TABLE + " p, " +
		NEW_GAME_OPPONENT_ID_VIEW + " n" +
		" WHERE p." + PLAYER_ID_COL + " = n." + PLAYER_ID_COL + ";";
	private static final String CREATE_VIEW_RESUME_GAME_OPPONENT = 
		"CREATE VIEW IF NOT EXISTS " + RESUME_GAME_OPPONENT_VIEW + " AS " +
		"SELECT p." + PLAYER_ID_COL + " " + PLAYER_ID_COL + ", " +
		"p." + PLAYER_NAME_COL + " " + PLAYER_NAME_COL + ", " +
		"a." + GAME_ID_COL + " " + GAME_ID_COL +
		" FROM " + PLAYER_TABLE + " p, " +
		ACTIVE_GAME_OPPONENT_ID_VIEW + " a" +
		" WHERE p." + PLAYER_ID_COL + " = a." + PLAYER_ID_COL + ";";

	public ChessDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "Creating tables");
		try {
			db.beginTransaction();
			try {
				db.execSQL(CREATE_TABLE_SELF);
				Log.d(TAG, "self table created");
				db.execSQL(CREATE_TABLE_PLAYER);
				Log.d(TAG, "player table created");
				db.execSQL(INSERT_PLAYER_ANY);
				Log.d(TAG, "player 'any' inserted");
				// db.execSQL(CREATE_TABLE_CHALLENGE);
				db.execSQL(CREATE_TABLE_GAME);
				Log.d(TAG, "game table created");
				db.execSQL(CREATE_TABLE_MOVE);
				Log.d(TAG, "move table created");
				db.execSQL(CREATE_UNCONFIRMED_GAME_VIEW);
				Log.d(TAG, "view " + UNCONFIRMED_GAME_VIEW + " created");
				db.execSQL(CREATE_UNFINISHED_GAME_VIEW);
				Log.d(TAG, "view " + UNFINISHED_GAME_VIEW + " created");
				db.execSQL(CREATE_VIEW_ACTIVE_GAME_OPP_ID_WITH_SELF);
				db.execSQL(CREATE_VIEW_ACTIVE_OPP_ID);
				db.execSQL(CREATE_VIEW_ACTIVE_GAME_OPP_ID);
				Log.d(TAG, "view " + ACTIVE_GAME_OPPONENT_ID_VIEW + " created");
				db.execSQL(CREATE_VIEW_OPPONENTS);
				db.execSQL(CREATE_VIEW_NEW_GAME_OPP_ID);
				Log.d(TAG, "view " + NEW_GAME_OPPONENT_ID_VIEW + " created");				
				db.execSQL(CREATE_VIEW_NEW_GAME_OPPONENT);
				Log.d(TAG, "view " + NEW_GAME_OPPONENT_VIEW + " created");
				db.execSQL(CREATE_VIEW_RESUME_GAME_OPPONENT);
				Log.d(TAG, "view " + RESUME_GAME_OPPONENT_VIEW + " created");
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
		} catch (SQLiteException e) {
			Log.v("Error creating tables", e.getMessage());
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
		db.execSQL("DROP VIEW IF EXISTS " + RESUME_GAME_OPPONENT_VIEW);
		db.execSQL("DROP VIEW IF EXISTS " + NEW_GAME_OPPONENT_VIEW);
		db.execSQL("DROP VIEW IF EXISTS " + NEW_GAME_OPPONENT_ID_VIEW);
		db.execSQL("DROP VIEW IF EXISTS " + OPPONENT_ID_VIEW);
		db.execSQL("DROP VIEW IF EXISTS " + ACTIVE_GAME_OPPONENT_ID_VIEW);
		db.execSQL("DROP VIEW IF EXISTS " + ACTIVE_OPPONENT_ID_VIEW);
		db.execSQL("DROP VIEW IF EXISTS " + ACTIVE_GAME_OPP_ID_WITH_SELF_VIEW);
		db.execSQL("DROP VIEW IF EXISTS " + UNFINISHED_GAME_VIEW);
		db.execSQL("DROP VIEW IF EXISTS " + UNCONFIRMED_GAME_VIEW);
		
		db.execSQL("DROP TABLE IF EXISTS " + MOVE_TABLE + ";");
		db.execSQL("DROP TABLE IF EXISTS " + GAME_TABLE + ";");
		db.execSQL("DROP TABLE IF EXISTS " + PLAYER_TABLE + ";");
		db.execSQL("DROP TABLE IF EXISTS " + SELF_TABLE + ";");
		onCreate(db);
		//*/
		/*
		// This can be deleted as soon as updated version has run on both emulators
		db.execSQL("DROP TABLE IF EXISTS " + CHALLENGE_TABLE + ";");
		db.execSQL(CREATE_UNCONFIRMED_GAME_VIEW);
		Log.d(TAG, "view " + UNCONFIRMED_GAME_VIEW + " created");
		db.execSQL(CREATE_UNFINISHED_GAME_VIEW);
		Log.d(TAG, "view " + UNFINISHED_GAME_VIEW + " created");
		db.execSQL(CREATE_VIEW_ACTIVE_GAME_OPP_ID_WITH_SELF);
		db.execSQL(CREATE_VIEW_ACTIVE_OPP_ID);
		db.execSQL(CREATE_VIEW_ACTIVE_GAME_OPP_ID);
		Log.d(TAG, "view " + ACTIVE_GAME_OPPONENT_ID_VIEW + " created");
		db.execSQL(CREATE_VIEW_OPPONENTS);
		db.execSQL(CREATE_VIEW_NEW_GAME_OPP_ID);
		Log.d(TAG, "view " + NEW_GAME_OPPONENT_ID_VIEW + " created");				
		db.execSQL(CREATE_VIEW_NEW_GAME_OPPONENT);
		Log.d(TAG, "view " + NEW_GAME_OPPONENT_VIEW + " created");
		db.execSQL(CREATE_VIEW_RESUME_GAME_OPPONENT);
		Log.d(TAG, "view " + RESUME_GAME_OPPONENT_VIEW + " created");
		//*/
	}
}
