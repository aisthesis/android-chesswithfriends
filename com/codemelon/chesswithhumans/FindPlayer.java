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
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * @author Marshall Farrier
 * @version 0.1 3/6/11
 */
public class FindPlayer extends Activity {
	private static final String TAG = "cwhFindPlayer";
	private ChessDataBaseAdapter db;
	private EditText handleText;
	private Button searchButton;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.find_player);
        
        handleText = (EditText) findViewById(R.id.opponent_name_for_search);
        searchButton = (Button) findViewById(R.id.search_for_player_button);
        db = new ChessDataBaseAdapter(this);
        
        searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				find();			
			}        	
        });
	}
	
	private void find() {
		String handle = handleText.getText().toString().trim();
		// Validate handle
		if (handle.length() < 6 || 20 < handle.length() || !handle.matches("[a-zA-Z_0-9]*")) {
			showMessage("Invalid user name!");
			handleText.setText("");
			return;
		}
		
		// Check local database
		db.open();
		if (db.findPlayer(handle) != -1) {
			showMessage("Player \"" + handle + "\" is already in your player list.");
			handleText.setText("");
			handle = "";
		}
		db.close();		
		if (handle.length() == 0) return;		
		
		// Check server
		checkServer(handle);		
	}
	
	private void showMessage(String msg) {
		Toast t = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}
	
	/** 
	 * Server sends back playerId if found, the string 'failure' on problems with query,
	 * or the string 'empty' if the query was successful but no matching player was found.
	 */
	private void checkServer(String handle) {
		String response = "";
		try {
			URL url = new URL(WEBSITE + DIRECTORY + FIND_PLAYER_SCRIPT);
			URLConnection conn = url.openConnection();
			
			// Send message
			conn.setDoOutput(true);			
			PrintWriter out = new PrintWriter(conn.getOutputStream());
			out.print("handle=" + URLEncoder.encode(handle, "UTF-8"));
			out.close();
			
			// Retrieve and display server message
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			try {
				if (reader.ready()) {
					response = reader.readLine().trim();
					Log.d(TAG, "server response: " + response);
				}
			}
			finally {
				reader.close();
			}
		}
		catch (MalformedURLException e) {
			Log.e(TAG, e.getMessage());
		}
		catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
		
		if (response.length() == 0) {
			showMessage("Could not connect to server. Please try again later!");
			handleText.setText("");
			return;
		}
		if (response.equals("failure")) {
			showMessage("Error querying database. Please try again later!");
			finish();
			return;
		}
		if (response.equals("empty")) {
			showMessage("Player \"" + handle + "\" not found!");
			handleText.setText("");
			return;
		}
		int playerId = Integer.parseInt(response);
		handleText.setText("");
		db.open();
		if (db.addPlayer(playerId, handle)) {
			showMessage("Player \"" + handle + "\" has been added to your player list.");
		}
		else {
			showMessage("Player \"" + handle + "\" could not be added to local database.");
			Log.e(TAG, "Error adding player " + handle + " to local database");
		}
		db.close();					
	}
}
