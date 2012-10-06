/**
 * Provides static methods for working with challenges
 */
package com.codemelon.chesswithhumans;

import java.util.Arrays;
import java.util.Comparator;

import android.util.Log;

/**
 * @author Marshall Farrier
 * @version 0.1 3/10/11
 */
public class ChallengeHelper {
	private static final String TAG = "cwhChallengeHelper";
	private static Comparator<Challenge> bySourceHandle = new Comparator<Challenge>() {
		@Override
		public int compare(Challenge object1, Challenge object2) {
			return object1.sourceHandle().compareToIgnoreCase(object2.sourceHandle());
		}		
	};
	private ChallengeHelper() {}
	
	/**
	 * Converts an array of Strings with comma-separated challenge data
	 * (as returned from server via PHP script)
	 * into an array of challenges sorted by handle of challenger
	 * @param csvList
	 * @param defaultStatus
	 * @return
	 */
	public static Challenge[] parseChallengeList(String[] csvList, int defaultStatus) {
		int len = csvList.length;
		Log.d(TAG, len + " challenge(s) found");
		Challenge[] result = new Challenge[len];
		String[] tmp;
		
		for (int i = 0; i < len; ++i) {
			tmp = csvList[i].split(",");
			result[i] = new Challenge(Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]), tmp[2],
					Integer.parseInt(tmp[3]), defaultStatus);
		}
		// TODO eliminate duplicate challenges (show only targeted challenge when both are present)
		Arrays.sort(result, bySourceHandle);
		return result;
	}
}
