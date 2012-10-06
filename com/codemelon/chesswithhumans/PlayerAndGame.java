/**
 * Copyright (c) 2011 Marshall Farrier 
 */
package com.codemelon.chesswithhumans;
/**
 * @author Marshall Farrier
 * @version 0.1 2/4/11
 * Simple class for creating an immutable object as tag
 * that can hold a player id and a game id
 */
public class PlayerAndGame {
	private int playerId;
	private int gameId;
	
	public PlayerAndGame(int pid, int gid) {
		playerId = pid;
		gameId = gid;
	}
	
	public int playerId() {
		return playerId;
	}
	
	public int gameId() {
		return gameId;
	}
}
