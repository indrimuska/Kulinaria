package com.indrimuska.kulinaria;

import com.indrimuska.kulinaria.MainActivity.Page;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public final class SliderFragment extends Fragment {
	private static final String KEY_CONTENT = "SliderFragment:page";
	
	private Page page;

	public static SliderFragment newInstance(Page page) {
		SliderFragment fragment = new SliderFragment();
		fragment.page = page;
		return fragment;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if ((savedInstanceState != null) && savedInstanceState.containsKey(KEY_CONTENT)) {
			page = (Page) savedInstanceState.getSerializable(KEY_CONTENT);
		}
		
		//TextView text = new TextView(getActivity());
		//text.setGravity(Gravity.CENTER);
		//text.setText(String.valueOf(pageNumber));
		//text.setTextSize(20 * getResources().getDisplayMetrics().density);
		//text.setPadding(20, 20, 20, 20);
		
		LinearLayout layout = new LinearLayout(getActivity());
		layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		layout.setGravity(Gravity.CENTER);
		//layout.addView(text);
		layout.addView(page.getView(getActivity()));
		
		return layout;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(KEY_CONTENT, page.getClass());
	}
}