/**
 * @file
 */
package com.codemelon.chesswithhumans;

import static com.codemelon.chesswithhumans.Constants.*;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Copyright (c) 2011 Marshall Farrier
 * @author Marshall Farrier
 * @version 0.1 1/28/11
 * 
 * To do:
 * - Stalemate/checkmate not yet implemented
 * - Draw by repetition not implemented (wait until early interactive versions: new menu required)
 * - Draw by "50 move rule without promotion or taking piece" not implemented
 * - Draw by insufficient material not implemented yet
 * - Resigning not implemented
 * - Interaction with database not implemented
 */
public class BoardView extends View {
	private static final String TAG = "ChessWithFriends";	
	private static final int SQUARES_PER_SIDE = 8;
	private static final int PROMOTION_CHOICES = 4;
	private final Game game;
	private Resources res;
	private float boardSize;
	private float size;		// Width and height of each square
	private int selXFrom;
	private int selYFrom;
	private int selXTo;
	private int selYTo;
	private final Rect selRectFrom = new Rect();
	private final Rect selRectTo = new Rect();
	// To avoid repeatedly memory allocation
	private Rect tmpRect;
	
	private Paint boardBackground;
	private Paint darkSquares;
	private Paint lightSquares;
	private Paint gridlines;
	private Paint hiliteSquare;
	private Paint hiliteGridline;
	private Paint piecePainter;
	private Paint buttonBackground;
	private Paint buttonForeground;
	private Paint buttonHilite;
	private Paint promotionForeground;
	private Paint promotionBackground;
	private Paint pressForChoicesForeground;
	private Paint whoseMoveForeground;
	
	private String confirmMoveLabel;
	private String abortMoveLabel;
	private String promoteLabel;
	private String pressForChoicesLabel;
	private String pressToExitLabel;
	private String yourMoveLabel;
	private String opponentsMoveLabel;
	private String drawLabel;
	private String whiteWinsLabel;
	private String blackWinsLabel;
	// Set up in onSizeChanged()
	private RectF confirmMoveButton;
	private RectF abortMoveButton;
	// private RectF whoseMoveText;
	private Rect pressForChoicesText;
	private float buttonPadding;
	private float buttonHeight;
	private float buttonTop;
	
	// For pawn promotion menu
	private Rect[] promotionChoices;
	
	private Bitmap[] pieces;
	private Position pos;
	private boolean white;
	
	private int mode;
	public static final int PLAY_GAME = 0;
	public static final int VIEW_GAME = 1;
	
	private boolean showConfirmAbort;
	private boolean confirmMove;
	private boolean abortMove;
	
	private boolean showPromotionChoices;
	private int promoteTo;	
	
	/**
	 * 
	 * @param ctx
	 * @param isWhite
	 * @param m Mode
	 */
	public BoardView(Context ctx, int m) {
		super(ctx);
		game = (Game) ctx;
		// Set up the board
		pos = new Position();
		init(ctx, m);
	}
	
