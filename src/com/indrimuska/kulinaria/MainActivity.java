package com.indrimuska.kulinaria;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
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
	private SliderAdapter sliderAdapter;
	private ViewPager pager;
	private PageIndicator indicator;
	private DatabaseInterface db;
	
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
		public abstract View getView();
	}
	
	// Slider's pages implementation
	final class MenuPage extends Page {
		public MenuPage() { super("Menu"); }
		
		@Override
		public View getView() {
			TextView text = new TextView(MainActivity.this);
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
		public View getView() {
			// Creating view
			final LinearLayout layout = new LinearLayout(MainActivity.this);
			layout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			layout.setOrientation(LinearLayout.VERTICAL);
			
			// List all ingredients
			layout.addView(getInventoryListViewFromDatabase());
			
			// Add ingredient's button
			Button addIngredientButton = new Button(MainActivity.this);
			LayoutParams buttonParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			buttonParams.gravity = Gravity.CENTER;
			addIngredientButton.setLayoutParams(buttonParams);
			addIngredientButton.setText(R.string.ingredientAdd);
			addIngredientButton.setOnClickListener(new OnClickListener() {
				AlertDialog dialog;
				@Override
				public void onClick(View v) {
					new IngredientDialog(dialog, layout).show();
				}
			});
			layout.addView(addIngredientButton);
			
			return layout;
		}
		
		// Inflate rows using SimpleCursorAdapter
		private ListView getInventoryListViewFromDatabase() {
			// Get the data from the database
			Cursor cursor = db.getIngredientList();
			startManagingCursor(cursor);
			
			// Inflate rows using SimpleCursorAdapter
			ListView list = new ListView(MainActivity.this);
			list.setScrollingCacheEnabled(false);
			list.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
			list.setAdapter(new SimpleCursorAdapter(MainActivity.this, R.layout.ingredient, cursor,
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
			list.setOnItemClickListener(new OnItemClickListener() {
				AlertDialog dialog;
				@Override
				public void onItemClick(AdapterView<?> parentView, View childView, int position, long id) {
					new IngredientDialog(dialog, (LinearLayout) childView)
						.show(childView.findViewById(R.id.listIngredientName));
				}
			});
			db.close();
			return list;
		}
		
		// Open the dialog
		private class IngredientDialog {
			AlertDialog dialog;
			LinearLayout layout;
			
			public IngredientDialog(AlertDialog dialog, LinearLayout layout) {
				this.dialog = dialog;
				this.layout = layout;
			}
			
			// Show empty dialog
			public void show() {
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
				final Button expirationDate = (Button) view.findViewById(R.id.elementIngredientExpirationDate);
				expirationDate.setOnClickListener(new OnClickListener() {
					Calendar calendar = Calendar.getInstance();
					@Override
					public void onClick(View v) {
						Date date = new Date();
						SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.dateFormat));
						try { date = dateFormat.parse(expirationDate.getText().toString()); }
						catch (ParseException e) { }
						calendar.setTime(date);
						new DatePickerDialog(MainActivity.this, new OnDateSetListener() {
							@Override
							public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
								calendar.set(year, monthOfYear, dayOfMonth);
								expirationDate.setText(DateFormat.format(getString(R.string.dateFormat), calendar.getTime().getTime()));
								if (calendar.getTime().getDate() < new Date().getDate())
									expirationDate.getBackground().setColorFilter(
											new PorterDuffColorFilter(Color.rgb(255, 102, 0), PorterDuff.Mode.SRC_ATOP));
								else expirationDate.getBackground().clearColorFilter();
							}
						}, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
					}
				});
				
				// On name changed
				name.addTextChangedListener(ingredientNameWatcher);
				
				// Setting buttons and open the dialog
				builder.setPositiveButton(R.string.ingredientSave, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Date date = new Date();
						SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.dateFormat));
						try { date = dateFormat.parse(expirationDate.getText().toString()); }
						catch (ParseException e) { }
						String ingredientName = name.getText().toString().trim();
						Double ingredientQuantity = new Double(quantity.getText().toString());
						String ingredientUnit = unit.getSelectedItem().toString();
						Long ingredientExpirationDate =
								expirationDate.getText().toString().equals(R.string.ingredientExpirationDateNoExpiry)
								? 0 : date.getTime();
						// Check if an ingredient is already stored
						if (db.ingredientAlreadyExists(ingredientName)) {
							int ingredientID = db.getIngredientID(ingredientName);
							String ingredientUpdate = getString(R.string.ingredientUpdated).replaceFirst("\\?", ingredientName);
							db.updateIngredient(ingredientID, ingredientName, ingredientQuantity, ingredientUnit, ingredientExpirationDate);
							Toast.makeText(MainActivity.this, ingredientUpdate, Toast.LENGTH_LONG).show();
						} else db.insertIngredient(ingredientName, ingredientQuantity, ingredientUnit, ingredientExpirationDate);
						// Updating the inventory page
						layout.removeViewAt(0);
						layout.addView(getInventoryListViewFromDatabase(), 0);
					}
				});
				builder.setNegativeButton(R.string.ingredientCancel, new DialogInterface.OnClickListener() {
					@Override public void onClick(DialogInterface dialog, int which) { }
				});
				dialog = builder.create();
				dialog.show();
			}
			
			// Show filled dialog
			public void show(View view) {
				show();
				final AutoCompleteTextView name = (AutoCompleteTextView) dialog.findViewById(R.id.elementIngredientName);
				name.setText(((TextView) view).getText());
				name.setAdapter(null);
				name.removeTextChangedListener(ingredientNameWatcher);
				dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.ingredientSave), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int which) {
						Button expirationDate = (Button) dialog.findViewById(R.id.elementIngredientExpirationDate);
						EditText quantity = (EditText) dialog.findViewById(R.id.elementIngredientQuantity);
						Spinner unit = (Spinner) dialog.findViewById(R.id.elementIngredientUnit);
						Date date = new Date();
						SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.dateFormat));
						try { date = dateFormat.parse(expirationDate.getText().toString()); }
						catch (ParseException e) { }
						String ingredientName = name.getText().toString().trim();
						Double ingredientQuantity = new Double(quantity.getText().toString());
						String ingredientUnit = unit.getSelectedItem().toString();
						Long ingredientExpirationDate =
								expirationDate.getText().toString().equals(R.string.ingredientExpirationDateNoExpiry)
								? 0 : date.getTime();
						// Check if an ingredient is already stored
						int ingredientID = db.getIngredientID(ingredientName);
						String ingredientUpdate = getString(R.string.ingredientUpdated).replaceFirst("\\?", ingredientName);
						db.updateIngredient(ingredientID, ingredientName, ingredientQuantity, ingredientUnit, ingredientExpirationDate);
						Toast.makeText(MainActivity.this, ingredientUpdate, Toast.LENGTH_LONG).show();
						// Updating the inventory page
						LinearLayout linearLayout = (LinearLayout) layout.getParent().getParent();
						linearLayout.removeViewAt(0);
						linearLayout.addView(getInventoryListViewFromDatabase(), 0);
					}
				});
			}
			
			private TextWatcher ingredientNameWatcher = new TextWatcher() {
				Calendar calendar = Calendar.getInstance();
				@Override
				@SuppressWarnings("unchecked") // for conversion from SpinnerAdapter to ArrayAdapter<String>
				public void onTextChanged(CharSequence text, int start, int before, int count) {
					EditText quantity = (EditText) dialog.findViewById(R.id.elementIngredientQuantity);
					Spinner unit = (Spinner) dialog.findViewById(R.id.elementIngredientUnit);
					Button expirationDate = (Button) dialog.findViewById(R.id.elementIngredientExpirationDate);
					if (db.ingredientAlreadyExists(text.toString().trim())) {
						// Set ingredient's informations into dialog
						dialog.setTitle(R.string.ingredientUpdate);
						Cursor cursor = db.getIngredient(text.toString().trim());
						startManagingCursor(cursor);
						cursor.moveToFirst();
						Double quantityDouble = cursor.getDouble(cursor.getColumnIndex(DatabaseInterface.INGREDIENTS.quantity));
						Long expirationDateLong = cursor.getLong(cursor.getColumnIndex(DatabaseInterface.INGREDIENTS.expirationDate));
						String unitString = cursor.getString(cursor.getColumnIndex(DatabaseInterface.INGREDIENTS.unit));
						quantity.setText(Double.toString(quantityDouble));
						unit.setSelection(((ArrayAdapter<String>) unit.getAdapter()).getPosition(unitString));
						if (expirationDateLong > 0) {
							Date date = new Date();
							SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.dateFormat));
							try { date = dateFormat.parse(expirationDate.getText().toString()); }
							catch (ParseException e) { }
							calendar.setTime(date);
							expirationDate.setText(DateFormat.format(getString(R.string.dateFormat), calendar.getTime().getTime()));
							if (new Date(expirationDateLong).getDate() < new Date().getDate())
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
						expirationDate.setText(getString(R.string.ingredientExpirationDateNoExpiry));
					}
				}
				// other methods to override (nothing to do)
				@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
				@Override public void afterTextChanged(Editable s) { }
			};
		}
	}
	final class RecipesPage extends Page {
		public RecipesPage() { super("Recipes"); }
		
		@Override
		public View getView() {
			TextView text = new TextView(MainActivity.this);
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
		public View getView() {
			TextView text = new TextView(MainActivity.this);
			text.setGravity(Gravity.CENTER);
			text.setText(pageName);
			text.setTextSize(20 * getResources().getDisplayMetrics().density);
			text.setPadding(20, 20, 20, 20);
			return text;
		}
	}
}
