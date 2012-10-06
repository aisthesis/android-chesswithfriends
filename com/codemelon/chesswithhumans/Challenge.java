/**
 * @file
 */
package com.codemelon.chesswithhumans;

/**
 * @author Marshall Farrier
 * @version 0.1 3/10/11
 * Utility class for working with challenges
 * Challenge objects are immutable
 */
public class Challenge {
	private int challengeId;
	private int sourceId;
	private String sourceHandle;
	private int targetId;
	private int status;
	
	public Challenge(int challId, int srcId, String srcHandle, int targId, int stat) {
		challengeId = challId;
		sourceId = srcId;
		sourceHandle = srcHandle;
		targetId = targId;
		status = stat;
	}
	
	public int challengeId() { return challengeId; }
	public int sourceId() { return sourceId; }
	public String sourceHandle() { return sourceHandle; }
	public int targetId() { return targetId; }
	public int status() { return status; }	
}
