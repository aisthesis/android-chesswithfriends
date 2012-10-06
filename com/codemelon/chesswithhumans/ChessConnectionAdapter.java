/**
 * @file
 */
package com.codemelon.chesswithhumans;

import static com.codemelon.chesswithhumans.Constants.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;

import android.util.Log;

/**
 * @author Marshall Farrier
 * @version 0.1 3/8/11
 * Utility class for connecting to PHP scripts
 */
public class ChessConnectionAdapter {
	private static final String TAG = "cwfConnectionAdapter";
	private static final String WEBSITE = "http://codemelon.com/";
	private static final String DIRECTORY = "script/android/chess_with_humans/";
	
	private ChessConnectionAdapter() {}
	
	public static LinkedList<String> getPhpPostResponse(String scriptName, String postContent) {
		LinkedList<String> response = new LinkedList<String>();
		String line = "";
		try {
			URL url = new URL(WEBSITE + DIRECTORY + scriptName);
			URLConnection conn = url.openConnection();
			conn.setConnectTimeout(CHESS_CONNECT_TIMEOUT);
			
			// Send request
			conn.setDoOutput(true);			
			PrintWriter out = new PrintWriter(conn.getOutputStream());
			out.print(postContent);
			out.close();
			
			// Retrieve server response
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			try {
				while (reader.ready()) {
					line = reader.readLine().trim();
					if (line.length() > 0) {
						response.add(line);
						Log.d(TAG, "server response line received");
					}
				}
			}
			finally {
				reader.close();
			}
		}
		catch (MalformedURLException e) {
			Log.e(TAG, e.getMessage());
		}
		catch (SocketTimeoutException e) {
			Log.e(TAG, e.getMessage());
		}
		catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
		return response;
	}
}
