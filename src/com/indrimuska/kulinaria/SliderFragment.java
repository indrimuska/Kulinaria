package com.indrimuska.kulinaria;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public final class SliderFragment extends Fragment {
	private static final String KEY_CONTENT = "SliderFragment:Content";
	
	private String content;
	
	public static SliderFragment newInstance(int pageNumber) {
		SliderFragment fragment = new SliderFragment();
		
		// Setting content string
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 20; i++) {
			builder.append(pageNumber).append(" ");
		}
		builder.deleteCharAt(builder.length() - 1);
		fragment.content = builder.toString();
		
		return fragment;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if ((savedInstanceState != null) && savedInstanceState.containsKey(KEY_CONTENT)) {
			content = savedInstanceState.getString(KEY_CONTENT);
		}
		
		TextView text = new TextView(getActivity());
		text.setGravity(Gravity.CENTER);
		text.setText(content);
		text.setTextSize(20 * getResources().getDisplayMetrics().density);
		text.setPadding(20, 20, 20, 20);
		
		LinearLayout layout = new LinearLayout(getActivity());
		layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		layout.setGravity(Gravity.CENTER);
		layout.addView(text);
		
		return layout;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_CONTENT, content);
	}
}