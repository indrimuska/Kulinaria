package com.indrimuska.kulinaria;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.viewpagerindicator.PageIndicator;
import com.viewpagerindicator.TabPageIndicator;
import com.viewpagerindicator.TitleProvider;

public class MainActivity extends FragmentActivity {
	SliderAdapter sliderAdapter;
	ViewPager pager;
	PageIndicator indicator;
	DatabaseInterface db;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// Initializing slider
		sliderAdapter = new SliderAdapter(getSupportFragmentManager());
		
		pager = (ViewPager) findViewById(R.id.pager);
		pager.setAdapter(sliderAdapter);
		
		indicator = (TabPageIndicator) findViewById(R.id.indicator);
		indicator.setViewPager(pager);
		
		// Initializing database
		db = new DatabaseInterface(this);
	}
	
	class SliderAdapter extends FragmentPagerAdapter implements TitleProvider {
		ArrayList<Page> pages = null;
		
		public SliderAdapter(FragmentManager fragmentManager) {
			super(fragmentManager);
			pages = new ArrayList<Page>();
			pages.add(new InventoryPage());
			pages.add(new MenuPage());
			pages.add(new RecipesPage());
			pages.add(new ShoppingListPage());
		}

		@Override
		public Fragment getItem(int position) {
			return SliderFragment.newInstance(pages.get(position % pages.size()));
		}

		@Override
		public int getCount() {
			return pages.size();
		}

		@Override
		public String getTitle(int position) {
			return pages.get(position % pages.size()).getTitle().toUpperCase();
		}
	}
	
	// Generic page to implement
	abstract static class Page {
		String pageName;
		static int pageNumber = -1;
		public Page() { pageNumber++; }
		public String getTitle() { return pageName; }
		public int getPageNumber() { return pageNumber; }
		public abstract View getView(FragmentActivity activity);
	}
	class InventoryPage extends Page {
		public final int pageNumber;
		
		public InventoryPage() {
			pageName = "Inventory";
			pageNumber = super.getPageNumber();
		}

		@Override
		public View getView(FragmentActivity activity) {
			TextView text = new TextView(activity);
			text.setGravity(Gravity.CENTER);
			text.setText(String.valueOf(pageNumber));
			text.setTextSize(20 * getResources().getDisplayMetrics().density);
			text.setPadding(20, 20, 20, 20);
			return text;
		}
	}
	class MenuPage extends Page {
		public final int pageNumber;
		
		public MenuPage() {
			pageName = "Menu";
			pageNumber = super.getPageNumber();
		}

		@Override
		public View getView(FragmentActivity activity) {
			TextView text = new TextView(activity);
			text.setGravity(Gravity.CENTER);
			text.setText(String.valueOf(pageNumber));
			text.setTextSize(20 * getResources().getDisplayMetrics().density);
			text.setPadding(20, 20, 20, 20);
			return text;
		}
	}
	class RecipesPage extends Page {
		public final int pageNumber;
		
		public RecipesPage() {
			pageName = "Recipes";
			pageNumber = super.getPageNumber();
		}

		@Override
		public View getView(FragmentActivity activity) {
			TextView text = new TextView(activity);
			text.setGravity(Gravity.CENTER);
			text.setText(String.valueOf(pageNumber));
			text.setTextSize(20 * getResources().getDisplayMetrics().density);
			text.setPadding(20, 20, 20, 20);
			return text;
		}
	}
	class ShoppingListPage extends Page {
		public final int pageNumber;
		
		public ShoppingListPage() {
			pageName = "Shopping List";
			pageNumber = super.getPageNumber();
		}

		@Override
		public View getView(FragmentActivity activity) {
			TextView text = new TextView(activity);
			text.setGravity(Gravity.CENTER);
			text.setText(String.valueOf(pageNumber));
			text.setTextSize(20 * getResources().getDisplayMetrics().density);
			text.setPadding(20, 20, 20, 20);
			return text;
		}
	}
}
