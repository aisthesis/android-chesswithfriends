/**
 * @file
 * Copyright (c) 2011 Marshall Farrier
 */
package com.codemelon.chesswithhumans;

import static com.codemelon.chesswithhumans.Constants.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import android.util.Log;

/**
 * @author Marshall Farrier
 * @version 0.1 1/20/11
 * For storing a position and implementing game logic.
 * A Position object will:
 * 1) Store a position and move list in memory
 * 2) Test a prospective move for legality in the current position
 * 3) Execute a move
 */
public class Position {
	private static final String TAG = "cwfPosition";
	/**
	 * For numeric encoding of pieces:
	 * black pawn, e.g., is thus specified by the formula:
	 * BLACK * PIECES + PAWN
	 */
	public static final int WHITE = 0;
	public static final int BLACK = 1;
	// This order is used in pawn promotion menu
	public static final int QUEEN = 0;
	public static final int ROOK = 1;
	public static final int KNIGHT = 2;
	public static final int BISHOP = 3;
	public static final int PAWN = 4;
	public static final int KING = 5;
	public static final int PIECES = 6;
	public static final int NONE = -1;
	
	public static final int BOARD_SIZE = 8;
	public static final int SQUARES = BOARD_SIZE * BOARD_SIZE;
	
	// Note that moveList will be double the size of
	// the traditional move list, since white move + black move
	// is 2 items in the moveList
	private static final int MOVE_LIST_CAPACITY = 100;
	
	// Array of size 64 storing pieces
	private int[] pos;
	// Array of size 2 specifying whether white and black king have moved
	// Needed to determine whether castling is allowed
	private boolean[] kingHasMoved;
	private boolean[] queenRookHasMoved;
	private boolean[] kingRookHasMoved;
	private ArrayList<Move> moveList;
	private int hasMove;
	
	// Constructor for a new game
	public Position() {
		// Initialize board
		pos = new int[SQUARES];
		startingBoard(pos);
		// Initialize kingHasMoved
		kingHasMoved = new boolean[2];
		queenRookHasMoved = new boolean[2];
		kingRookHasMoved = new boolean[2];
		for (int i = 0; i < 2; ++i) {
			kingHasMoved[i] = false;
			queenRookHasMoved[i] = false;
			kingRookHasMoved[i] = false;
		}
		moveList = new ArrayList<Move>(MOVE_LIST_CAPACITY);
		hasMove = WHITE;
	}
	
	// For continuing a game
	public Position(ArrayList<Move> ml) {
		this();
		int len = ml.size();
		Move m;
		for (int i = 0; i < len; ++i) {
			m = ml.get(i);
			this.move(m.from(), m.to(), m.piece());
		}
	}
	
	/**
	 * Returns true iff hasMove color cannot move out of check
	 * @return
	 */
	public boolean checkmate() {
		if (!isInCheck(hasMove)) return false;		
		return !legalMoveExists();
	}
	
	public boolean stalemate() {
		if (isInCheck(hasMove)) return false;		
		return !legalMoveExists();
	}
	
	// Castling cannot possibly be the only legal move
	public boolean legalMoveExists() {
		LinkedList<Integer> moves;
		ListIterator<Integer> it;		
		for (int from = 0; from < SQUARES; ++from) {
			if (pos[from] != NONE && pos[from] / PIECES == hasMove) {
				moves = movesNoCastling(from);
				it = moves.listIterator();
				while (it.hasNext()) {
					if (!isInCheckAfterMove(hasMove, from, it.next(), pos[from])) return true;
				}
			}
		}		
		return false;
	}
	/**
	 * @param from
	 * @param to
	 * @return
	 */
	public boolean isMove(int from, int to) {
		// 'from' and 'to' must be legitimate squares
		if (from < 0 || SQUARES <= from) return false;
		if (to < 0 || SQUARES <= to) return false;
		// 'from' and 'to' cannot be the same
		if (from == to) return false;
		// A piece must occupy initial square
		if (pos[from] == NONE) return false;
		// Piece on initial square must have proper color
		if (pos[from] / PIECES != hasMove) return false;
		// Piece on target square cannot be same color
		if (pos[to] != NONE && pos[to] / PIECES == hasMove) return false;
		// King must not be in check after move
		// Piece moved shouldn't matter here
		if (isInCheckAfterMove(hasMove, from, to, pos[from])) return false;
		if (pos[from] == hasMove * PIECES + QUEEN) {
			return isQueenMove(from, to);
		}
		if (pos[from] == hasMove * PIECES + ROOK) {
			return isRookMove(from, to);
		}
		if (pos[from] == hasMove * PIECES + BISHOP) {
			return isBishopMove(from, to);
		}
		if (pos[from] == hasMove * PIECES + KING) {
			return isKingMove(from, to);
		}
		if (pos[from] == hasMove * PIECES + KNIGHT) {
			return isKnightMove(from, to);
		}
		if (pos[from] == hasMove * PIECES + PAWN) {
			return isPawnMove(from, to);
		}
		return false;
	}
	
	public int hasMove() {
		return hasMove;
	}
	public boolean whiteToMove() {
		return hasMove == WHITE;
	}
	/**
	 * Specifies the piece occupying the given square
	 * @param square
	 * @return
	 */
	public int getPiece(int square) {
		return pos[square];
	}
	
	/**
	 * Returns the code corresponding to the current draw status, e.g.
	 * DRAW_BY_REPETITION, etc. (cf. respective constants)
	 * @return
	 */
	public int drawStatus() {
		Log.d(TAG, "retrieving draw status");
		// TODO complete list of possible draws
		if (fiftyMoveDraw()) {
			return DRAW_BY_FIFTY_MOVE_RULE;
		}
		/*
		 * It is important that drawByRepetition() be the last option checked
		 * because it only activates the draw if it is called by the player
		 * who has the current move.
		 */
		else if (drawByRepetition()) {
			return DRAW_BY_REPETITION;
		}
		else {
			return NO_DRAW;
		}
	}
	