	public BoardView(Context ctx, int m, ArrayList<Move> moveList) {
		super(ctx);
		game = (Game) ctx;
		// Set up the board
		pos = new Position(moveList);
		init(ctx, m);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		boardSize = h - 1.0f;
		// Portrait orientation
		if (h > w) {
			boardSize = w - 1.0f;
		}
		size = boardSize / SQUARES_PER_SIDE;
		if (selXFrom >= 0) {
			getRect(selXFrom, selYFrom, selRectFrom, 0);
			if (selXTo >= 0) {
				getRect(selXTo, selYTo, selRectTo, 0);
			}
		}
		
		// Set up confirm-abort buttons
		buttonPadding = boardSize / 11.0f;
		buttonHeight = 3.0f * buttonForeground.getTextSize();
		buttonTop = boardSize / 2 + h / 2.0f - buttonHeight / 2;
		confirmMoveButton = new RectF(buttonPadding, buttonTop, 
				buttonPadding * 5, buttonTop + buttonHeight);
		abortMoveButton = new RectF(buttonPadding * 6, buttonTop,
				buttonPadding * 10, buttonTop + buttonHeight);
		// whoseMoveText = new Rect(0, boardSize, boardSize, boardSize + 2 * whoseMoveForeground.getTextSize());
		pressForChoicesText = new Rect(0, (int) boardSize, (int) boardSize, h);
		
		Log.d(TAG, "onSizeChanged: " + size);
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	@Override
	protected void onDraw(Canvas canv) {
		int i, j;
		float w = getWidth();
		float h = getHeight();
		// Draw the background		
		canv.drawRect(0, 0, w, h, boardBackground);
		// Draw the board
		for (i = 0; i < SQUARES_PER_SIDE; ++i) {
			for (j = 0; j < SQUARES_PER_SIDE; ++j) {
				if ((i + j) % 2 == 0) {
					// Light square
					canv.drawRect(i * size, j * size, i * size + size,
							j * size + size, lightSquares);
				}
				else {	// Dark square
					canv.drawRect(i * size, j * size, i * size + size,
							j * size + size, darkSquares);
				}
			}
		}
			
		// Draw the gridlines		
		for (i = 0; i <= SQUARES_PER_SIDE; ++i) {
			// Across
			canv.drawLine(0, i * size, boardSize, i * size, gridlines);
			// Down
			canv.drawLine(i * size, 0, i * size, boardSize, gridlines);
		}
		//*/
		
		Rect r = new Rect();
		// Draw the selection
		if (selXFrom >= 0) {
			getRect(selXFrom, selYFrom, r, 0);
			canv.drawRect(r, hiliteSquare);
			hiliteSquareOutline(selXFrom, selYFrom, canv);
			if (selXTo >= 0) {
				getRect(selXTo, selYTo, r, 0);
				canv.drawRect(r, hiliteSquare);
				hiliteSquareOutline(selXTo, selYTo, canv);
			}
		}
		
		// Draw the pieces		
		drawPieces(canv, r);
		
		// Draw Confirm / Abort buttons as needed
		if (showConfirmAbort) {	
			showWhoseMove(canv);
			showConfirmAbort(canv);			
		}
		else if (showPromotionChoices) {
			showPromotionChoices(canv);
		}
		else {
			showWhoseMove(canv);
			showPressForChoices(canv);
			// TODO pressHereMessage(canv);
		}
	}
	
	/**
	 * Cf. Burnette, p. 78
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int act = event.getAction();
		float eventX = event.getX();
		float eventY = event.getY();
		if (showConfirmAbort) {
			return confirmAbortHandler(event, act, eventX, eventY);			
		}
		if (showPromotionChoices) {
			return promotionHandler(event, act, eventX, eventY);
		}
		if (act != MotionEvent.ACTION_DOWN) {
			return super.onTouchEvent(event);
		}
		if (eventY > boardSize) {
			return chooseActionHandler();
		}
		int x = (int)(eventX / size);
		int y = (int)(eventY / size);
		select(x, y);
		Log.d(TAG, "onTouchEvent: x " + x + ", y " + y);
		return true;
	}
	
	/**
	 * Returns the code corresponding to the current draw status, e.g.
	 * DRAW_BY_REPETITION, etc. (cf. respective constants)
	 * @return
	 */
	public int drawStatus() {
		return pos.drawStatus();
	}
	
	public boolean myMove() {
		if (white) {
			if (pos.hasMove() == Position.WHITE) return true;
		}
		else {
			if (pos.hasMove() == Position.BLACK) return true;
		}		
		return false;
	}
	
	private boolean chooseActionHandler() {
		if (game.colorResult() < WHITE_WINS) {
			game.startChooseAction();
		}
		else {
			game.exitGame();
		}
		return true;
	}
	
	/**
	 * 
	 * @param event
	 * @param action
	 * @param x
	 * @param y
	 * @return
	 */
	private boolean confirmAbortHandler(MotionEvent event, int action, float x, float y) {
		if (action == MotionEvent.ACTION_UP) {
			if (abortMove) {
				abort();
				abortMove = false;
				confirmMove = false;
				return true;
			}
			if (confirmMove) {
				move();
				confirmMove = false;
				abortMove = false;
				return true;
			}				
			return super.onTouchEvent(event);
		}
		if (action != MotionEvent.ACTION_DOWN) {
			return super.onTouchEvent(event);
		}
		if (y > boardSize) {
			if (confirmMoveButton.contains(x, y)) {
				confirmMove = true;
				/*
				Rect r = new Rect();
				confirmMoveButton.roundOut(r);
				invalidate(r);
				*/
				invalidate(pressForChoicesText);
				return true;
			}
			else if (abortMoveButton.contains(x, y)) {
				abortMove = true;
				/*
				Rect r = new Rect();
				abortMoveButton.roundOut(r);
				invalidate(r);
				*/
				invalidate(pressForChoicesText);
				return true;
			}			
		}
		return super.onTouchEvent(event);
	}
	
	private boolean promotionHandler(MotionEvent event, int action, float x, float y) {
		if (action == MotionEvent.ACTION_DOWN && promoteTo == Position.NONE) {
			for (int i = 0; i < PROMOTION_CHOICES; ++i) {
				if (promotionChoices[i].contains((int) x, (int) y)) {
					promoteTo = pos.hasMove() * Position.PIECES + i;
					tmpRect.set(promotionChoices[i]);
					tmpRect.inset(-2, -2);
					invalidate(tmpRect);
					return true;
				}
			}
		}
		else if (action == MotionEvent.ACTION_UP && promoteTo != Position.NONE) {
			showConfirmAbort = true;
			showPromotionChoices = false;
			tmpRect.set(0, (int) boardSize, getWidth(), getHeight());
			invalidate(tmpRect);
			getRect(selXFrom, selYFrom, tmpRect, 2);
			invalidate(tmpRect);
			getRect(selXTo, selYTo, tmpRect, 2);
			invalidate(tmpRect);
		}
		return super.onTouchEvent(event);
	}
	
	/**
	 * Executes move stored in sel?From and sel?To.
	 * Clears highlighted squares.
	 * Removes confirm-abort buttons.
	 * Also finds appropriate target piece in case of pawn promotion.
	 */
	private void move() {
		int from = getSquare(selXFrom, selYFrom);
		int to = getSquare(selXTo, selYTo);
		int piece = pos.getPiece(from);
		boolean checkForInsufficientMaterial = false;
		
		// Get piece for pawn promotion as needed
		if (promoteTo != Position.NONE) {
			piece = promoteTo;
			// In case promoting to a knight or bishop leads to insufficient material
			checkForInsufficientMaterial = true;
		}
		else if (pos.getPiece(to) != Position.NONE && pos.getPiece(from) % Position.PIECES != Position.PAWN
				&& pos.getPiece(from) % Position.PIECES != Position.QUEEN
				&& pos.getPiece(from) % Position.PIECES != Position.ROOK) {
			checkForInsufficientMaterial = true;
		}
		// Make the move in the Position object
		pos.move(from, to, piece);
		// Enter move in database
		game.move(from, to, piece);
		promoteTo = Position.NONE;
		
		clearSelection();
		
		// Handle checkmate and stalemate if necessary
		if (!pos.legalMoveExists()) {
			if (pos.isInCheck(pos.hasMove())) {
				// TODO handle checkmate in Game
				if (pos.hasMove() == Position.WHITE) {
					game.checkmate(from, to, piece, BLACK_WINS);
					return;
				}
				else {
					game.checkmate(from, to, piece, WHITE_WINS);
					return;
				}
			}
			else {
				game.stalemate(from, to, piece);
				return;
			}
		}
		else if (checkForInsufficientMaterial) {
			if (pos.checkmateImpossible()) {
				game.handleDraw(DRAW_BY_NO_MORE_CHECKMATE);
				return;
			}
		}
		// Note the above return statements if status != UNFINISHED_GAME
		game.sendMove(from, to, piece, UNFINISHED_GAME);
	}
	
	private void abort() {		
		clearSelection();
	}
	
	private void clearSelection() {
		// Clear selected squares
		Rect r1 = new Rect();
		Rect r2 = new Rect();
		int fromSquare = getSquare(selXFrom, selYFrom);
		int toSquare = getSquare(selXTo, selYTo);
		// if e.p., update square of taken pawn
		if (isEnPassant(fromSquare, toSquare)) {
			int takenPawnY = selYTo;
			if (white) {	// white perspective
				if (pos.hasMove() == Position.BLACK) ++takenPawnY;
				else --takenPawnY;
			}
			else {	// black perspective
				if (pos.hasMove() == Position.BLACK) --takenPawnY;
				else ++takenPawnY;
			}
			getRect(selXTo, takenPawnY, r1, 0);
		}
		// Recalculate rook squares in case of castling
		else if (isCastling(fromSquare, toSquare)) {
			getRect(selXTo + 1, selYTo, r1, 0);
			int otherX;
			if (selXFrom < selXTo) {	// King's side
				otherX = selXTo - 1;
			}
			else {	// Queen's side
				otherX = selXTo - 2;
			}
			getRect(otherX, selYTo, r2, 0);			
		}
		// Restore state of promoteTo for pawn promotion
		promoteTo = Position.NONE;
		getRect(selXFrom, selYFrom, tmpRect, 2);
		selXFrom = -1;
		selYFrom = -1;
		invalidate(tmpRect);
		getRect(selXTo, selYTo, tmpRect, 2);
		selXTo = -1;
		selYTo = -1;
		invalidate(tmpRect);
		
		if (!r1.isEmpty()) invalidate(r1);
		if (!r2.isEmpty()) invalidate(r2);
		
		// Hide confirm-abort menu
		showConfirmAbort = false;
		tmpRect = new Rect(0, (int) boardSize, (int) boardSize, (int) getHeight());
		invalidate(tmpRect);
	}
	
	/**
	 * Can make rectangle a little larger to capture overlapping outline of hilite
	 * @param x
	 * @param y
	 * @param r
	 */
	private void getRect(int x, int y, Rect r, int overlap) {
		r.set((int)(x * size) - overlap, (int)(y * size) - overlap, 
				(int)(x * size + size) + overlap, (int)(y * size + size) + overlap);
	}
	
	private void hiliteSquareOutline(int x, int y, Canvas canv) {
		canv.drawLine(x * size, y * size, x * size, y * size + size, hiliteGridline);
		canv.drawLine(x * size, y * size + size, x * size + size, y * size + size, hiliteGridline);
		canv.drawLine(x * size + size, y * size, x * size + size, y * size + size, hiliteGridline);
		canv.drawLine(x * size, y * size, x * size + size, y * size, hiliteGridline);
	}
	
	private void drawPieces(Canvas canv, Rect r) {
		int i, piece;
		if (selXTo == -1) {	// No target square selected
			if (white) {
				for (i = 0; i < Position.SQUARES; ++i) {
					piece = pos.getPiece(i);
					if (piece != Position.NONE) {
						getRect(i % Position.BOARD_SIZE, 7 - (i / Position.BOARD_SIZE), r, 0);
						canv.drawBitmap(pieces[piece], null, r, piecePainter);
					}
				}
			}
			else {
				for (i = 0; i < Position.SQUARES; ++i) {
					piece = pos.getPiece(i);
					if (piece != Position.NONE) {
						getRect(7 - i % Position.BOARD_SIZE, i / Position.BOARD_SIZE, r, 0);
						canv.drawBitmap(pieces[piece], null, r, piecePainter);
					}
				}
			}
		}
		else {	// Both from and to square selected
			int fromSquare, toSquare;
			
			if (white) {	// White perspective
				fromSquare = (7 - selYFrom) * Position.BOARD_SIZE + selXFrom;
				toSquare = (7 - selYTo) * Position.BOARD_SIZE + selXTo;
				// Determine if castling move
				if (isCastling(fromSquare, toSquare)) {
					int rookFromSquare, rookToSquare;
					if (fromSquare < toSquare) {	// King's side
						rookFromSquare = toSquare + 1;
						rookToSquare = toSquare - 1;
					}
					else {	// Queen's side
						rookFromSquare = toSquare - 2;
						rookToSquare = toSquare + 1;
					}
					for (i = 0; i < Position.SQUARES; ++i) {
						if (i != fromSquare && i != toSquare && i != rookFromSquare) {
							if (i != rookToSquare) {
								piece = pos.getPiece(i);
								if (piece != Position.NONE) {
									getRect(i % Position.BOARD_SIZE, 7 - (i / Position.BOARD_SIZE), r, 0);
									canv.drawBitmap(pieces[piece], null, r, piecePainter);
								}
							}
							else {
								getRect(i % Position.BOARD_SIZE, 7 - (i / Position.BOARD_SIZE), r, 0);
								canv.drawBitmap(pieces[pos.getPiece(rookFromSquare)], null, r, piecePainter);
							}
						}
					}
				}
				// Case of pawn promotion
				else if (isPawnPromotion(fromSquare, toSquare)) {
					// Promotion piece not yet selected (don't move piece yet)
					if (promoteTo == Position.NONE) {
						for (i = 0; i < Position.SQUARES; ++i) {
							piece = pos.getPiece(i);
							if (piece != Position.NONE) {
								getRect(i % Position.BOARD_SIZE, 7 - (i / Position.BOARD_SIZE), r, 0);
								canv.drawBitmap(pieces[piece], null, r, piecePainter);
							}
						}
					}
					// Promotion piece selected
					else {					
						for (i = 0; i < Position.SQUARES; ++i) {
							if (i != fromSquare && i != toSquare) {
								piece = pos.getPiece(i);
								if (piece != Position.NONE) {
									getRect(i % Position.BOARD_SIZE, 7 - (i / Position.BOARD_SIZE), r, 0);
									canv.drawBitmap(pieces[piece], null, r, piecePainter);
								}
							}
							getRect(selXTo, selYTo, r, 0);
							canv.drawBitmap(pieces[promoteTo], null, r, piecePainter);
						}
					}
				}
				// Determine possible e.p. move
				else if (!isEnPassant(fromSquare, toSquare)) {
					for (i = 0; i < Position.SQUARES; ++i) {
						if (i != fromSquare && i != toSquare) {
							piece = pos.getPiece(i);
							if (piece != Position.NONE) {
								getRect(i % Position.BOARD_SIZE, 7 - (i / Position.BOARD_SIZE), r, 0);
								canv.drawBitmap(pieces[piece], null, r, piecePainter);
							}
						}
					}
				}
				else {	// Move is e.p.
					int takenPawnSquare = toSquare;
					if (pos.hasMove() == Position.BLACK) takenPawnSquare += Position.BOARD_SIZE;
					else takenPawnSquare -= Position.BOARD_SIZE;
					for (i = 0; i < Position.SQUARES; ++i) {
						if (i != fromSquare && i != toSquare && i != takenPawnSquare) {
							piece = pos.getPiece(i);
							if (piece != Position.NONE) {
								getRect(i % Position.BOARD_SIZE, 7 - (i / Position.BOARD_SIZE), r, 0);
								canv.drawBitmap(pieces[piece], null, r, piecePainter);
							}
						}
					}
				}
			}
			else {	// Black perspective
				fromSquare = selYFrom * Position.BOARD_SIZE + 7 - selXFrom;
				toSquare = selYTo * Position.BOARD_SIZE + 7 - selXTo;
				// Determine if castling move
				if (isCastling(fromSquare, toSquare)) {
					int rookFromSquare, rookToSquare;
					if (fromSquare < toSquare) {	// King's side
						rookFromSquare = toSquare + 1;
						rookToSquare = toSquare - 1;
					}
					else {	// Queen's side
						rookFromSquare = toSquare - 2;
						rookToSquare = toSquare + 1;
					}
					for (i = 0; i < Position.SQUARES; ++i) {
						if (i != fromSquare && i != toSquare && i != rookFromSquare) {
							if (i != rookToSquare) {
								piece = pos.getPiece(i);
								if (piece != Position.NONE) {
									getRect(7 - i % Position.BOARD_SIZE, i / Position.BOARD_SIZE, r, 0);
									canv.drawBitmap(pieces[piece], null, r, piecePainter);
								}
							}
							else {
								getRect(7 - i % Position.BOARD_SIZE, i / Position.BOARD_SIZE, r, 0);
								canv.drawBitmap(pieces[pos.getPiece(rookFromSquare)], null, r, piecePainter);
							}
						}
					}
				}
				// Case of pawn promotion
				else if (isPawnPromotion(fromSquare, toSquare)) {
					// Promotion piece not yet selected (don't move piece yet)
					if (promoteTo == Position.NONE) {
						for (i = 0; i < Position.SQUARES; ++i) {
							piece = pos.getPiece(i);
							if (piece != Position.NONE) {
								getRect(7 - i % Position.BOARD_SIZE, i / Position.BOARD_SIZE, r, 0);
								canv.drawBitmap(pieces[piece], null, r, piecePainter);
							}
						}
					}
					// Promotion piece selected
					else {					
						for (i = 0; i < Position.SQUARES; ++i) {
							if (i != fromSquare && i != toSquare) {
								piece = pos.getPiece(i);
								if (piece != Position.NONE) {
									getRect(7 - i % Position.BOARD_SIZE, i / Position.BOARD_SIZE, r, 0);
									canv.drawBitmap(pieces[piece], null, r, piecePainter);
								}
							}
							getRect(selXTo, selYTo, r, 0);
							canv.drawBitmap(pieces[promoteTo], null, r, piecePainter);
						}
					}
				}
				// Determine possible e.p. move
				else if (!isEnPassant(fromSquare, toSquare)) {
					for (i = 0; i < Position.SQUARES; ++i) {
						if (i != fromSquare && i != toSquare) {
							piece = pos.getPiece(i);
							if (piece != Position.NONE) {
								getRect(7 - i % Position.BOARD_SIZE, i / Position.BOARD_SIZE, r, 0);
								canv.drawBitmap(pieces[piece], null, r, piecePainter);
							}
						}
					}
				}
				else {	// Move is e.p.
					int takenPawnSquare = toSquare;
					if (pos.hasMove() == Position.BLACK) takenPawnSquare += Position.BOARD_SIZE;
					else takenPawnSquare -= Position.BOARD_SIZE;
					for (i = 0; i < Position.SQUARES; ++i) {
						if (i != fromSquare && i != toSquare && i != takenPawnSquare) {
							piece = pos.getPiece(i);
							if (piece != Position.NONE) {
								getRect(7 - i % Position.BOARD_SIZE, i / Position.BOARD_SIZE, r, 0);
								canv.drawBitmap(pieces[piece], null, r, piecePainter);
							}
						}
					}
				}
			}
			// Now draw piece stored in fromSquare on target square
			// Except for case of pawn promotion
			if (!isPawnPromotion(fromSquare, toSquare)) {
				getRect(selXTo, selYTo, r, 0);
				canv.drawBitmap(pieces[pos.getPiece(fromSquare)], null, r, piecePainter);
			}
		}
		
		
	}
	
	/**
	 * Cf. Burnette, p. 77
	 * @param x
	 * @param y
	 */
	private void select(int x, int y) {
		// Do nothing if it isn't your move
		if (mode == PLAY_GAME && white != pos.whiteToMove()) return;
		x = Math.min(Math.max(x, 0), 7);
		y = Math.min(Math.max(y, 0), 7);
		int fromSquare, toSquare, piece, myColor = pos.hasMove();
		
		Rect r = new Rect();
		// No source square selected
		if (selXFrom == -1) {
			fromSquare = getSquare(x, y);
			Log.d(TAG, "fromSquare: " + fromSquare);
			piece = pos.getPiece(fromSquare);
			Log.d(TAG, "piece: " + piece);
			if (piece == Position.NONE || piece / Position.PIECES != myColor) return; // Square unselectable, so do nothing
			selXFrom = x;
			selYFrom = y;
			getRect(selXFrom, selYFrom, selRectFrom, 0);
			getRect(selXFrom, selYFrom, r, 2);
			invalidate(r);
		}
		else if (selXTo == -1) {
			// Reselecting same square will invalidate 'from' selection
			if (x == selXFrom && y == selYFrom) {
				getRect(selXFrom, selYFrom, r, 2);	// We need to invalidate with overlap here
				selXFrom = -1;
				selYFrom = -1;
				invalidate(r);
			}
			// Source square selected
			// If target square already selected, do nothing
			else {
				fromSquare = getSquare(selXFrom, selYFrom);
				toSquare = getSquare(x, y);
				// Only valid moves are selectable
				if (!pos.isMove(fromSquare, toSquare)) return;
				selXTo = x;
				selYTo = y;
				getRect(selXTo, selYTo, selRectTo, 0);
				getRect(selXTo, selYTo, r, 2);
				invalidate(selRectFrom);
				invalidate(r);
				// For e.p. also invalidate rectangle of taken pawn
				if (isEnPassant(fromSquare, toSquare)) {
					int takenPawnY = selYTo;
					if (white) {	// white perspective
						if (pos.hasMove() == Position.BLACK) ++takenPawnY;
						else --takenPawnY;
					}
					else {	// black perspective
						if (pos.hasMove() == Position.BLACK) --takenPawnY;
						else ++takenPawnY;
					}
					getRect(selXTo, takenPawnY, r, 0);
					invalidate(r);
				}
				// Take care of castling
				else if (isCastling(fromSquare, toSquare)) {
					// just invalidate the appropriate squares
					getRect(selXTo + 1, selYTo, r, 0);
					invalidate(r);
					int otherX;
					if (selXFrom < selXTo) {	// King's side
						otherX = selXTo - 1;
					}
					else {	// Queen's side
						otherX = selXTo - 2;
					}
					getRect(otherX, selYTo, r, 0);
					invalidate(r);
				}
				// Pawn promotion
				if (isPawnPromotion(fromSquare, toSquare)) showPromotionChoices = true;
				// Call confirm / abort menu
				else showConfirmAbort = true;
				
				r = new Rect(0, (int) boardSize, (int) boardSize, getHeight());
				invalidate(r);
			}
		}
	}
	
	/**
	 * Returns the square number corresponding to the given x
	 * and y coordinates. Also adjusts according to whether we
	 * are looking at the board from the black or the white
	 * perspective.
	 * Performs no validation of input data.
	 * @param x
	 * @param y
	 * @return
	 */
	private int getSquare(int x, int y) {
		if (white) {
			return Position.BOARD_SIZE * (7 - y) + x;
		}
		else {
			return Position.BOARD_SIZE * y + 7 - x;
		}
	}
	
	private void showConfirmAbort(Canvas canvas) {
		// Cf. paad, p. 101
		// Draw buttons		
		// Confirm button		
		canvas.drawRect(confirmMoveButton, buttonBackground);
		if (confirmMove) canvas.drawRect(confirmMoveButton, buttonHilite);
		float textWidth = buttonForeground.measureText(confirmMoveLabel);
		canvas.drawText(confirmMoveLabel, buttonPadding * 3 - textWidth / 2,
				buttonTop + buttonHeight * 0.65f, buttonForeground);
		// Abort button		
		canvas.drawRect(abortMoveButton, buttonBackground);
		if (abortMove) canvas.drawRect(abortMoveButton, buttonHilite);
		textWidth = buttonForeground.measureText(abortMoveLabel);
		canvas.drawText(abortMoveLabel, buttonPadding * 8 - textWidth / 2,
				buttonTop + buttonHeight * 0.65f, buttonForeground);
	}
	
	private void showWhoseMove(Canvas canvas) {
		String whoseMoveStr = yourMoveLabel;
		int colorResult = game.colorResult();
		if (colorResult == UNFINISHED_GAME) {
			if (!myMove()) {
				whoseMoveStr = opponentsMoveLabel;
			}
		}
		else if (colorResult == WHITE_WINS) {
			whoseMoveStr = whiteWinsLabel;
		}
		else if (colorResult == BLACK_WINS) {
			whoseMoveStr = blackWinsLabel;
		}
		else if (colorResult == DRAW) {
			whoseMoveStr = drawLabel;
		}
		float padding = whoseMoveForeground.getTextSize();
		canvas.drawText(whoseMoveStr, padding, boardSize + 2 * padding, whoseMoveForeground);
	}
	
	private void showPressForChoices(Canvas canvas) {
		if (game.colorResult() < WHITE_WINS) {
			canvas.drawText(pressForChoicesLabel, pressForChoicesText.centerX(), 
				pressForChoicesText.centerY(), pressForChoicesForeground);
		}
		else {
			canvas.drawText(pressToExitLabel, pressForChoicesText.centerX(), 
					pressForChoicesText.centerY(), pressForChoicesForeground);
		}
	}
	
	private void showPromotionChoices(Canvas canvas) {
		// Determine size for drawing pieces
		int h = getHeight();
		int w = getWidth();
		int hasMove = pos.hasMove();
		float pieceSize = (h - boardSize) / 3.0f;
		if (pieceSize > w / 5.0f) pieceSize = w / 5.0f;
		// Determine text height
		float textHeight = promotionForeground.getTextSize();
		// Determine padding for top and bottom
		float paddingTop = (h - boardSize - pieceSize - 2 * textHeight) / 2;
		// Determine padding between piece images
		float paddingLeft = (w - 4 * pieceSize) / 5;
		// Change background color
		// canvas.drawRect(0, boardSize, w, h, promotionBackground);
		// Draw text
		canvas.drawText(promoteLabel, paddingLeft, boardSize + paddingTop + textHeight / 2, promotionForeground);
		// Draw pieces
		int top = (int) (boardSize + paddingTop + 2 * textHeight);
		int i;
		for (i = 0; i < PROMOTION_CHOICES; ++i) {
			promotionChoices[i] = new Rect((int) (paddingLeft + i * (paddingLeft + pieceSize)),
					top, (int) (paddingLeft + pieceSize + i * (paddingLeft + pieceSize)), top + (int) pieceSize);
			canvas.drawRect(promotionChoices[i], promotionBackground);
			canvas.drawBitmap(pieces[hasMove * Position.PIECES + i], null, promotionChoices[i], piecePainter);
		}
		// Case when selection key is pushed down but hasn't yet been released
		if (promoteTo != Position.NONE) {
			canvas.drawRect(promotionChoices[promoteTo % Position.PIECES], hiliteSquare);
			canvas.drawRect(promotionChoices[promoteTo % Position.PIECES], hiliteGridline);
		}
		for (i = 0; i < PROMOTION_CHOICES; ++i) {
			canvas.drawBitmap(pieces[hasMove * Position.PIECES + i], null, promotionChoices[i], piecePainter);
		}
	}
	
	/**
	 * Returns true only if all of the following conditions hold:
	 * 1) The piece on fromSquare is a pawn
	 * 2) The move is not along the same file
	 * 3) The target square is empty
	 * @param fromSquare
	 * @param toSquare
	 * @return
	 */
	private boolean isEnPassant(int fromSquare, int toSquare) {
		if (pos.getPiece(fromSquare) % Position.PIECES == Position.PAWN) {
			if (pos.getPiece(toSquare) == Position.NONE) {
				if (Math.abs(fromSquare - toSquare) % Position.BOARD_SIZE != 0) return true;				
			}
		}
		return false;
	}
	
	/**
	 * Returns true if both:
	 * 1) The piece on fromSquare is a King
	 * 2) The distance involved in the move is 2 
	 * @param fromSquare
	 * @param toSquare
	 * @return
	 */
	private boolean isCastling(int fromSquare, int toSquare) {
		if (pos.getPiece(fromSquare) % Position.PIECES == Position.KING) {
			if (Math.abs(toSquare - fromSquare) == 2) return true;
		}
		return false;
	}
	
	/**
	 * Returns true if both:
	 * 1) Piece on fromSquare is a pawn
	 * 2) toSquare is on row 0 (first row) or 7 (last row)
	 * @param fromSquare
	 * @param toSquare
	 * @return
	 */
	private boolean isPawnPromotion(int fromSquare, int toSquare) {
		if (pos.getPiece(fromSquare) % Position.PIECES == Position.PAWN){
			int toRow = toSquare / Position.BOARD_SIZE;
			if (toRow == 0) return true;
			if (toRow == 7) return true;
		}
		return false;
	}
	
	private void init(Context ctx, int m) {
		setId(BOARD_VIEW_ID);
		white = game.white();
		// opponentId = oppId;
		mode = m;
		showConfirmAbort = false;
		confirmMove = false;
		abortMove = false;
		tmpRect = new Rect();
		showPromotionChoices = false;
		promoteTo = Position.NONE;
		promotionChoices = new Rect[PROMOTION_CHOICES];
		
		res = getResources();		
		
		boardBackground = new Paint();
		darkSquares = new Paint();
		lightSquares = new Paint();
		gridlines = new Paint();
		hiliteSquare = new Paint();
		hiliteGridline = new Paint();
		buttonBackground = new Paint();
		buttonForeground = new Paint(Paint.ANTI_ALIAS_FLAG);
		pressForChoicesForeground = new Paint(Paint.ANTI_ALIAS_FLAG);
		whoseMoveForeground = new Paint(Paint.ANTI_ALIAS_FLAG);
		buttonHilite = new Paint();
		promotionForeground = new Paint();
		promotionBackground = new Paint();
		boardBackground.setColor(res.getColor(
				R.color.board_background));
		darkSquares.setColor(res.getColor(
				R.color.dark_squares));
		lightSquares.setColor(res.getColor(
				R.color.light_squares));
		gridlines.setColor(res.getColor(
				R.color.gridlines));
		hiliteSquare.setColor(res.getColor(
				R.color.hilite_square));
		hiliteGridline.setColor(res.getColor(
				R.color.hilite_gridline));
		hiliteGridline.setStrokeWidth(3.0f);
		hiliteGridline.setStyle(Paint.Style.STROKE);
		buttonBackground.setColor(res.getColor(
				R.color.button_background));
		buttonForeground.setColor(res.getColor(
				R.color.button_foreground));
		buttonHilite.setColor(res.getColor(R.color.button_hilite));
		promotionForeground.setColor(res.getColor(R.color.promotion_text));
		promotionBackground.setColor(res.getColor(R.color.promotion_background));
		whoseMoveForeground.setColor(res.getColor(R.color.whose_move_foreground));
		whoseMoveForeground.setTextAlign(Paint.Align.LEFT);
		pressForChoicesForeground.setColor(res.getColor(R.color.press_for_choices_foreground));
		pressForChoicesForeground.setTextSize(16.0f);
		pressForChoicesForeground.setTextAlign(Paint.Align.CENTER);
		
		piecePainter = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
		
		// Various labels
		yourMoveLabel = res.getString(R.string.your_move_label);
		opponentsMoveLabel = res.getString(R.string.opponents_move_label);
		drawLabel = res.getString(R.string.draw_move_label);
		whiteWinsLabel = res.getString(R.string.white_wins_move_label);
		blackWinsLabel = res.getString(R.string.black_wins_move_label);
		pressForChoicesLabel = res.getString(R.string.press_for_choices_label);
		pressToExitLabel = res.getString(R.string.press_to_exit_label);
		// Set up parameters for confirm and abort buttons
		confirmMoveLabel = res.getString(R.string.confirm_move_label);
		abortMoveLabel = res.getString(R.string.abort_move_label);
		// Set up pawn promotion label
		promoteLabel = res.getString(R.string.promote_label);
		
		// Initial selection
		selXFrom = -1;
		selYFrom = -1;
		selXTo = -1;
		selYTo = -1;
		
		// Get pieces from .png resources
		pieces = new Bitmap[2 * Position.PIECES];
		
		BitmapFactory.Options opts = new BitmapFactory.Options();		
		opts.inSampleSize = 2;
		opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
		//*/
		pieces[Position.WHITE * Position.PIECES + Position.PAWN]
		       = BitmapFactory.decodeResource(res, R.drawable.white_pawn, opts);
		pieces[Position.WHITE * Position.PIECES + Position.ROOK]
		       = BitmapFactory.decodeResource(res, R.drawable.white_rook, opts);
		pieces[Position.WHITE * Position.PIECES + Position.KNIGHT]
		       = BitmapFactory.decodeResource(res, R.drawable.white_knight, opts);
		pieces[Position.WHITE * Position.PIECES + Position.BISHOP]
		       = BitmapFactory.decodeResource(res, R.drawable.white_bishop, opts);
		pieces[Position.WHITE * Position.PIECES + Position.QUEEN]
		       = BitmapFactory.decodeResource(res, R.drawable.white_queen, opts);
		pieces[Position.WHITE * Position.PIECES + Position.KING]
		       = BitmapFactory.decodeResource(res, R.drawable.white_king, opts);
		pieces[Position.BLACK * Position.PIECES + Position.PAWN]
		       = BitmapFactory.decodeResource(res, R.drawable.black_pawn, opts);
		pieces[Position.BLACK * Position.PIECES + Position.ROOK]
		       = BitmapFactory.decodeResource(res, R.drawable.black_rook, opts);
		pieces[Position.BLACK * Position.PIECES + Position.KNIGHT]
		       = BitmapFactory.decodeResource(res, R.drawable.black_knight, opts);
		pieces[Position.BLACK * Position.PIECES + Position.BISHOP]
		       = BitmapFactory.decodeResource(res, R.drawable.black_bishop, opts);
		pieces[Position.BLACK * Position.PIECES + Position.QUEEN]
		       = BitmapFactory.decodeResource(res, R.drawable.black_queen, opts);
		pieces[Position.BLACK * Position.PIECES + Position.KING]
		       = BitmapFactory.decodeResource(res, R.drawable.black_king, opts);
		
		setFocusable(true);
		setFocusableInTouchMode(true);
	}
}
