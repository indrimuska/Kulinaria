package com.indrimuska.kulinaria;

import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;

public class GroupedListAdapter extends BaseAdapter {
	Map<String, Adapter> sections = new LinkedHashMap<String, Adapter>();
	ArrayAdapter<String> headers;
	
	static final int HEADER_VIEW_TYPE = 0;
	
	GroupedListAdapter(Context context, int header) {
		headers = new ArrayAdapter<String>(context, header);
	}
	
	public void addSection(String header, Adapter child) {
		headers.add(header);
		sections.put(header, child);
	}
	
	@Override
	public int getCount() {
		int elements = 0;
		for (Adapter section : sections.values()) elements += section.getCount() + 1;
		return elements;
	}
	
	@Override
	public int getViewTypeCount() {
		int items = 1;
		for (Adapter adapter : sections.values()) items += adapter.getViewTypeCount();
		return items;
	}
	
	@Override
	public Object getItem(int position) {
		for (Adapter section : sections.values()) {
			int size = section.getCount() + 1;
			// check if position inside this section
			if (position == 0) return section;
			if (position < size) return section.getItem(position - 1);
			// otherwise jump into next section
			position -= size;
		}
		return null;
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		int section_number = 0;
		for (Adapter section : sections.values()) {
			int size = section.getCount() + 1;
			// check if position inside this section
			if (position == 0) return headers.getView(section_number, convertView, parent);
			if (position < size) return section.getView(position - 1, convertView, parent);
			// otherwise jump into next section
			position -= size;
			section_number++;
		}
		return null;
	}
	
	@Override
	public int getItemViewType(int position) {
		int type = 1;
		for (Adapter section : sections.values()) {
			int size = section.getCount() + 1;
			// check if position inside this section
			if (position == 0) return HEADER_VIEW_TYPE;
			if (position < size) return type + section.getItemViewType(position - 1);
			// otherwise jump into next section
			position -= size;
			type += section.getViewTypeCount();
		}
		return -1;
	}
	
	@Override
	public boolean isEnabled(int position) {
		return getItemViewType(position) != HEADER_VIEW_TYPE;
	}
}
