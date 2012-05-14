package com.indrimuska.kulinaria;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
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
		public View getView(final FragmentActivity activity) {
			// Creating view
			final LinearLayout layout = new LinearLayout(activity);
			layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			layout.setOrientation(LinearLayout.VERTICAL);
			
			// List all ingredients
			layout.addView(getInventoryFromDatabase(activity));
			
			// Add ingredient's button
			Button button = new Button(activity);
			LayoutParams buttonParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			buttonParams.gravity = Gravity.CENTER;
			button.setLayoutParams(buttonParams);
			button.setText(R.string.ingredientAdd);
			button.setOnClickListener(new OnClickListener() {
				AlertDialog dialog;
				@Override
				public void onClick(View v) {
					// Creating the dialog
					View view = getLayoutInflater().inflate(R.layout.ingredient_dialog, null);
					final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setView(view);
					builder.setCancelable(true);
					builder.setTitle(R.string.ingredientAdd);
					
					// Inflating auto-complete list
					Cursor cursor = db.getIngredientList();
					startManagingCursor(cursor);
					ArrayAdapter<String> textAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_dropdown_item_1line);
					while (cursor.moveToNext()) textAdapter.add(cursor.getString(cursor.getColumnIndex(DatabaseInterface.INGREDIENTS.name)));
					final AutoCompleteTextView name = (AutoCompleteTextView) view.findViewById(R.id.elementIngredientName);
			        name.setAdapter(textAdapter);
					
					// Inflating spinner
					final EditText quantity = (EditText) view.findViewById(R.id.elementIngredientQuantity);
					final Spinner unit = (Spinner) view.findViewById(R.id.elementIngredientUnit);
					ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
							MainActivity.this, R.array.units, R.layout.spinner_text_white);
					spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					unit.setAdapter(spinnerAdapter);
					
					// Expired date's button
					final Calendar date = Calendar.getInstance();
					final Button expirationDate = (Button) view.findViewById(R.id.elementIngredientExpiredDate);
					expirationDate.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							new DatePickerDialog(MainActivity.this, dateSetListener,
									date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show();
						}
						private OnDateSetListener dateSetListener = new OnDateSetListener() {
							@Override
							public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
								date.set(year, monthOfYear, dayOfMonth);
								expirationDate.setText(DateFormat.format(getString(R.string.dateFormat), date.getTime().getTime()));
							}
						};
					});
					
					// On name changed
					name.addTextChangedListener(new TextWatcher() {
						// @SuppressWarnings is for conversion from SpinnerAdapter to ArrayAdapter<String>
						@Override
						@SuppressWarnings("unchecked")
						public void onTextChanged(CharSequence text, int start, int before, int count) {
							if (db.ingredientAlreadyExists(text.toString().trim())) {
								// Set ingredient's informations into dialog
								dialog.setTitle(R.string.ingredientUpdate);
								Cursor cursor = db.getIngredient(name.getText().toString().trim());
								startManagingCursor(cursor);
								cursor.moveToFirst();
								Double quantityDouble = cursor.getDouble(cursor.getColumnIndex(DatabaseInterface.INGREDIENTS.quantity));
								Long expirationDateLong = cursor.getLong(cursor.getColumnIndex(DatabaseInterface.INGREDIENTS.expirationDate));
								String unitString = cursor.getString(cursor.getColumnIndex(DatabaseInterface.INGREDIENTS.unit));
								quantity.setText(Double.toString(quantityDouble));
								unit.setSelection(((ArrayAdapter<String>) unit.getAdapter()).getPosition(unitString));
								if (expirationDateLong > 0) {
									date.setTime(new Date(expirationDateLong));
									expirationDate.setText(DateFormat.format(getString(R.string.dateFormat), date.getTime().getTime()));
									if (expirationDateLong < new Date().getTime())
										expirationDate.getBackground().setColorFilter(
												new PorterDuffColorFilter(Color.rgb(255, 102, 0), PorterDuff.Mode.SRC_ATOP));
									else expirationDate.getBackground().clearColorFilter();
								}
							} else {
								// Reset dialog interface
								dialog.setTitle(R.string.ingredientAdd);
								quantity.setText("");
								unit.setSelection(0);
								expirationDate.getBackground().clearColorFilter();
								expirationDate.setText(getString(R.string.ingredientExpiredDateNoExpiry));
							}
						}
						// other methods to override (nothing to do)
						@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
						@Override public void afterTextChanged(Editable s) { }
					});
					
					// Setting buttons and open the dialog
					builder.setPositiveButton(R.string.ingredientSave, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							String ingredientName = name.getText().toString().trim();
							Double ingredientQuantity = new Double(quantity.getText().toString());
							String ingredientUnit = unit.getSelectedItem().toString();
							Long ingredientExpirationDate =
									expirationDate.getText().toString().equals(R.string.ingredientExpiredDateNoExpiry)
									? 0 : date.getTime().getTime();
							// Check if an ingredient is already stored
							if (db.ingredientAlreadyExists(ingredientName)) {
								String ingredientUpdate = getString(R.string.ingredientUpdated).replaceFirst("\\?", ingredientName);
								db.updateIngredient(ingredientName, ingredientQuantity, ingredientUnit, ingredientExpirationDate);
								Toast.makeText(MainActivity.this, ingredientUpdate, Toast.LENGTH_LONG).show();
							} else db.insertIngredient(ingredientName, ingredientQuantity, ingredientUnit, ingredientExpirationDate);
							// Updating the inventory page
							layout.removeViewAt(0);
							layout.addView(getInventoryFromDatabase(activity), 0);
						}
					});
					builder.setNegativeButton(R.string.ingredientCancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
					dialog = builder.create();
					dialog.show();
				}
			});
			layout.addView(button);
			
			return layout;
		}

		// Inflate rows using SimpleCursorAdapter
		private ListView getInventoryFromDatabase(Context activity) {
			// Get the data from the database
			Cursor cursor = db.getIngredientList();
			startManagingCursor(cursor);
			
			// Inflate rows using SimpleCursorAdapter
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
			return list;
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
