package com.indrimuska.kulinaria;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import com.viewpagerindicator.PageIndicator;
import com.viewpagerindicator.TabPageIndicator;
import com.viewpagerindicator.TitleProvider;

public class MainActivity extends FragmentActivity {
	private static final String[] indicators = new String[] { "Dispensa", "Menu", "Ricette", "Shopping List" };
	
	SliderAdapter sliderAdapter;
	ViewPager pager;
	PageIndicator indicator;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		sliderAdapter = new SliderAdapter(getSupportFragmentManager());

		pager = (ViewPager) findViewById(R.id.pager);
		pager.setAdapter(sliderAdapter);
		
		indicator = (TabPageIndicator) findViewById(R.id.indicator);
		indicator.setViewPager(pager);
	}
	
	class SliderAdapter extends FragmentPagerAdapter implements TitleProvider {
		public SliderAdapter(FragmentManager fragmentManager) {
			super(fragmentManager);
		}

		@Override
		public Fragment getItem(int position) {
			return SliderFragment.newInstance(position);
		}

		@Override
		public int getCount() {
			return MainActivity.indicators.length;
		}

		@Override
		public String getTitle(int position) {
			return MainActivity.indicators[position % MainActivity.indicators.length].toUpperCase();
		}
	}
}
