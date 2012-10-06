/**
 * @file
 * Copyright (c) 2011 Marshall Farrier
 */
package com.codemelon.chesswithhumans;

import static com.codemelon.chesswithhumans.Constants.*;

// import com.codemelon.chesswithfriends.R;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

/**
 * @author Marshall Farrier
 * @version 0.1 2/12/11
 */
public class GameOutcomeTextView extends TextView {
	private static final String TAG = "cwhGameOutcomeTextView";
	private Resources res;

	public GameOutcomeTextView(Context context) {
		super(context);
		init(context);
	}

	public GameOutcomeTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	public GameOutcomeTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	private void init(Context context) {
		res = context.getResources();
		GameOver gameOverActivity = (GameOver) context;
		int selfResult = gameOverActivity.selfResult();
		int colorResult = gameOverActivity.colorResult();
		StringBuilder message = new StringBuilder("");
		switch (selfResult) {
		case OPPONENT_RESIGNED:
			message.append(res.getText(R.string.opponent_resigned_label));
			break;
		case YOU_RESIGNED:
			message.append(res.getText(R.string.you_resigned_label));
			break;
		case YOU_GOT_CHECKMATED:
			message.append(res.getText(R.string.you_got_checkmated_label));
			break;
		case OPPONENT_GOT_CHECKMATED:
			message.append(res.getText(R.string.you_won_by_checkmate_label));
			break;
		case STALEMATE:
			message.append(res.getText(R.string.stalemate_label));
			break;
		case DRAW_BY_AGREEMENT:
			message.append(res.getText(R.string.draw_by_agreement_label));
			break;
		case DRAW_BY_NO_MORE_CHECKMATE:
			message.append(res.getText(R.string.draw_by_checkmate_impossible_label));
			break;
		case DRAW_BY_REPETITION:
			message.append(res.getText(R.string.draw_by_repetition_label));
			break;
		case DRAW_BY_FIFTY_MOVE_RULE:
			message.append(res.getText(R.string.draw_by_fifty_move_rule_label));
			break;
		}
		switch (colorResult) {
		case WHITE_WINS:
			message.append("1:0");
			break;
		case BLACK_WINS:
			message.append("0:1");
			break;
		case DRAW:
			message.append("1/2:1/2");
			break;
		}
		setText(message);
		Log.d(TAG, "custom TextView initialized successfully");
	}
}