	/**
	 * Note that each "move" in moveList is only a half-move according
	 * to the rules of chess
	 */
	private boolean fiftyMoveDraw() {
		Log.d(TAG, "checking for 50 move draw");
		final int len = moveList.size();
		final int lastDrawingSeqStart = len - 100;
		if (lastDrawingSeqStart < 0) return false;
		int firstPossibleSeqStart = 0;
		
		int[] testBoard = new int[SQUARES];
		startingBoard(testBoard);
		for (int i = 0; i < len; ++i) {
			// Not a pawn move and not a capture
			if (testBoard[moveList.get(i).from()] % PIECES == PAWN || testBoard[moveList.get(i).to()] != NONE) {
				firstPossibleSeqStart = i + 1;
				if (firstPossibleSeqStart > lastDrawingSeqStart) return false;
			}			
			executeMove(testBoard, moveList.get(i).from(), moveList.get(i).to(),
					moveList.get(i).piece());
		}
		return true;
	}
	
	/**
	 * According to the rules, draw by repetition can only be declared on one's
	 * own move (handle that case in game). It occurs when:
	 * 1) Moving player could make a move that would create the same position
	 * a third time
	 * 2) The current position has occurred 3 times
	 * Change in castling status makes 2 positions not identical
	 * Change in ability to take e.p. makes 2 positions not identical
	 * Same player must have the move for 2 positions to be identical
	 * 
	 */
	private boolean drawByRepetition() {
		if (hasOccurredTwice(pos, hasMove)) return true;
		/**
		 * For the next part, we need to exclude from the list of available moves:
		 * 1) pawn moves
		 * 2) castling
		 * 3) taking an opponent's piece
		 */
		int i;
		int refHasMove = (hasMove + 1) % 2;
		LinkedList<Integer> toSquares;
		ListIterator<Integer> it;
		int to;
		// Make a copy of the current position
		int[] refPos = new int[SQUARES];
		for (i = 0; i < SQUARES; ++i) {
			refPos[i] = pos[i];
		}
		for (i = 0; i < SQUARES; ++i) {
			if (refPos[i] != NONE && refPos[i] / PIECES == hasMove
					&& refPos[i] % PIECES != PAWN) {
				toSquares = movesNoCastling(i);
				it = toSquares.listIterator();
				while (it.hasNext()) {
					to = it.next();
					// Not a taking move
					if (refPos[to] == NONE) {
						// Set and check new position
						refPos[to] = refPos[i];
						refPos[i] = NONE;
						if (hasOccurredTwice(refPos, refHasMove)) return true;
						// Reset refPos
						refPos[i] = refPos[to];
						refPos[to] = NONE;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Does not count the current position so as to apply to both variations
	 * of the draw by repetition rule. In other words, the position generated
	 * by the last move in moveList is not tested here.
	 * For castling right tests, we can use the state of the calling object
	 * because moves changing castling status will be excluded prior to entering this
	 * method
	 * @param refPos
	 * @param refHasMove
	 * @return
	 */
	private boolean hasOccurredTwice(int[] refPos, int refHasMove) {		
		int[] testPos = new int[SQUARES];
		
		startingBoard(testPos);
		int len = moveList.size();
		int startCheck = 0, i;
		
		// Determine where we need to start checking
		for (i = 0; i < len; ++i) {
			// No subsequent pawn move can have occurred
			if (testPos[moveList.get(i).from()] % PIECES == PAWN) {
				startCheck = i + 1;
			}
			// No piece has been taken in the interim
			else if (testPos[moveList.get(i).to()] != NONE) {
				startCheck = i + 1;
			}
			// Castling cannot have occurred
			else if (testPos[moveList.get(i).from()] % PIECES == KING) {
				// If someone is castling
				if (Math.abs(moveList.get(i).from() - moveList.get(i).to()) == 2) {
					startCheck = i + 1;
				}
			}
			executeMove(testPos, moveList.get(i).from(), moveList.get(i).to(),
					moveList.get(i).piece());
		}
		
		// Initialize everything we need for testing
		int testHasMove = WHITE;
		boolean[] testKingHasMoved = new boolean[2];
		boolean[] testKingRookHasMoved = new boolean[2];
		boolean[] testQueenRookHasMoved = new boolean[2];
		for (i = 0; i < 2; ++i) {
			testKingHasMoved[i] = false;
			testKingRookHasMoved[i] = false;
			testQueenRookHasMoved[i] = false;
		}		
		startingBoard(testPos);
		
		// Get to startCheck position
		for (i = 0; i < startCheck; ++i) {
			testHasMove = executeMove(testPos, moveList.get(i).from(), moveList.get(i).to(),
					moveList.get(i).piece(), testHasMove, testKingHasMoved, testKingRookHasMoved,
					testQueenRookHasMoved);
		}
		
		// If startCheck position allows e.p., take one more move before comparing positions
		if (allowsEnPassant(testPos, moveList.get(startCheck - 1).from(), moveList.get(startCheck - 1).to())) {
			testHasMove = executeMove(testPos, moveList.get(startCheck).from(), moveList.get(startCheck).to(),
					moveList.get(startCheck).piece(), testHasMove, testKingHasMoved, testKingRookHasMoved,
					testQueenRookHasMoved);
			startCheck++;
		}
		
		int occurrenceCount = 0;
		// We only check for identity up to penultimate move
		len--;
		
		for (i = startCheck; i < len; ++i) {
			if (testHasMove == refHasMove) {
				if (positionallyEqual(testPos, refPos)) {
					if (equalKingHasMovedArrays(testKingHasMoved, kingHasMoved)
							&& equalKingHasMovedArrays(testKingRookHasMoved, kingRookHasMoved)
							&& equalKingHasMovedArrays(testQueenRookHasMoved, queenRookHasMoved)) {
						++occurrenceCount;
						if (occurrenceCount >= 2) return true;
					}
				}
			}
			testHasMove = executeMove(testPos, moveList.get(i).from(), moveList.get(i).to(),
					moveList.get(i).piece(), testHasMove, testKingHasMoved, testKingRookHasMoved,
					testQueenRookHasMoved);
		}
		return false;
	}
	
	private static boolean equalKingHasMovedArrays(boolean[] khm1, boolean[] khm2) {
		for (int i = 0; i < 2; ++i) {
			if (khm1[i] != khm2[i]) return false;
		}
		return true;
	}
	
	private static boolean allowsEnPassant(int[] testPos, int priorFrom, int priorTo) {
		// Last piece moved was a pawn
		if (testPos[priorTo] % PIECES == PAWN) {
			// Pawn made a double advance
			if (Math.abs(priorTo - priorFrom) == BOARD_SIZE * 2) {
				int opposingColor = (testPos[priorTo] / PIECES + 1) % 2;
				// There is an opposing pawn adjacent to the pawn
				if (priorTo % BOARD_SIZE < BOARD_SIZE - 1) {
					if (testPos[priorTo + 1] == PAWN + opposingColor * PIECES) return true;
				}
				if (priorTo % BOARD_SIZE > 0) {
					if (testPos[priorTo - 1] == PAWN + opposingColor * PIECES) return true;
				}
			}
		}
		return false;
	}
	
	private static int executeMove(int[] testPos, int from, int to, int piece, int testHasMove,
			boolean[] testKingHasMoved, boolean[] testKingRookHasMoved, boolean[] testQueenRookHasMoved) {
		if (!testKingHasMoved[testHasMove] && from == origKingPos(testHasMove)) {
			testKingHasMoved[testHasMove] = true;
		}
		if (!testKingRookHasMoved[testHasMove] && from == origKingRookPos(testHasMove)) {
			testKingRookHasMoved[testHasMove] = true;
		}
		if (!testQueenRookHasMoved[testHasMove] && from == origQueenRookPos(testHasMove)) {
			testQueenRookHasMoved[testHasMove] = true;
		}
		executeMove(testPos, from, to, piece);
		return (testHasMove + 1) % 2;
	}
	
	private static int origKingPos(int color) {
		if (color == WHITE)	return 4;
		else return 60;
	}
	
	private static int origKingRookPos(int color) {
		if (color == WHITE) return 7;
		else return 63;
	}
	
	private static int origQueenRookPos(int color) {
		if (color == WHITE) return 0;
		else return 56;
	}
	
	private static boolean positionallyEqual(int[] pos1, int[] pos2) {
		for (int i = 0; i < SQUARES; ++i) {
			if (pos1[i] != pos2[i]) return false;
		}
		return true;
	}
	
	/**
	 * Wikipedia lists these as:
	 * 1) K vs. K
	 * 2) K + N vs. K
	 * 3) K + any number of bishops of color X vs. K + any number of bishops of color X 
	 * (all bishops must be of same color, but number doesn't matter)
	 
	 * A full implementation will be more complicated due to pawn blockades
	 * I think the cases are:
	 * 1) Insufficient material
	 * 2) Only pawns and king but both sides blockaded
	 * 3) Complete blockade with bishop that can't break through
	 * Blockade with queen: queen can always take an opponent's pawn
	 * Blockade with rook: rook can always be sacrificed allowing pawn break-through
	 */
	public boolean checkmateImpossible() {
		int bishopColor = -1;
		boolean knightFound = false;
		int row, col;
		for (int i = 0; i < SQUARES; ++i) {
			if (pos[i] % PIECES == ROOK || pos[i] % PIECES == QUEEN || pos[i] % PIECES == PAWN) {
				return false;
			}
			if (pos[i] % PIECES == KNIGHT) {
				if (knightFound) return false;
				if (bishopColor >= 0) return false;
				knightFound = true;
			}
			else if (pos[i] % PIECES == BISHOP) {
				if (knightFound) return false;
				row = i / SQUARES;
				col = i % SQUARES;
				if (bishopColor == -1) {
					bishopColor = (row + col) % 2;
				}
				else if (bishopColor != (row + col % 2)) return false;
			}
		}
		return true;
	}
	
	/**
	 * This method assumes that the piece on the 'from' square is a Q.
	 * This is for efficiency purposes to avoid checking twice but also
	 * means that the piece has to be checked outside of this method!!!
	 * @param from
	 * @param to
	 * @return
	 */
	private boolean isQueenMove(int from, int to) {
		if (isRookMove(from, to)) return true;
		return isBishopMove(from, to);
	}
	
	/**
	 * This method assumes that the piece on the 'from' square is a R.
	 * This is for efficiency purposes to avoid checking twice but also
	 * means that the piece has to be checked outside of this method!!!
	 * @param from
	 * @param to
	 * @return
	 */
	private boolean isRookMove(int from, int to) {
		int i, j;
		int fromRow = from / BOARD_SIZE;
		int toRow = to / BOARD_SIZE;
		int fromCol = from % BOARD_SIZE;
		int toCol = to % BOARD_SIZE;
		// 'from' and 'to' are on the same row
		if (fromRow == toRow) {
			if (toCol < fromCol) {
				// Move not blocked
				for (j = fromCol - 1; j > toCol; --j) {
					if (pos[fromRow * BOARD_SIZE + j] != NONE) return false;
				}
				return true;
			}
			if (fromCol < toCol) {
				// Move not blocked
				for (j = fromCol + 1; j < toCol; ++j) {
					if (pos[fromRow * BOARD_SIZE + j] != NONE) return false;
				}
				return true;
			}
		}
		// 'from' and 'to' are in the same column
		if (fromCol == toCol) {
			if (toRow < fromRow) {
				// Move not blocked
				for (i = fromRow - 1; i > toRow; --i) {
					if (pos[i * BOARD_SIZE + fromCol] != NONE) return false;
				}
				return true;
			}
			if (fromRow < toRow) {
				// Move not blocked
				for (i = fromRow + 1; i < toRow; ++i) {
					if (pos[i * BOARD_SIZE + fromCol] != NONE) return false;
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * This method assumes that the piece on the 'from' square is a B.
	 * This is for efficiency purposes to avoid checking twice but also
	 * means that the piece has to be checked outside of this method!!
	 * @param from
	 * @param to
	 * @return
	 */
	private boolean isBishopMove(int from, int to) {
		int i, j;
		int fromRow = from / BOARD_SIZE;
		int toRow = to / BOARD_SIZE;
		int fromCol = from % BOARD_SIZE;
		int toCol = to % BOARD_SIZE;
		// 'from' and 'to' are on the same / diagonal
		if (toRow - fromRow == toCol - fromCol) {
			if (toRow < fromRow) {
				// Move not blocked
				j = fromCol - 1;
				for (i = fromRow - 1; i > toRow; --i) {
					if (pos[i * BOARD_SIZE + j--] != NONE) return false;
				}
				return true;
			}
			if (fromRow < toRow) {
				// Move not blocked
				j = fromCol + 1;
				for (i = fromRow + 1; i < toRow; ++i) {
					if (pos[i * BOARD_SIZE + j++] != NONE) return false;
				}
				return true;
			}
		}
		// 'from' and 'to' are on the same \ diagonal
		if (toRow - fromRow == fromCol - toCol) {
			if (toRow < fromRow) {
				// Move not blocked
				j = fromCol + 1;
				for (i = fromRow - 1; i > toRow; --i) {
					if (pos[i * BOARD_SIZE + j++] != NONE) return false;
				}
				return true;
			}
			if (fromRow < toRow) {
				// Move not blocked
				j = fromCol - 1;
				for (i = fromRow + 1; i < toRow; ++i) {
					if (pos[i * BOARD_SIZE + j--] != NONE) return false;
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Does not check whether K would be in check after move
	 * since that is determined elsewhere in the method isMove().
	 * Does verify that castling is not allowed if K is in check, etc.
	 * @param colorToMove
	 * @param from
	 * @param to
	 * @return
	 */
	private boolean isKingMove(int from, int to) {
		int fromRow = from / BOARD_SIZE;
		int toRow = to / BOARD_SIZE;
		int rowDiff = toRow - fromRow;
		int fromCol = from % BOARD_SIZE;
		int toCol = to % BOARD_SIZE;
		int colDiff = toCol - fromCol;
		int castles = isCastlingMove(hasMove, from, to);
		
		// Validate non-castling moves
		if (castles == 0) {
			if (-1 <= rowDiff && rowDiff <= 1 && -1 <= colDiff && colDiff <= 1) {
				return true;
			}
		}
		// Validate castling moves
		else  {
			// Not allowed if K has moved
			if (kingHasMoved[hasMove]) {
				return false;
			}
			// Not allowed if K currently in check
			if (isInCheck(pos, hasMove)) {
				return false;
			}
			int i;
			// Validate king's side castling
			if (castles == 1) {
				// No blocking pieces
				for (i = 1; i <= 2; ++i) {
					if (pos[from + i] != NONE) return false;
				}
				// King rook hasn't moved
				if (kingRookHasMoved[hasMove]) return false;
				// King wouldn't be in check on square crossed
				if (isInCheckAfterMove(hasMove, from, from + 1, hasMove * PIECES + KING)) {
					return false;
				}
				return true;
			}
			// Validate queen's side castling
			if (castles == -1) {
				// No blocking pieces
				for (i = 1; i <= 3; ++i) {
					if (pos[from - i] != NONE) return false;
				}
				// Queen rook hasn't moved
				if (queenRookHasMoved[hasMove]) return false;
				// King wouldn't be in check on square crossed
				if (isInCheckAfterMove(hasMove, from, from - 1, hasMove * PIECES + KING)) {
					return false;
				}
				return true;
			}			
		}
		return false;
	}
	
	/**
	 * 
	 * @param colorToMove
	 * @param from
	 * @param to
	 * @return
	 */
	private boolean isKnightMove(int from, int to) {
		int fromRow = from / BOARD_SIZE;
		int toRow = to / BOARD_SIZE;
		int rowDiff = toRow - fromRow;
		int fromCol = from % BOARD_SIZE;
		int toCol = to % BOARD_SIZE;
		int colDiff = toCol - fromCol;
		
		if (rowDiff == 1 || rowDiff == -1) {
			if (colDiff == 2 || colDiff == -2) return true;
			return false;
		}
		if (rowDiff == 2 || rowDiff == -2) {
			if (colDiff == 1 || colDiff == -1) return true;
			return false;
		}
		return false;
	}
	
	private boolean isPawnMove(int from, int to) {
		int fromRow = from / BOARD_SIZE;
		int toRow = to / BOARD_SIZE;
		int rowDiff = toRow - fromRow;
		int fromCol = from % BOARD_SIZE;
		int toCol = to % BOARD_SIZE;
		int colDiff = toCol - fromCol;
		
		int direction = 1;
		int origRow = 1;
		if (hasMove == BLACK) {
			direction = -1;
			origRow = 6;
		}
		
		// No taking
		if (colDiff == 0) {
			if (pos[to] != NONE) return false;
			// Single step in proper direction
			if (rowDiff == direction) return true;
			// Double step
			if (fromRow == origRow && rowDiff == 2 * direction) {
				// No piece on traversed square
				if (pos[from + direction * BOARD_SIZE] != NONE) return false;
				return true;
			}
			return false;
		}
		// Taking (including e.p.)
		if (colDiff == 1 || colDiff == -1) {
			if (rowDiff != direction) return false;
			if (allowsEnPassant(to)) return true;
			if (pos[to] != NONE) return true;
			return false;
		}		
		return false;
	}
	
	/**
	 * Returns true iff a pawn can be taken e.p. on the given square.
	 * @param square
	 * @return
	 */
	private boolean allowsEnPassant(int square) {		
		int numMoves = moveList.size();
		if (numMoves == 0) return false;
		Move lastMove = moveList.get(numMoves - 1);
		
		if (lastMove.piece() % PIECES == PAWN) {			
			if (lastMove.to() - lastMove.from() == 2 * BOARD_SIZE &&
					square == lastMove.from() + BOARD_SIZE) {
				return true;
			}
			if (lastMove.from() - lastMove.to() == 2 * BOARD_SIZE &&
					square == lastMove.to() + BOARD_SIZE) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Does not verify whether piece moved is a king but only checks for
	 * appropriate squares.
	 * @param from
	 * @param to
	 * @return Returns 1 for king's side castle, -1 for queen's side castle
	 * and 0 if not a castling move at all
	 */
	private static int isCastlingMove(int colorToMove, int from, int to) {
		int fromRow = from / BOARD_SIZE;
		int toRow = to / BOARD_SIZE;
		int fromCol = from % BOARD_SIZE;
		int toCol = to % BOARD_SIZE;
		int origRow = 0;
		if (colorToMove == BLACK) origRow = 7;
		int origCol = 4;
		if (fromRow == origRow && fromCol == origCol &&	toRow == origRow) {
			if (toCol - fromCol == 2) return 1;	// king's side castling
			if (toCol - fromCol == -2) return -1;	// queen's side
		}
		return 0;
	}
	/**
	 * Specifies whether the king of the given color is in
	 * check following the given move (which may have been made by
	 * either player). So, this method will tell us whether black's
	 * move gives white check.
	 * And it will allow us to determine move validity by
	 * determining whether white's king would still be in check if 
	 * white were to make the given move
	 * @param kingColor
	 * @param from
	 * @param to
	 * @param piece Necessary because piece can change in case of pawn promotion
	 * @return
	 */
	public boolean isInCheckAfterMove(int kingColor, int from, int to, int piece) {
		int[] newPos = new int[SQUARES];
		
		// Start with the current position
		for (int i = 0; i < SQUARES; ++i) {
			newPos[i] = pos[i];
		}
		// Then make the specified move
		executeMove(newPos, from, to, piece);
		return isInCheck(newPos, kingColor);
	}
	
	public boolean isInCheck(int kingColor) {
		return isInCheck(pos, kingColor);
	}
	
	/**
	 * Determines whether the king of the given color is in check
	 * in the given position
	 * @param posit
	 * @param kingColor
	 * @return
	 */
	private static boolean isInCheck(int[] posit, int kingColor) {
		int i, j, row, col, startRow, stopRow, startCol, stopCol, tmp;
		final int OPPOSITE_COLOR = (kingColor + 1) % 2;
		final int OPP_KING = OPPOSITE_COLOR * PIECES + KING;
		final int OPP_QUEEN = OPPOSITE_COLOR * PIECES + QUEEN;
		final int OPP_BISHOP = OPPOSITE_COLOR * PIECES + BISHOP;
		final int OPP_KNIGHT = OPPOSITE_COLOR * PIECES + KNIGHT;
		final int OPP_ROOK = OPPOSITE_COLOR * PIECES + ROOK;
		final int OPP_PAWN = OPPOSITE_COLOR * PIECES + PAWN;
		
		// Find the king's position
		int kingPos = findPiece(posit, kingColor * PIECES + KING);
		row = kingPos / BOARD_SIZE;
		col = kingPos % BOARD_SIZE;		
		
		// Check adjacent squares for opposing king
		startRow = row - 1;
		stopRow = row + 1;
		startCol = col - 1;
		stopCol = col + 1;
		if (startRow < 0) startRow = 0;		
		if (BOARD_SIZE <= stopRow) stopRow = BOARD_SIZE -1;
		if (startCol < 0) startCol = 0;		
		if (BOARD_SIZE <= stopCol) stopCol = BOARD_SIZE -1;
		for (i = startRow; i <= stopRow; ++i) {
			for (j = startCol; j <= stopCol; ++j) {
				if (posit[i * BOARD_SIZE + j] == OPP_KING) return true;
			}
		}
		
		// Check appropriate squares for opposing pawns
		if (kingColor == WHITE && row < BOARD_SIZE - 2) {
			if (col > 0 && posit[(row + 1) * BOARD_SIZE + col - 1] == OPP_PAWN) return true;
			if (col < BOARD_SIZE - 1 && posit[(row + 1) * BOARD_SIZE + col + 1] == OPP_PAWN) return true;
		}
		if (kingColor == BLACK && row > 1) {
			if (col > 0 && posit[(row - 1) * BOARD_SIZE + col - 1] == OPP_PAWN) return true;
			if (col < BOARD_SIZE - 1 && posit[(row - 1) * BOARD_SIZE + col + 1] == OPP_PAWN) return true;
		}
		
		// Check backwards on row for Q or R
		for (i = col - 1; i >= 0; --i) {
			tmp = posit[row * BOARD_SIZE + i];
			if (tmp == OPP_QUEEN) return true;
			if (tmp == OPP_ROOK) return true;
			if (tmp != NONE) break;
		}
		// Check forwards on row for Q or R
		for (i = col + 1; i < BOARD_SIZE; ++i) {
			tmp = posit[row * BOARD_SIZE + i];
			if (tmp == OPP_QUEEN) return true;
			if (tmp == OPP_ROOK) return true;
			if (tmp != NONE) break;
		}
		// Check downwards on col for Q or R
		for (i = row - 1; i >= 0; --i) {
			tmp = posit[i * BOARD_SIZE + col];
			if (tmp == OPP_QUEEN) return true;
			if (tmp == OPP_ROOK) return true;
			if (tmp != NONE) break;
		}
		// Check upwards on col for Q or R
		for (i = row + 1; i < BOARD_SIZE; ++i) {
			tmp = posit[i * BOARD_SIZE + col];
			if (tmp == OPP_QUEEN) return true;
			if (tmp == OPP_ROOK) return true;
			if (tmp != NONE) break;
		}
		
		// Check lower left diagonal for Q or B
		j = col - 1;
		for (i = row - 1; i >= 0; --i) {
			if (j < 0) break;
			tmp = posit[i * BOARD_SIZE + j--];
			if (tmp == OPP_QUEEN) return true;
			if (tmp == OPP_BISHOP) return true;
			if (tmp != NONE) break;			
		}
		// Check upper left diagonal for Q or B
		j = col - 1;
		for (i = row + 1; i < BOARD_SIZE; ++i) {
			if (j < 0) break;
			tmp = posit[i * BOARD_SIZE + j--];
			if (tmp == OPP_QUEEN) return true;
			if (tmp == OPP_BISHOP) return true;
			if (tmp != NONE) break;
		}
		// Check upper right diagonal for Q or B
		j = col + 1;
		for (i = row + 1; i < BOARD_SIZE; ++i) {
			if (BOARD_SIZE <= j) break;			
			tmp = posit[i * BOARD_SIZE + j++];
			if (tmp == OPP_QUEEN) return true;
			if (tmp == OPP_BISHOP) return true;
			if (tmp != NONE) break;			
		}
		// Check lower right diagonal for Q or B
		j = col + 1;
		for (i = row - 1; i >= 0; --i) {
			if (BOARD_SIZE <= j) break;	
			tmp = posit[i * BOARD_SIZE + j++];
			if (tmp == OPP_QUEEN) return true;
			if (tmp == OPP_BISHOP) return true;
			if (tmp != NONE) break;
		}
		
		// Check for knight checks
		if (row - 2 >= 0) {
			if (col - 1 >= 0 && posit[(row - 2) * BOARD_SIZE + col - 1] == OPP_KNIGHT) {
				return true;
			}
			if (col + 1 < BOARD_SIZE && posit[(row - 2) * BOARD_SIZE + col + 1] == OPP_KNIGHT) {
				return true;
			}
		}
		if (row - 1 >= 0) {
			if (col - 2 >= 0 && posit[(row - 1) * BOARD_SIZE + col - 2] == OPP_KNIGHT) {
				return true;
			}
			if (col + 2 < BOARD_SIZE && posit[(row - 1) * BOARD_SIZE + col + 2] == OPP_KNIGHT) {
				return true;
			}
		}
		if (row + 1 < BOARD_SIZE) {
			if (col - 2 >= 0 && posit[(row + 1) * BOARD_SIZE + col - 2] == OPP_KNIGHT) {
				return true;
			}
			if (col + 2 < BOARD_SIZE && posit[(row + 1) * BOARD_SIZE + col + 2] == OPP_KNIGHT) {
				return true;
			}
		}
		if (row + 2 < BOARD_SIZE) {
			if (col - 1 >= 0 && posit[(row + 2) * BOARD_SIZE + col - 1] == OPP_KNIGHT) {
				return true;
			}
			if (col + 1 < BOARD_SIZE && posit[(row + 2) * BOARD_SIZE + col + 1] == OPP_KNIGHT) {
				return true;
			}
		}
		return false;
	}
	/**
	 * Returns the location of the given piece
	 * @param posit Position
	 * @param piece Piece
	 * @return
	 */
	private static int findPiece(int[] posit, int piece) {
		for (int i = 0; i < SQUARES; ++i) {
			if (posit[i] == piece) return i;
		}
		return -1;
	}
	
	/**
	 * Unsafe: Assumes that move is valid.
	 * Changes current position accordingly.
	 * @param from
	 * @param to
	 * @param piece
	 */
	protected void move(int from, int to, int piece) {
		// Add move to move list
		moveList.add(new Move(piece, from, to));
		// Adjust castling possibilities as necessary
		if (!kingHasMoved[hasMove] && piece == hasMove * PIECES + KING) {
			kingHasMoved[hasMove] = true;
		}
		if (!queenRookHasMoved[hasMove] && from == hasMove * 7 * BOARD_SIZE) {
			queenRookHasMoved[hasMove] = true;
		}
		if (!kingRookHasMoved[hasMove] && from == hasMove * 7 * BOARD_SIZE + 7) {
			kingRookHasMoved[hasMove] = true;
		}
		// Modify position
		executeMove(pos, from, to, piece);
		// Change hasMove
		hasMove = (hasMove + 1) % 2;
	}
	/**
	 * Completely unsafe method: Assumes that move has been
	 * validated as a legal move.
	 * @param posit
	 * @param from
	 * @param to
	 * @param piece
	 */
	private static void executeMove(int[] posit, int from, int to, int piece) {
		if (posit[from] % PIECES != KING) {
			// Take care of en passant
			if (posit[from] % PIECES == PAWN && posit[to] == NONE) {
				int fromCol = from % BOARD_SIZE;
				int toCol = to % BOARD_SIZE;
				if (fromCol != toCol) {
					// Has to be e.p. if we get here
					// So remove appropriate pawn
					if (piece / PIECES == BLACK) {
						posit[to + BOARD_SIZE] = NONE;
					}
					else {
						posit[to - BOARD_SIZE] = NONE;
					}
				}
			}
			posit[to] = piece;
			posit[from] = NONE;
		}
		else {	// piece is a K
			int colorToMove = posit[from] / PIECES;
			int castles = isCastlingMove(colorToMove, from, to);
			if (castles == 0) {	// Not castling
				posit[to] = piece;
				posit[from] = NONE;
			}
			else if (castles == 1) {	// king's side
				// Move the king
				posit[to] = piece;
				posit[from] = NONE;
				// Move the rook
				posit[to - 1] = colorToMove * PIECES + ROOK;
				posit[to + 1] = NONE;
			}
			else {	// queen's side
				// Move the king
				posit[to] = piece;
				posit[from] = NONE;
				// Move the rook
				posit[to + 1] = colorToMove * PIECES + ROOK;
				posit[to - 2] = NONE;
			}
		}
	}
	
	/**
	 * Will throw ArrayIndexOutOfBoundsException if posArr is not proper size
	 * @param posArr
	 * @param piece
	 * @param row
	 * @param col
	 */
	private static void setPiece(int[] posArr, int piece, int row, int col) {
		posArr[row * BOARD_SIZE + col] = piece;
	}
	
	/**
	 * Sets up posArr as starting board.
	 * Assumes that posArr has proper size.
	 * @param posArr
	 * @throws ArrayIndexOutOfBoundsException if posArr doesn't have proper size
	 */
	private static void startingBoard(int[] posArr) throws ArrayIndexOutOfBoundsException {
		int i;
		for (i = 0; i < SQUARES; ++i ) {
			posArr[i] = NONE;
		}
		// Set pawns
		for (i = 0; i < BOARD_SIZE; ++i) {
			// White
			setPiece(posArr, WHITE * PIECES + PAWN, 1, i);
			// Black
			setPiece(posArr, BLACK * PIECES + PAWN, 6, i);
		}
		// Rooks
		setPiece(posArr, WHITE * PIECES + ROOK, 0, 0);
		setPiece(posArr, WHITE * PIECES + ROOK, 0, 7);
		setPiece(posArr, BLACK * PIECES + ROOK, 7, 0);
		setPiece(posArr, BLACK * PIECES + ROOK, 7, 7);
		// Knights
		setPiece(posArr, WHITE * PIECES + KNIGHT, 0, 1);
		setPiece(posArr, WHITE * PIECES + KNIGHT, 0, 6);
		setPiece(posArr, BLACK * PIECES + KNIGHT, 7, 1);
		setPiece(posArr, BLACK * PIECES + KNIGHT, 7, 6);
		// Bishops
		setPiece(posArr, WHITE * PIECES + BISHOP, 0, 2);
		setPiece(posArr, WHITE * PIECES + BISHOP, 0, 5);
		setPiece(posArr, BLACK * PIECES + BISHOP, 7, 2);
		setPiece(posArr, BLACK * PIECES + BISHOP, 7, 5);
		// Queens
		setPiece(posArr, WHITE * PIECES + QUEEN, 0, 3);
		setPiece(posArr, BLACK * PIECES + QUEEN, 7, 3);
		// Kings
		setPiece(posArr, WHITE * PIECES + KING, 0, 4);
		setPiece(posArr, BLACK * PIECES + KING, 7, 4);
	}
	
	/**
	 * Returns a linked list containing all possible pawn moves
	 * from the input square except that this method does
	 * not determine whether the move would leave the hasMove
	 * king in check. That will be determined elsewhere.
	 * Does not verify the piece actually
	 * occupying the input square. Also presupposes that hasMove
	 * is the color that will move.
	 * Used in checkmate() method
	 * @param from
	 * @return
	 */
	private LinkedList<Integer> pawnMoves(int from) {
		LinkedList<Integer> moves = new LinkedList<Integer>();
		int fromRow = from / BOARD_SIZE;		
		int fromCol = from % BOARD_SIZE;
		
		int direction = 1;
		int origRow = 1;
		if (hasMove == BLACK) {
			direction = -1;
			origRow = 6;
		}
		int tmp;
		
		// No taking
		if (pos[from + direction * BOARD_SIZE] == NONE) {
			moves.add(from + direction * BOARD_SIZE);			
			if (fromRow == origRow) {
				tmp = from + 2 * direction * BOARD_SIZE;
				if (pos[tmp] == NONE) {
					moves.add(tmp);
				}				
			}
		}
		// Taking (including e.p.)
		if (fromCol > 0) {		// Toward the "minus" side
			tmp = from + direction * BOARD_SIZE - 1;
			if (pos[tmp] != NONE && 
					pos[tmp] / PIECES != hasMove) {
				moves.add(tmp);
			}
			else if (allowsEnPassant(tmp)) moves.add(tmp);
		}		
		if (fromCol < BOARD_SIZE - 1) {
			tmp = from + direction * BOARD_SIZE + 1;
			if (pos[tmp] != NONE && 
					pos[tmp] / PIECES != hasMove) {
				moves.add(tmp);
			}
			else if (allowsEnPassant(tmp)) moves.add(tmp);
		}
		return moves;
	}
	
	private LinkedList<Integer> kingMovesNoCastling(int from) {
		LinkedList<Integer> moves = new LinkedList<Integer>();
		int fromRow = from / BOARD_SIZE;
		int fromCol = from % BOARD_SIZE;
		int i, j, tmp;
		for (i = -1; i <= 1; ++i) {
			for (j = -1; j <= 1; ++j) {
				if (i != 0 || j != 0) {
					if (0 <= fromRow + i && fromRow + i < BOARD_SIZE) {
						if (0 <= fromCol + j && fromCol + j < BOARD_SIZE) {
							tmp = from + j + i * BOARD_SIZE;
							if (pos[tmp] == NONE || pos[tmp] / PIECES != hasMove) {
								moves.add(tmp);
							}
						}
					}
				}
			}
		}
		return moves;
	}
	
	/**
	 * Does not include castling
	 * @param from
	 * @return
	 */
	private LinkedList<Integer> rookMovesNoCastling(int from) {
		LinkedList<Integer> moves = new LinkedList<Integer>();
		int fromRow = from / BOARD_SIZE;
		int fromCol = from % BOARD_SIZE;
		int i, tmp;
		
		// Moving in negative direction in row
		i = -1;
		while (fromCol + i >= 0) {
			tmp = from + i;
			if (pos[tmp] == NONE) moves.add(tmp);
			else if (pos[tmp] / PIECES != hasMove) {	// Different color piece
				moves.add(tmp);
				break;
			}
			else break;		// Same color piece
			--i;
		}
		// Moving in positive direction in row
		i = 1;
		while (fromCol + i < BOARD_SIZE) {
			tmp = from + i;
			if (pos[tmp] == NONE) moves.add(tmp);
			else if (pos[tmp] / PIECES != hasMove) {	// Different color piece
				moves.add(tmp);
				break;
			}
			else break;		// Same color piece
			++i;
		}
		// Negative direction in column
		i = -1;
		while (fromRow + i >= 0) {
			tmp = from + i * BOARD_SIZE;
			if (pos[tmp] == NONE) moves.add(tmp);
			else if (pos[tmp] / PIECES != hasMove) {	// Different color piece
				moves.add(tmp);
				break;
			}
			else break;		// Same color piece
			--i;
		}
		// Positive direction in column
		i = 1;
		while (fromRow + i < BOARD_SIZE) {
			tmp = from + i * BOARD_SIZE;
			if (pos[tmp] == NONE) moves.add(tmp);
			else if (pos[tmp] / PIECES != hasMove) {	// Different color piece
				moves.add(tmp);
				break;
			}
			else break;		// Same color piece
			++i;
		}
		return moves;
	}
	
	private LinkedList<Integer> bishopMoves(int from) {
		LinkedList<Integer> moves = new LinkedList<Integer>();
		int fromRow = from / BOARD_SIZE;
		int fromCol = from % BOARD_SIZE;
		int i, tmp;
		
		// Moving in negative direction along / diagonal
		i = -1;
		while (fromCol + i >= 0 && fromRow + i >= 0) {
			tmp = from + i + i * BOARD_SIZE;
			if (pos[tmp] == NONE) moves.add(tmp);
			else if (pos[tmp] / PIECES != hasMove) {	// Different color piece
				moves.add(tmp);
				break;
			}
			else break;		// Same color piece
			--i;
		}
		// Moving in positive direction along / diagonal
		i = 1;
		while (fromCol + i < BOARD_SIZE && fromRow + i < BOARD_SIZE) {
			tmp = from + i + i * BOARD_SIZE;
			if (pos[tmp] == NONE) moves.add(tmp);
			else if (pos[tmp] / PIECES != hasMove) {	// Different color piece
				moves.add(tmp);
				break;
			}
			else break;		// Same color piece
			++i;
		}
		// Down and right along \ diagonal
		i = 1;
		while (fromRow - i >= 0 && fromCol + i < BOARD_SIZE) {
			tmp = from  + i - i * BOARD_SIZE;
			if (pos[tmp] == NONE) moves.add(tmp);
			else if (pos[tmp] / PIECES != hasMove) {	// Different color piece
				moves.add(tmp);
				break;
			}
			else break;		// Same color piece
			++i;
		}
		// Up and left along \ diagonal
		i = 1;
		while (fromRow + i < BOARD_SIZE && fromCol - i >= 0) {
			tmp = from - i + i * BOARD_SIZE;
			if (pos[tmp] == NONE) moves.add(tmp);
			else if (pos[tmp] / PIECES != hasMove) {	// Different color piece
				moves.add(tmp);
				break;
			}
			else break;		// Same color piece
			++i;
		}
		return moves;
	}
	
	private LinkedList<Integer> knightMoves(int from) {
		LinkedList<Integer> moves = new LinkedList<Integer>();
		int fromRow = from / BOARD_SIZE;
		int fromCol = from % BOARD_SIZE;
		int tmp;
		
		if (fromRow - 2 >= 0) {
			if (fromCol - 1 >= 0) {
				tmp = from - 1 - 2 * BOARD_SIZE;
				if (pos[tmp] == NONE) moves.add(tmp);
				else if (pos[tmp] / PIECES != hasMove) moves.add(tmp);
			}
			if (fromCol + 1 < BOARD_SIZE) {
				tmp = from + 1 - 2 * BOARD_SIZE;
				if (pos[tmp] == NONE) moves.add(tmp);
				else if (pos[tmp] / PIECES != hasMove) moves.add(tmp);
			}
		}
		if (fromRow - 1 >= 0) {
			if (fromCol - 2 >= 0) {
				tmp = from - 2 - BOARD_SIZE;
				if (pos[tmp] == NONE) moves.add(tmp);
				else if (pos[tmp] / PIECES != hasMove) moves.add(tmp);
			}
			if (fromCol + 2 < BOARD_SIZE) {
				tmp = from + 2 - BOARD_SIZE;
				if (pos[tmp] == NONE) moves.add(tmp);
				else if (pos[tmp] / PIECES != hasMove) moves.add(tmp);
			}
		}
		if (fromRow + 1 < BOARD_SIZE) {
			if (fromCol - 2 >= 0) {
				tmp = from - 2 + BOARD_SIZE;
				if (pos[tmp] == NONE) moves.add(tmp);
				else if (pos[tmp] / PIECES != hasMove) moves.add(tmp);
			}
			if (fromCol + 2 < BOARD_SIZE) {
				tmp = from + 2 + BOARD_SIZE;
				if (pos[tmp] == NONE) moves.add(tmp);
				else if (pos[tmp] / PIECES != hasMove) moves.add(tmp);
			}
		}
		if (fromRow + 2 < BOARD_SIZE) {
			if (fromCol - 1 >= 0) {
				tmp = from - 1 + 2 * BOARD_SIZE;
				if (pos[tmp] == NONE) moves.add(tmp);
				else if (pos[tmp] / PIECES != hasMove) moves.add(tmp);
			}
			if (fromCol + 1 < BOARD_SIZE) {
				tmp = from + 1 + 2 * BOARD_SIZE;
				if (pos[tmp] == NONE) moves.add(tmp);
				else if (pos[tmp] / PIECES != hasMove) moves.add(tmp);
			}
		}		
		return moves;
	}
	
	/**
	 * List of all possible moves for the piece on the from square.
	 * The move list includes moves leaving the king in check.
	 * It filters out all moves violating other rules and does
	 * not include castling moves.
	 * This method is used for determining checkmate.
	 * @param from
	 * @return
	 */
	private LinkedList<Integer> movesNoCastling(int from) {
		switch (pos[from] % PIECES) {
		case PAWN:
			return pawnMoves(from);
		case ROOK:
			return rookMovesNoCastling(from);
		case KNIGHT:
			return knightMoves(from);
		case BISHOP:
			return bishopMoves(from);
		case QUEEN:
			LinkedList<Integer> moves = rookMovesNoCastling(from);
			moves.addAll(bishopMoves(from));
			return moves;
		case KING:
			return kingMovesNoCastling(from);		
		}
		return new LinkedList<Integer>();
	}
}
