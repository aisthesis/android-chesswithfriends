/**
 * @file
 * Copyright (c) 2011 Marshall Farrier 
 */
package com.codemelon.chesswithhumans;

import static com.codemelon.chesswithhumans.Constants.*;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * @author Marshall Farrier
 * @version 0.1 2/8/11
 */
public class ResumeRadioGroup extends RadioGroup {
	private static final String TAG = "cwfResumeRadioGroup";
	private ChessDataBaseAdapter db;
	private Cursor playerCursor;
	private int numPlayers;
	private RadioButton[] playerRadio;
	private int selectedPlayer;
	private int selectedGame;
	private CompoundButton currentButton;
	
	public ResumeRadioGroup(Context context) {
		super(context);
		
		this.setOrientation(VERTICAL);		
		init(context);
	}
	
	public ResumeRadioGroup(Context context, AttributeSet ats) {
		super(context, ats);
		this.setOrientation(VERTICAL);	
		init(context);
	}
	
	private void init(Context context) {
		selectedPlayer = -1;
		selectedGame = -1;
		currentButton = null;
		db = new ChessDataBaseAdapter(context);
		db.open();
		playerCursor = db.getActiveGamePlayers();
		
		numPlayers = playerCursor.getCount();
		Log.d(TAG, numPlayers + " players found with active games");
		
		final int playerNameCol = playerCursor.getColumnIndex(PLAYER_NAME_COL);		
		final int playerIdCol = playerCursor.getColumnIndex(PLAYER_ID_COL);
		final int gameIdCol = playerCursor.getColumnIndex(GAME_ID_COL);
		final int selfToMoveCol = playerCursor.getColumnIndex(SELF_TO_MOVE_COL);
		playerRadio = new RadioButton[numPlayers];
		// new
		LinearLayout container;
		TextView moveText;
		
		playerCursor.moveToFirst();
		for (int i = 0; i < numPlayers; ++i) {
			container = new LinearLayout(context);
			playerRadio[i] = new RadioButton(context);
			playerRadio[i].setText(playerCursor.getString(playerNameCol));
			/*
			Log.d(TAG, "setTag() playerId: " + playerCursor.getInt(playerIdCol) + ", gameId: " +
					playerCursor.getInt(gameIdCol));
			playerRadio[i].setTag(new PlayerAndGame(playerCursor.getInt(playerIdCol), 
					playerCursor.getInt(gameIdCol)));
			*/
			final int GAME = playerCursor.getInt(gameIdCol);
			final int PLAYER = playerCursor.getInt(playerIdCol);
			playerRadio[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked && currentButton != buttonView) {
						if (currentButton != null) {
							currentButton.setChecked(false);
						}
						currentButton = buttonView;
					}
					if (isChecked) {
						selectedGame = GAME;
						selectedPlayer = PLAYER;
					}
					else {
						selectedGame = -1;
						selectedPlayer = -1;
					}					
				}
			});
			moveText = new TextView(context);
			moveText.setGravity(Gravity.RIGHT);
			if (playerCursor.getInt(selfToMoveCol) == 1) {
				moveText.setText("Your move!");
				moveText.setTextColor(Color.parseColor("#ffddad65"));
				container.setBackgroundColor(Color.parseColor("#ff67656c"));
			}
			container.setPadding(5, 0, 5, 0);
			container.setOrientation(HORIZONTAL);
			container.addView(playerRadio[i], LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			container.addView(moveText, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
			//*/
			this.addView(container, LayoutParams.FILL_PARENT, 
					LayoutParams.WRAP_CONTENT);
			playerCursor.moveToNext();
		}
		playerCursor.close();
		db.close();
	}
	
	public int selectedPlayer() { return selectedPlayer; }
	public int selectedGame() { return selectedGame; }
}
