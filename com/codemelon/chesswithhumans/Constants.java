/**
 * @file
 */
package com.codemelon.chesswithhumans;

import android.app.Activity;
import android.net.Uri;

/**
 * @author Marshall Farrier
 * @version 0.1 2/3/11
 * Cf. Burnette, p. 177; Steele-To, p. 235
 */
public interface Constants {
	public static final String DATABASE_NAME = "chess.db";
	
	// Tables
	public static final String MOVE_TABLE = "move";
	public static final String GAME_TABLE = "game";
	public static final String PLAYER_TABLE = "player";
	public static final String SELF_TABLE = "self_data";
	// public static final String CHALLENGE_TABLE = "challenge";
	
	// SQLite views
	public static final String UNCONFIRMED_GAME_VIEW = "unconfirmed_game_view";
	public static final String UNFINISHED_GAME_VIEW = "unfinished_game_view";
	public static final String ACTIVE_GAME_OPP_ID_WITH_SELF_VIEW = "active_game_opp_self_view";
	// List of id's for opponents that have an unfinished game in the db
	public static final String ACTIVE_OPPONENT_ID_VIEW = "active_opp_id_view";
	// List of id's and games for opponents that have an unfinished game in the db
	public static final String ACTIVE_GAME_OPPONENT_ID_VIEW = "active_game_opp_id_view";
	// List of all possible opponents (excludes only self)
	public static final String OPPONENT_ID_VIEW = "opponent_id_view";
	// List of id's for players eligible for new game
	public static final String NEW_GAME_OPPONENT_ID_VIEW = "new_game_opp_id_view";
	// Players for a new game (opponent must not have unfinished game in db)
	public static final String NEW_GAME_OPPONENT_VIEW = "new_game_opponent_view";
	// Players with whom a game is still active
	public static final String RESUME_GAME_OPPONENT_VIEW = "resume_game_opponent_view";
	
	// Columns
	// For game table
	public static final String GAME_ID_COL = "game_id";
	public static final String DATE_STARTED_COL = "date_started";
	public static final String WHITE_COL = "white";
	public static final String BLACK_COL = "black";
	public static final String RESULT_COL = "result";
	
	// For challenge data
	public static final String CHALLENGE_ID_COL = "challenge_id";
	public static final String CHALLENGE_TARGET_COL = "target_id";
	public static final String CHALLENGE_DATE_COL = "initiated";
	public static final String CHALLENGE_STATUS_COL = "status";
	public static final String CHALLENGE_SOURCE_ID_COL = "source_id";
	public static final String CHALLENGE_SOURCE_HANDLE_COL = "source_handle";
	
	// For player table
	public static final String PLAYER_ID_COL = "player_id";
	// Handles are unique
	public static final String PLAYER_NAME_COL = "name";
	// For self table
	public static final String DEVICE_ID_COL = "device_id";
	
	// For move table
	// GAME_KEY_COL will be the first column in this table
	// The primary key will be GAME_KEY_COL together with MOVE_NUM_COL
	public static final String MOVE_NUM_COL = "move_number";
	public static final String FROM_SQUARE_COL = "from_square";
	public static final String TO_SQUARE_COL = "to_square";
	public static final String PIECE_COL = "piece";
	
	// For cursor used to construct ResumeRadioGroup
	/**
	 * Takes value 1 if self is to move, 0 if opponent is to move
	 */
	public static final String SELF_TO_MOVE_COL = "self_to_move";
	
	// For results in ChooseAction
	public static final int CHOOSE_ACTION_ACTIVITY = 0;
	public static final int RESULT_PAUSE_GAME = Activity.RESULT_FIRST_USER;
	public static final int RESULT_RESIGN = Activity.RESULT_FIRST_USER + 1;
	public static final int RESULT_CALL_DRAW = Activity.RESULT_FIRST_USER + 2;
	public static final int RESULT_OFFER_DRAW = Activity.RESULT_FIRST_USER + 3;
	
	// For GameOver activity
	public static final int GAME_OVER_ACTIVITY = 1;
	public static final int CONTACT_SELECTOR_ACTIVITY = 2;
	
