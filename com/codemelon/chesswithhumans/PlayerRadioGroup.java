/**
 * @file
 * Copyright (c) 2011 Marshall Farrier
 */
package com.codemelon.chesswithhumans;

import static com.codemelon.chesswithhumans.Constants.*;

import android.content.Context;
import android.database.Cursor;
import android.util.AttributeSet;
import android.widget.RadioButton;
import android.widget.RadioGroup;

/**
 * @author Marshall Farrier
 * @version 0.1 2/8/11
 */
public class PlayerRadioGroup extends RadioGroup {
	private ChessDataBaseAdapter dbAdapter;
	private Cursor playerCursor;
	private int numPlayers;
	private RadioButton[] playerRadio;

	public PlayerRadioGroup(Context context) {
		super(context);
		
		this.setOrientation(VERTICAL);		
		init(context);
	}
	
	public PlayerRadioGroup(Context context, AttributeSet ats) {
		super(context, ats);
		this.setOrientation(VERTICAL);	
		init(context);
	}
	
	private void init(Context context) {
		dbAdapter = new ChessDataBaseAdapter(context);
		dbAdapter.open();
		playerCursor = dbAdapter.getNewGamePlayers();
		
		numPlayers = playerCursor.getCount();
		
		final int playerNameCol = playerCursor.getColumnIndex(PLAYER_NAME_COL);
		final int playerIdCol = playerCursor.getColumnIndex(PLAYER_ID_COL);
		playerRadio = new RadioButton[numPlayers];
		
		playerCursor.moveToFirst();
		for (int i = 0; i < numPlayers; ++i) {
			playerRadio[i] = new RadioButton(context);
			playerRadio[i].setText(playerCursor.getString(playerNameCol));
			playerRadio[i].setTag(playerCursor.getInt(playerIdCol));
			this.addView(playerRadio[i], new LayoutParams(LayoutParams.WRAP_CONTENT, 
					LayoutParams.FILL_PARENT));
			playerCursor.moveToNext();
		}
		playerCursor.close();
		dbAdapter.close();
	}
}
