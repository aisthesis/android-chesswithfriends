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
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.telephony.TelephonyManager;

/**
 * @author Marshall Farrier
 * @version 0.1 3/5/11
 * Cf. Burnette, p. 124
 */
public class Register extends Activity {
	private static final String TAG = "cwhRegister";
	private ChessDataBaseAdapter db;
	private EditText handleText;
	private Button selectButton;
	private String device;
	private int selfId;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        
        db = new ChessDataBaseAdapter(this);
        device = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE))
        	.getDeviceId().trim() + "01234567890123456789";
        device = device.substring(0, 20);
        selfId = -1;
        Log.d(TAG, "device id is " + device);
        handleText = (EditText) findViewById(R.id.username_text);
        selectButton = (Button) findViewById(R.id.select_handle_button);
        
        selectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				register();				
			}        	
        });
	}
	
	private void register() {
		String handle = handleText.getText().toString().trim();
		if (handle.length() < 6) {
			showMessage("User name must have at least 6 characters.");
			handleText.setText("");			
			return;
		}
		if (handle.length() > 20) {
			showMessage("User name can have no more than 20 characters.");
			handleText.setText("");
			return;
		}
		if (!handle.matches("[a-zA-Z_0-9]*")) {
			showMessage("Invalid user name.");
			handleText.setText("");
			return;
		}
		if (!registerWithServer(handle)) {
			handleText.setText("");
		}
		else {
			db.open();
			db.enterRegistrationData(selfId, handle, device);
			db.close();
			finish();
		}
	}
	
	private void showMessage(String msg) {
		Toast t = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}
	
	private boolean registerWithServer(String handle) {
		String response = "";
		try {
			URL url = new URL(WEBSITE + DIRECTORY + REGISTRATION_SCRIPT);
			URLConnection conn = url.openConnection();
			
			// Send message
			conn.setDoOutput(true);			
			PrintWriter out = new PrintWriter(conn.getOutputStream());
			out.print("handle=" + URLEncoder.encode(handle, "UTF-8") + 
					"&device=" + URLEncoder.encode(device, "UTF-8"));
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
			return false;
		}
		if (response.equals("insertion_failure")) {
			showMessage("User name \"" + handle + "\" is already in use. Please choose another handle!");
			return false;
		}
		showMessage("Congratulations! You are now registered with the user name \"" + handle + "\".");
		selfId = Integer.parseInt(response);
		return true;
	}
}
