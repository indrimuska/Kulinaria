package com.indrimuska.kulinaria;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;

public class ArrayCursorAdapter {
	private ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
	
	public ArrayCursorAdapter(Context context, Cursor cursor, String[] keys) {
		while (cursor.moveToNext()) {
			Map<String, Object> element = new HashMap<String, Object>();
			for (String key : keys) element.put(key, cursor.getString(cursor.getColumnIndex(key)));
			list.add(element);
		}
	}
	
	public ArrayList<Map<String, Object>> getList() {
		return list;
	}
}
