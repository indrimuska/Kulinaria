package com.indrimuska.kulinaria;

import com.indrimuska.kulinaria.MainActivity.Page;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
		if (savedInstanceState != null && savedInstanceState.containsKey(KEY_CONTENT))
			page = (Page) savedInstanceState.getSerializable(KEY_CONTENT);
		return page.getView(getActivity());
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(KEY_CONTENT, page.getClass());
	}
}