/**
 * @file
 * Copyright (c) 2011 Marshall Farrier
 */
package com.codemelon.chesswithhumans;

//import com.codemelon.chesswithfriends.R;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;

/**
 * @author Marshall Farrier
 * @version 0.1 2/19/11
 * 
 */
public class ContactSelector extends Activity {
	private static final String TAG = "cwfContactSelector";
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_list_view);
        
        Intent i = getIntent();
        // Changed from paad, so cf. book if this doesn't work:
        
        final Uri data = i.getData();
        String[] projection = {ContactsContract.Contacts.DISPLAY_NAME};
        final Cursor c =  getContentResolver().query(data, projection, null, null, null);
        
        String[] from = {ContactsContract.Contacts.DISPLAY_NAME};
        int[] to = {R.id.contact_name};
        
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, 
        		R.layout.contact_item_text_view, c, from, to);
        
        ListView lv = (ListView) findViewById(R.id.view_contacts);
        lv.setAdapter(adapter);
        
        lv.setOnItemClickListener(new OnItemClickListener() {
        	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        		// Move the cursor to the selected item
        		c.moveToPosition(pos);
        		// Extract the row id
        		int rowId = c.getInt(c.getColumnIndexOrThrow("_id"));
        		// Construct the result URI
        		Uri outURI = Uri.parse(data.toString() + rowId);
        		Intent outData = new Intent();
        		outData.setData(outURI);
        		setResult(Activity.RESULT_OK, outData);
        		finish();
        	}
        });
    }
}
