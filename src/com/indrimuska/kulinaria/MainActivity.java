package com.indrimuska.kulinaria;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
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
	
	@Override
	protected void onPause() {
		super.onPause();
		
		// Close database
		db.close();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
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
		public Page(int name) { this.pageName = getString(name); }
		public String getTitle() { return pageName; }
		public abstract View getView();
	}
	
	// Slider's pages implementation
	final class MenuPage extends Page {
		public MenuPage() { super(R.string.menuPage); }
		
		@Override
		public View getView() {
			Locale.setDefault(new Locale(getString(R.string.language)));
			LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.menu_page, null);
			((TextView) layout.findViewById(R.id.menuTodayNumber)).setText(new SimpleDateFormat("dd").format(new Date()));
			((TextView) layout.findViewById(R.id.menuTodayDate)).setText(
					new SimpleDateFormat(getString(R.string.extendedDateFormat), Locale.getDefault()).format(new Date()));
			
			// Menu grouped by meal
			final ArrayList<ArrayList<Integer>> menuByMeal = new ArrayList<ArrayList<Integer>>();
			for (String meal : getResources().getStringArray(R.array.meals))
				menuByMeal.add(db.getMenu(new SimpleDateFormat("yyyy-MM-dd").format(new Date()), meal));
			ExpandableListView menuMealsList = (ExpandableListView) layout.findViewById(R.id.menuMealsList);
			menuMealsList.setAdapter(new BaseExpandableListAdapter() {
				private String[] groups = getResources().getStringArray(R.array.meals);
				
				@Override
				public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
					TextView meal = new TextView(MainActivity.this);
					meal.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
					meal.setText(getGroup(groupPosition).toString());
					meal.setPadding(60, 20, 20, 20);
					return meal;
				}
				@Override
				public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
					LinearLayout recipe = (LinearLayout) getLayoutInflater().inflate(R.layout.menu_list_item, null);
					TextView recipeName = (TextView) recipe.findViewById(R.id.menuRecipeName);
					Cursor cursor = db.getRecipe(new Integer(getChild(groupPosition, childPosition).toString()));
					try {
						cursor.moveToFirst();
						recipeName.setText(cursor.getString(cursor.getColumnIndex(DatabaseInterface.RECIPES.name)));
					} finally {
						cursor.close();
					}
					return recipe;
				}
				// Other methods
				@Override public Object getGroup(int groupPosition) { return groups[groupPosition]; }
				@Override public long getGroupId(int groupPosition) { return groupPosition; }
				@Override public int getGroupCount() { return groups.length; }
				@Override public Object getChild(int groupPosition, int childPosition) { return menuByMeal.get(groupPosition).get(childPosition).toString(); }
				@Override public long getChildId(int groupPosition, int childPosition) { return childPosition; }
				@Override public int getChildrenCount(int groupPosition) { return menuByMeal.get(groupPosition).size(); }
				@Override public boolean isChildSelectable(int groupPosition, int childPosition) { return true; }
				@Override public boolean hasStableIds() { return true; }
			});
			return layout;
		}
	}
	final class InventoryPage extends Page {
		public InventoryPage() { super(R.string.inventoryPage); }
		
		@Override
		public View getView() {
			// Inflating view
			final LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.inventory_page, null);
			layout.addView(getInventoryListViewFromDatabase(), 0);
			
			// Add ingredient's button
			Button addIngredientButton = (Button) layout.findViewById(R.id.listIngredientAdd);
			addIngredientButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					new IngredientDialog(layout).show(null);
				}
			});
			
			return layout;
		}
		
		// Return a ListView filled with all ingredients in the inventory
		public View getInventoryListViewFromDatabase() {
			// Get the data from the database
			Cursor cursor = db.getInventory();
			ArrayList<Map<String, Object>> inventoryList = new ArrayList<Map<String, Object>>();
			String[] from = new String[] {
					DatabaseInterface.INVENTORY.name,
					DatabaseInterface.INVENTORY.quantity,
					DatabaseInterface.INVENTORY.unit,
					DatabaseInterface.INVENTORY.id
			};
			
			try {
				// Check if there are any ingredients stored
				if (cursor.getCount() > 0) inventoryList = db.cursorToMapArray(cursor, from);
				else {
					TextView text = new TextView(MainActivity.this);
					text.setGravity(Gravity.CENTER);
					text.setText(R.string.inventoryNoIngredients);
					text.setTextSize(20 * getResources().getDisplayMetrics().density);
					text.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
					text.setPadding(20, 20, 20, 20);
					return text;
				}
			} finally {
				cursor.close();
			}
			
			// Inflate rows using SimpleCursorAdapter
			ListView list = new ListView(MainActivity.this);
			list.setScrollingCacheEnabled(false);
			list.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
			SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, inventoryList, R.layout.ingredient_list_item, from,
					new int[] {
							R.id.listIngredientName,
							R.id.listIngredientQuantity,
							R.id.listIngredientUnit,
							R.id.listIngredientOptions
					});
			adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
				@Override
				public boolean setViewValue(View view, Object data, String textRepresentation) {
					if (view.getId() != R.id.listIngredientOptions) return false;
					view.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(final View view) {
							// Create dialog menu
							new AlertDialog.Builder(MainActivity.this)
								.setItems(new String[] {
										getString(R.string.ingredientUpdate),
										getString(R.string.ingredientDelete)
								}, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										final LinearLayout layout = (LinearLayout) view.getParent();
										final TextView name = (TextView) layout.findViewById(R.id.listIngredientName);
										switch (which) {
										case 0:
											// Update ingredient
											new IngredientDialog(layout).show(name);
											break;
										case 1:
											// Delete ingredient
											new AlertDialog.Builder(MainActivity.this)
												.setTitle(R.string.ingredientDelete)
												.setMessage(R.string.ingredientDeleteQuestion)
												.setNegativeButton(R.string.ingredientCancel, null)
												.setPositiveButton(R.string.ingredientDeleteButton, new AlertDialog.OnClickListener() {
													@Override
													public void onClick(DialogInterface dialog, int which) {
														String ingredientName = name.getText().toString().trim();
														db.deleteInventoryIngredient(db.getInventoryIngredientID(ingredientName));
														String ingredientUpdate = getString(R.string.ingredientDeleted)
																.replaceFirst("\\?", ingredientName);
														Toast.makeText(MainActivity.this, ingredientUpdate, Toast.LENGTH_LONG).show();
														// Updating the inventory page
														LinearLayout linearLayout = (LinearLayout) layout.getParent().getParent();
														linearLayout.removeViewAt(0);
														linearLayout.addView(getInventoryListViewFromDatabase(), 0);
													}
												}).show();
											break;
										}
									}
								}).show();
						}
					});
					return true;
				}
			});
			list.setAdapter(adapter);
			return list;
		}
		
		// Open the dialog
		private class IngredientDialog {
			int ingredientID;
			AlertDialog dialog;
			LinearLayout layout;
			
			public IngredientDialog(LinearLayout layout) {
				this.ingredientID = db.getMaxInventoryIngredientID()+1;
				this.layout = layout;
			}
			
			// Show dialog window
			@SuppressWarnings("unchecked")
			public void show(final TextView ingredientView) {
				// Creating the dialog
				View dialogView = getLayoutInflater().inflate(R.layout.ingredient_dialog, null);
				final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle(R.string.ingredientAdd);
				builder.setView(dialogView);
				
				// Identifying elements
				final AutoCompleteTextView name = (AutoCompleteTextView) dialogView.findViewById(R.id.elementIngredientName);
				final EditText quantity = (EditText) dialogView.findViewById(R.id.elementIngredientQuantity);
				final Spinner unit = (Spinner) dialogView.findViewById(R.id.elementIngredientUnit);
				final Button expirationDate = (Button) dialogView.findViewById(R.id.elementIngredientExpirationDate);
				final ImageButton removeExpirationDate = (ImageButton) dialogView.findViewById(R.id.elementIngredientRemoveExpirationDate);
				
				// Inflating auto-complete list
				Cursor cursor = db.getIngredients();
				ArrayAdapter<String> textAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_dropdown_item_1line);
				try {
					try {
						while (cursor.moveToNext())
							textAdapter.add(cursor.getString(cursor.getColumnIndex(DatabaseInterface.INGREDIENTS.name)));
					} finally {
						cursor.close();
					}
				} finally {
					db.close();
				}
				name.setAdapter(textAdapter);
				
				// Inflating spinner
				ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
						MainActivity.this, R.array.units, R.layout.spinner_text_white);
				spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				unit.setAdapter(spinnerAdapter);
				
				// Expiration date button listener
				expirationDate.setOnClickListener(new OnClickListener() {
					Calendar calendar = Calendar.getInstance();
					@Override
					public void onClick(View v) {
						Date date = new Date();
						SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.simpleDateFormat));
						try { date = dateFormat.parse(expirationDate.getText().toString()); }
						catch (ParseException e) { }
						calendar.setTime(date);
						new DatePickerDialog(MainActivity.this, new OnDateSetListener() {
							@Override
							public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
								calendar.set(year, monthOfYear, dayOfMonth);
								expirationDate.setText(DateFormat.format(getString(R.string.simpleDateFormat), calendar.getTime().getTime()));
								if (calendar.getTime().getDate() < new Date().getDate())
									expirationDate.getBackground().setColorFilter(
											new PorterDuffColorFilter(Color.rgb(255, 102, 0), PorterDuff.Mode.SRC_ATOP));
								else expirationDate.getBackground().clearColorFilter();
								removeExpirationDate.setVisibility(View.VISIBLE);
							}
						}, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
					}
				});

				// Remove expiration date button listener
				removeExpirationDate.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						expirationDate.setText(R.string.ingredientExpirationDateNoExpiry);
						expirationDate.getBackground().clearColorFilter();
						removeExpirationDate.setVisibility(View.INVISIBLE);
					}
				});
				
				// Dialog for update
				if (ingredientView != null) {
					builder.setTitle(R.string.ingredientUpdate);
					String ingredienName = ingredientView.getText().toString().trim();
					cursor = db.getInventoryIngredient(ingredienName);
					String unitString;
					double quantityDouble;
					long expirationDateLong;
					try {
						cursor.moveToFirst();
						ingredientID = cursor.getInt(cursor.getColumnIndex(DatabaseInterface.INVENTORY.id));
						unitString = cursor.getString(cursor.getColumnIndex(DatabaseInterface.INVENTORY.unit));
						quantityDouble = cursor.getDouble(cursor.getColumnIndex(DatabaseInterface.INVENTORY.quantity));
						expirationDateLong = cursor.getLong(cursor.getColumnIndex(DatabaseInterface.INVENTORY.expirationDate));
					} finally {
						cursor.close();
					}
					name.setText(ingredienName);
					name.setAdapter(null);
					quantity.setText(Double.toString(quantityDouble));
					unit.setSelection(((ArrayAdapter<String>) unit.getAdapter()).getPosition(unitString));
					if (expirationDateLong > 0) {
						Date date = new Date(expirationDateLong);
						expirationDate.setText(DateFormat.format(getString(R.string.simpleDateFormat), date.getTime()));
						if (new Date(expirationDateLong).getDate() < new Date().getDate())
							expirationDate.getBackground().setColorFilter(
									new PorterDuffColorFilter(Color.rgb(255, 102, 0), PorterDuff.Mode.SRC_ATOP));
						removeExpirationDate.setVisibility(View.VISIBLE);
					}
				}
				
				// Setting buttons and open the dialog
				builder.setNegativeButton(R.string.ingredientCancel, null);
				builder.setPositiveButton(R.string.ingredientSave, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (name.getText().toString().trim().length() == 0 || quantity.getText().length() == 0) return;
						Date date = new Date();
						SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.simpleDateFormat));
						try { date = dateFormat.parse(expirationDate.getText().toString()); }
						catch (ParseException e) { }
						final String ingredientName = name.getText().toString().trim();
						final double ingredientQuantity = new Double(quantity.getText().toString());
						final String ingredientUnit = unit.getSelectedItem().toString();
						final long ingredientExpirationDate =
								expirationDate.getText().toString().equals(getString(R.string.ingredientExpirationDateNoExpiry))
								? 0 : date.getTime();
						final int existingIngredientID;
						// Check if another ingredient is already stored with the same name
						if (db.inventoryIngredientAlreadyExists(ingredientName) &&
							(existingIngredientID = db.getInventoryIngredientID(ingredientName)) != ingredientID) {
							String ingredientAlreadyExists = getString(R.string.ingredientAlreadyExists).replaceFirst("\\?", ingredientName);
							new AlertDialog.Builder(MainActivity.this)
								.setMessage(ingredientAlreadyExists)
								.setNegativeButton(R.string.ingredientCancel, null)
								.setPositiveButton(R.string.ingredientSave, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										db.deleteInventoryIngredient(existingIngredientID);
										// New ingredient replace existing one
										if (ingredientView == null) db.insertInventoryIngredient(
												ingredientName, ingredientQuantity, ingredientUnit, ingredientExpirationDate);
										// Existing ingredient replace another one
										else db.updateInventoryIngredient(ingredientID,
												ingredientName, ingredientQuantity, ingredientUnit, ingredientExpirationDate);
										String ingredientUpdate = getString(R.string.ingredientUpdated).replaceFirst("\\?", ingredientName);
										Toast.makeText(MainActivity.this, ingredientUpdate, Toast.LENGTH_LONG).show();
										// Updating the inventory page
										layout.removeViewAt(0);
										layout.addView(getInventoryListViewFromDatabase(), 0);
									}
								}).show();
						} else {
							if (ingredientView == null) {
								// Insert new ingredient
								db.insertInventoryIngredient(ingredientName, ingredientQuantity, ingredientUnit, ingredientExpirationDate);
								String ingredientAdded = getString(R.string.ingredientAdded).replaceFirst("\\?", ingredientName);
								Toast.makeText(MainActivity.this, ingredientAdded, Toast.LENGTH_LONG).show();
							} else {
								// Update existing ingredient
								db.updateInventoryIngredient(ingredientID, ingredientName, ingredientQuantity, ingredientUnit, ingredientExpirationDate);
								String ingredientUpdate = getString(R.string.ingredientUpdated).replaceFirst("\\?", ingredientName);
								Toast.makeText(MainActivity.this, ingredientUpdate, Toast.LENGTH_LONG).show();
								layout = (LinearLayout) layout.getParent().getParent();
							}
							// Updating the inventory page
							layout.removeViewAt(0);
							layout.addView(getInventoryListViewFromDatabase(), 0);
						}
					}
				});
				dialog = builder.create();
				dialog.show();
			}
		}
	}
	final class RecipesPage extends Page {
		public RecipesPage() { super(R.string.recipesPage); }
		public ListView list = null;
		public ListView search = null;
		public EditText searchField = null;
		
		@Override
		public View getView() {
			// Setting view
			LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.recipes_page, null);
			list = (ListView) layout.findViewById(R.id.recipesDishesList);
			search = (ListView) layout.findViewById(R.id.recipesSearchList);
			searchField = (EditText) layout.findViewById(R.id.recipesSearch);
			list.setScrollingCacheEnabled(false);
			search.setScrollingCacheEnabled(false);
			
			// Filling contents using SimpleAdapter
			ArrayList<Map<String, Object>> dishes = new ArrayList<Map<String, Object>>();
			String[] dishesList = getResources().getStringArray(R.array.dishes);
			for (int i = 0; i < dishesList.length; i++) {
				Map<String, Object> dishInfo = new HashMap<String, Object>();
				dishInfo.put("image", getResources().getIdentifier("drawable/dishes_" + i, "drawable", getPackageName()));
				dishInfo.put("name", dishesList[i]);
				dishInfo.put("other", getString(R.string.recipesNumber)
						.replaceFirst("\\?", Integer.toString(db.getRecipeCount(dishesList[i]))));
				dishes.add(dishInfo);
			}
			list.setAdapter(new SimpleAdapter(MainActivity.this, dishes, R.layout.dishes_list_item,
					new String[] { "image", "name", "other" },
					new int[] { R.id.listDishImage, R.id.listDishName, R.id.listDishOther }));
			list.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					startActivity(new Intent(MainActivity.this, RecipesListActivity.class)
						.putExtra("dish", ((TextView) view.findViewById(R.id.listDishName)).getText().toString()));
				}
			});
			
			// Recipes search listeners
			searchField.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View view, MotionEvent event) {
					Drawable clearButton = searchField.getCompoundDrawables()[2];
					if (clearButton != null && event.getAction() == MotionEvent.ACTION_DOWN) {
						if (event.getX() > view.getWidth() - view.getPaddingRight() - clearButton.getIntrinsicWidth()) {
							searchField.setText("");
							searchField.setCompoundDrawables(null, null, null, null);
				        }
					}
					return false;
				}
			});
			searchField.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					LayoutParams show = new LayoutParams(LayoutParams.FILL_PARENT, 0, 1);
					LayoutParams hidden = new LayoutParams(LayoutParams.FILL_PARENT, 0, 0);
					int dp20 = (int) (20 * getResources().getDisplayMetrics().density);
					show.setMargins(dp20, dp20, dp20, dp20);
					hidden.setMargins(0, 0, 0, 0);
					if (s.toString().trim().length() == 0) {
						list.setLayoutParams(show);
						search.setLayoutParams(hidden);
						list.setVisibility(View.VISIBLE);
						search.setVisibility(View.INVISIBLE);
						return;
					}
					list.setLayoutParams(hidden);
					search.setLayoutParams(show);
					list.setVisibility(View.INVISIBLE);
					search.setVisibility(View.VISIBLE);
					Drawable clearButton = getResources().getDrawable(R.drawable.ic_clear_holo_light);
					clearButton.setBounds(0, 0, clearButton.getIntrinsicWidth(), clearButton.getIntrinsicHeight());
					searchField.setCompoundDrawables(null, null, clearButton, null);
					
					Cursor cursor = db.searchRecipe(((EditText) ((LinearLayout)
							search.getParent()).findViewById(R.id.recipesSearch)).getText().toString().trim());
					ArrayList<Map<String, Object>> recipesList = new ArrayList<Map<String, Object>>();
					String[] from = new String[] {
							DatabaseInterface.RECIPES.id,
							DatabaseInterface.RECIPES.name,
							DatabaseInterface.RECIPES.readyTime
					};
					try { recipesList = db.cursorToMapArray(cursor, from); }
					finally { cursor.close(); }
					SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, recipesList, R.layout.recipes_list_item, from,
							new int[] {
								R.id.recipesListId,
								R.id.recipesListName,
								R.id.recipesListOther
							});
					adapter.setViewBinder(new SimpleAdapter.ViewBinder() {
						@Override
						public boolean setViewValue(View view, Object data, String textRepresentation) {
							if (view.getId() != R.id.recipesListOther) return false;
							((TextView) view).setText(getString(R.string.recipeReadyIn).replace("?",
									RecipeActivity.secondsToTime(Integer.parseInt(data.toString()) * 60)));
							return true;
						}
					});
					search.setAdapter(adapter);
					search.setOnItemClickListener(new AdapterView.OnItemClickListener() {
						@Override
						public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
							startActivity(new Intent(MainActivity.this, RecipeActivity.class)
								.putExtra("recipeId", Integer.parseInt(
										((TextView) view.findViewById(R.id.recipesListId)).getText().toString())));
						}
					});
				}
				// Other methods
				@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
				@Override public void afterTextChanged(Editable s) { }
			});
			
			return layout;
		}
	}
	final class ShoppingListPage extends Page {
		public ShoppingListPage() { super(R.string.shoppingListPage); }
		
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
