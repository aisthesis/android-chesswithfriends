/**
 * @file
 */
package com.codemelon.chesswithhumans;

/**
 * Copyright (c) 2011 Marshall Farrier
 * @author Marshall Farrier
 * @version 0.1 1/20/11
 * Immutables objects representing chess moves
 * Used for ChessWithFriends app
 * In case of pawn promotion, the piece should be listed as the
 * piece that the pawn becomes.
 */
public class Move {
	private int piece;
	private int from;
	private int to;
	
	/**	 * 
	 * @param p Int value of piece to place on target square
	 * @param f Int value of source square
	 * @param t Int value of target square
	 */
	public Move(int p, int f, int t) {
		piece = p;
		from = f;
		to = t;
	}
	
	public int piece() { return piece;}
	public int from() { return from;}
	public int to() { return to;}
}
