package com.indrimuska.kulinaria;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

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
		ArrayList<Page> pages = new ArrayList<Page>();
		
		public SliderAdapter(FragmentManager fragmentManager) {
			super(fragmentManager);
			pages.add(new MenuPage());
			pages.add(new InventoryPage());
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
	abstract class Page {
		protected String pageName;
		public Page(String pageName) { this.pageName = pageName; }
		public String getTitle() { return pageName; }
		public abstract View getView(FragmentActivity activity);
	}
	
	// Slider's pages implementation
	final class MenuPage extends Page {
		public MenuPage() { super("Menu"); }
		
		@Override
		public View getView(FragmentActivity activity) {
			TextView text = new TextView(activity);
			text.setGravity(Gravity.CENTER);
			text.setText(pageName);
			text.setTextSize(20 * getResources().getDisplayMetrics().density);
			text.setPadding(20, 20, 20, 20);
			return text;
		}
	}
	final class InventoryPage extends Page {
		public InventoryPage() { super("Inventory"); }
		
		@Override
		public View getView(FragmentActivity activity) {
			// Get the data from the database
			Cursor cursor = db.getIngredientList();
			startManagingCursor(cursor);
			
			// Creating view
			LinearLayout layout = new LinearLayout(activity);
			layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			layout.setOrientation(LinearLayout.VERTICAL);
			
			// Putting rows using SimpleCursorAdapter
			ListView list = new ListView(activity);
			list.setScrollingCacheEnabled(false);
			list.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
			list.setAdapter(new SimpleCursorAdapter(activity, R.layout.ingredient, cursor,
					new String[] {
							DatabaseInterface.INGREDIENTS.name,
							DatabaseInterface.INGREDIENTS.quantity,
							DatabaseInterface.INGREDIENTS.unit
					},
					new int[] {
							R.id.listIngredientName,
							R.id.listIngredientQuantity,
							R.id.listIngredientUnit
					}));
			layout.addView(list);
			
			// Add ingredient's button
			Button button = new Button(activity);
			LayoutParams buttonParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			buttonParams.gravity = Gravity.CENTER;
			button.setLayoutParams(buttonParams);
			button.setText(R.string.addIngredient);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// Creating the dialog
					View view = getLayoutInflater().inflate(R.layout.ingredient_dialog, null);
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setView(view);
					builder.setCancelable(true);
					builder.setTitle(R.string.addIngredient);
					
					// Inflating spinner
					Spinner spinner = (Spinner) view.findViewById(R.id.elementIngredientUnit);
					ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
							MainActivity.this, R.array.units, R.layout.spinner_text_white);
					adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					spinner.setAdapter(adapter);
					
					// Expired date's button
					final Calendar date = Calendar.getInstance();
					final Button button = (Button) view.findViewById(R.id.elementIngredientExpiredDate);
					button.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							new DatePickerDialog(MainActivity.this, dateSetListener,
									date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show();
						}
						private OnDateSetListener dateSetListener = new OnDateSetListener() {
							@Override
							public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
								date.set(year, monthOfYear, dayOfMonth);
								button.setText(String.format("%s/%s/%s",
										date.get(Calendar.DAY_OF_MONTH), date.get(Calendar.MONTH), date.get(Calendar.YEAR)));
							}
						};
					});
					
					// Setting buttons and open the dialog
					builder.setPositiveButton(R.string.ingredientSave, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					});
					builder.setNegativeButton(R.string.ingredientCancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
					builder.show();
				}
			});
			layout.addView(button);
			
			return layout;
		}
	}
	final class RecipesPage extends Page {
		public RecipesPage() { super("Recipes"); }
		
		@Override
		public View getView(FragmentActivity activity) {
			TextView text = new TextView(activity);
			text.setGravity(Gravity.CENTER);
			text.setText(pageName);
			text.setTextSize(20 * getResources().getDisplayMetrics().density);
			text.setPadding(20, 20, 20, 20);
			return text;
		}
	}
	final class ShoppingListPage extends Page {
		public ShoppingListPage() { super("Shopping List"); }
		
		@Override
		public View getView(FragmentActivity activity) {
			TextView text = new TextView(activity);
			text.setGravity(Gravity.CENTER);
			text.setText(pageName);
			text.setTextSize(20 * getResources().getDisplayMetrics().density);
			text.setPadding(20, 20, 20, 20);
			return text;
		}
	}
}