	/**
	 * The following constants are also used in the remote database
	 */
	// Game outcome
	// Until game is complete
	public static final int UNFINISHED_GAME = 0;
	public static final int DRAW_OFFERED = 1;
	public static final int DRAW_OFFER_ACCEPTED = 2;
	public static final int DRAW_OFFER_DECLINED = 3;
	public static final int DRAW_ACCEPTED_RECEIVED = 4;
	public static final int DRAW_DECLINED_CONFIRMED = 5;
	public static final int DRAW_OFFERED_BY_OPPONENT = 6;
	public static final int DRAW_CALLED = 7;
	public static final int WHITE_RESIGNED = 8;
	public static final int BLACK_RESIGNED = 9;
	// public static final int DRAW_OFFER_DECLINED_BY_ME = 6;	
	public static final int WHITE_WINS_BY_CHECKMATE = 10;
	public static final int BLACK_WINS_BY_CHECKMATE = 11;
	public static final int DRAW_BY_STALEMATE = 12;
	// Not to be confused with DRAW_BY_NO_MORE_CHECKMATE, which is used for final message !!!
	public static final int DRAW_BY_NO_MORE_CHECKMATE_FOR_DB = 13;
	public static final int DRAW_BY_REPETITION_FOR_DB = 14;
	public static final int DRAW_BY_FIFTY_FOR_DB = 15;
	// Final results
	public static final int WHITE_WINS = 100;
	public static final int BLACK_WINS = WHITE_WINS + 1;
	public static final int DRAW = WHITE_WINS + 2;

	// This is used to signal remote script that input is a result rather than a move
	public static final int RESULT_SIGNAL = 100;
	// These are used to select final message
	public static final int OPPONENT_RESIGNED = 1;
	public static final int YOU_RESIGNED = 2;
	public static final int YOU_GOT_CHECKMATED = 3;
	public static final int OPPONENT_GOT_CHECKMATED = 4;
	public static final int STALEMATE = 5;
	public static final int DRAW_BY_AGREEMENT = 6;
	public static final int DRAW_BY_NO_MORE_CHECKMATE = 7;
	public static final int DRAW_BY_REPETITION = 8;
	public static final int DRAW_BY_FIFTY_MOVE_RULE = 9;
	public static final int NO_DRAW = 10;
	
	// For connecting to server
	public static final String WEBSITE = "http://codemelon.com/";
	public static final String DIRECTORY = "script/android/chess_with_humans/";
	public static final String REGISTRATION_SCRIPT = "register.php";
	public static final String FIND_PLAYER_SCRIPT = "find_player.php";
	public static final String CREATE_CHALLENGE_SCRIPT = "create_challenge.php";
	public static final String FIND_CHALLENGE_SCRIPT = "find_challenge.php";
	public static final String VIEW_CHALLENGES_SCRIPT = "view_challenges.php";
	public static final String ANSWER_CHALLENGE_SCRIPT = "answer_challenge.php";
	public static final String CREATE_GAME_SCRIPT = "create_game.php";
	public static final String VIEW_CHALLENGE_ANSWERS_SCRIPT = "view_challenge_answers.php";
	public static final String GET_GAME_PLAYERS_SCRIPT = "get_game_players.php";
	public static final String GET_LAST_MOVE_SCRIPT = "get_last_move.php";
	public static final String UPDATE_MOVES_AND_RESULTS_SCRIPT = "update_moves_and_results.php";
	public static final String INSERT_MOVE_SCRIPT = "insert_move.php";
	
	public static final int CHESS_CONNECT_TIMEOUT = 5000;
	
	// For creating an open challenge
	/* Note that this is the value stored in the remote database
	 * for player with handle 'any'
	 */
	public static final int PLAYER_ANY_ID = 1;
	/* A challenge is first listed as open when posted.
	 * When the opponent accepts or declines, it is then
	 * listed as such.
	 * Finally, when the original challenger gets the message stating
	 * challenge status, the challenge is deleted.
	 */
	public static final int CHALLENGE_OPEN = 0;
	public static final int CHALLENGE_ACCEPTED = 1;
	public static final int CHALLENGE_DECLINED = 2;
	
	// For ContentProvider (used for self-identification)
	// TODO I don't think this is used (3/7/11)
	// Cf. Burnette, p. 186
	public static final String AUTHORITY = "com.codemelon.chesswithhumans";
	public static final String PROVIDER_NAME = "self_data";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PROVIDER_NAME);
	
	// For identifying BoardView
	public static final int BOARD_VIEW_ID = 1;
	
	// For extra Intent data
	public static final String OPPONENT_ID = "com.codemelon.chesswithhumans.opponentId";
	public static final String GAME_ID = "com.codemelon.chesswithhumans.gameId";
	public static final String COLOR_RESULT = "com.codemelon.chesswithhumans.colorResult";
	public static final String SELF_RESULT = "com.codemelon.chesswithhumans.selfResult";
	public static final String OPEN_CHALLENGE_DATA = "com.codemelon.chesswithhumans.openChallengeData";
	
	// For move array in ChessConnectionAdapter
	public static final int MOVE_NUMBER = 0;
	public static final int MOVE_FROM = 1;
	public static final int MOVE_TO = 2;
	public static final int MOVE_PIECE = 3;
}
