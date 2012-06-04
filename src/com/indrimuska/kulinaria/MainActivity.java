package com.indrimuska.kulinaria;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.indrimuska.kulinaria.DatabaseInterface.INGREDIENTS;
import com.indrimuska.kulinaria.DatabaseInterface.INVENTORY;
import com.indrimuska.kulinaria.DatabaseInterface.MENU;
import com.indrimuska.kulinaria.DatabaseInterface.RECIPES;
import com.indrimuska.kulinaria.DatabaseInterface.RECIPES_INGREDIENTS;
import com.viewpagerindicator.TabPageIndicator;
import com.viewpagerindicator.TitleProvider;

public class MainActivity extends FragmentActivity {
	private static DatabaseInterface db;
	private ViewPager pager;
	
	// this pages are refreshed through other pages
	InventoryPage inventoryPage;
	ShoppingListPage shoppingListPage;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// Initializing slider
		pager = (ViewPager) findViewById(R.id.pager);
		pager.setAdapter(new SliderAdapter(getSupportFragmentManager()));
		((TabPageIndicator) findViewById(R.id.indicator)).setViewPager(pager);
		
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
			inventoryPage = new InventoryPage();
			shoppingListPage = new ShoppingListPage();
			
			pages.add(new MenuPage());
			pages.add(inventoryPage);
			pages.add(new RecipesPage());
			pages.add(shoppingListPage);
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
		public abstract void refresh();
	}
	
	// Slider's pages implementation
	final class MenuPage extends Page {
		public MenuPage() { super(R.string.menuPage); }

		// Date of the menu
		private Date day = new Date();
		
		@Override
		public View getView() {
			final LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.menu_page, null);
			inflateView(layout);
			
			// Add dish button listener
			((Button) layout.findViewById(R.id.menuAdd)).setOnClickListener(new OnClickListener() {
				private Spinner meal, dish, recipe;
				
				@Override
				public void onClick(View view) {
					// Creating the dialog
					View dialogView = getLayoutInflater().inflate(R.layout.menu_dish_dialog, null);
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setTitle(R.string.menuDishAdd);
					builder.setView(dialogView);
					
					// Identifying elements
					meal = (Spinner) dialogView.findViewById(R.id.menuMeal);
					dish = (Spinner) dialogView.findViewById(R.id.menuDish);
					recipe = (Spinner) dialogView.findViewById(R.id.menuRecipe);
					
					// Filling all fields
					Locale.setDefault(new Locale(getString(R.string.language)));
					ArrayAdapter<CharSequence> mealAdapter = ArrayAdapter.createFromResource(
							MainActivity.this, R.array.meals, R.layout.spinner_text_white);
					mealAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					meal.setAdapter(mealAdapter);
					ArrayAdapter<CharSequence> dishAdapter = ArrayAdapter.createFromResource(
							MainActivity.this, R.array.dishes, R.layout.spinner_text_white);
					dishAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					dish.setAdapter(dishAdapter);
					
					// Setting listeners
					AdapterView.OnItemSelectedListener onSelectListener = new AdapterView.OnItemSelectedListener() {
						@Override
						public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
							// Inflating recipe list
							Cursor cursor = db.getRecipesExcept(dish.getSelectedItem().toString(), recipesToExclude());
							ArrayAdapter<CharSequence> recipeAdapter = new ArrayAdapter<CharSequence>(
									MainActivity.this, R.layout.spinner_text_white);
							try {
								if (cursor.getCount() > 0) while (cursor.moveToNext())
									recipeAdapter.add(cursor.getString(cursor.getColumnIndex(RECIPES.name)));
								else recipeAdapter.add(getString(R.string.recipesNoRecipes));
							} finally {
								cursor.close();
							}
							recipeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
							recipe.setAdapter(recipeAdapter);
						}
						@Override public void onNothingSelected(AdapterView<?> parent) { }
					};
					meal.setOnItemSelectedListener(onSelectListener);
					dish.setOnItemSelectedListener(onSelectListener);
					
					// Dialog buttons
					builder.setNegativeButton(R.string.buttonCancel, null);
					builder.setPositiveButton(R.string.buttonSave, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Cursor cursor = db.getRecipesExcept(dish.getSelectedItem().toString(), recipesToExclude());
							ArrayList<Integer> recipesId = new ArrayList<Integer>();
							try {
								if (cursor.getCount() > 0) while (cursor.moveToNext())
									recipesId.add(cursor.getInt(cursor.getColumnIndex(RECIPES.id)));
								else return;
							} finally {
								cursor.close();
							}
							db.addMenuDish(
									new SimpleDateFormat(DatabaseInterface.DATAFORMAT).format(day),
									meal.getSelectedItem().toString(),
									recipesId.get(recipe.getSelectedItemPosition()));
							String menuDishAdded = getString(R.string.menuDishAdded)
									.replaceFirst("\\?", recipe.getSelectedItem().toString())
									.replaceFirst("\\?", meal.getSelectedItem().toString());
							Toast.makeText(MainActivity.this, menuDishAdded, Toast.LENGTH_SHORT).show();
							inflateMenuMealsList((ExpandableListView) layout.findViewById(R.id.menuMealsList));
						}
					});
					builder.show();
				}
				
				private ArrayList<Integer> recipesToExclude() {
					String[] meals = getResources().getStringArray(R.array.meals);
					ArrayList<ArrayList<Integer>> menuByMeal = new ArrayList<ArrayList<Integer>>();
					for (String meal : getResources().getStringArray(R.array.meals))
						menuByMeal.add(db.getMenu(new SimpleDateFormat(DatabaseInterface.DATAFORMAT).format(day), meal));
					ArrayList<Integer> recipesToExclude = new ArrayList<Integer>();
					for (int i = 0; i < menuByMeal.size(); i++)
						if (meals[i].equals(meal.getSelectedItem().toString()))
							recipesToExclude.addAll(menuByMeal.get(i));
					return recipesToExclude;
				}
			});
			
			// Change data buttons
			((ImageView) layout.findViewById(R.id.menuDayBefore)).setOnClickListener(new OnClickListener() {
				@Override public void onClick(View view) { day.setDate(day.getDate()-1); inflateView(layout); }
			});
			((ImageView) layout.findViewById(R.id.menuDayAfter)).setOnClickListener(new OnClickListener() {
				@Override public void onClick(View view) { day.setDate(day.getDate()+1); inflateView(layout); }
			});
			return layout;
		}
		
		// Set all the day informations including the menu list
		private void inflateView(LinearLayout layout) {
			String menuDailyMenu = day.getDate() == new Date().getDate()
					? getString(R.string.menuTodaysMenu)
					: getString(R.string.menuDailyMenu);
			
			// Set day informations
			Locale.setDefault(new Locale(getString(R.string.language)));
			((TextView) layout.findViewById(R.id.menuTodayNumber)).setText(new SimpleDateFormat("dd").format(day));
			((TextView) layout.findViewById(R.id.menuTodaysMenu)).setText(menuDailyMenu);
			((TextView) layout.findViewById(R.id.menuTodayDate)).setText(
					new SimpleDateFormat(getString(R.string.extendedDateFormat), Locale.getDefault()).format(day));
			
			// Fill menu
			inflateMenuMealsList((ExpandableListView) layout.findViewById(R.id.menuMealsList));
		}
		
		// Fill the menu list
		private void inflateMenuMealsList(final ExpandableListView menuMealsList) {
			final String[] meals = getResources().getStringArray(R.array.meals);
			final ArrayList<ArrayList<Integer>> menuByMeal = new ArrayList<ArrayList<Integer>>();
			for (String meal : getResources().getStringArray(R.array.meals))
				menuByMeal.add(db.getMenu(new SimpleDateFormat(DatabaseInterface.DATAFORMAT).format(day), meal));
			menuMealsList.setAdapter(new BaseExpandableListAdapter() {
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
					final String meal = getGroup(groupPosition).toString();
					final int recipeId = new Integer(getChild(groupPosition, childPosition).toString());
					LinearLayout recipe = (LinearLayout) getLayoutInflater().inflate(R.layout.menu_list_item, null);
					TextView recipeName = (TextView) recipe.findViewById(R.id.menuDishName);
					Cursor cursor = db.getRecipe(recipeId);
					try {
						cursor.moveToFirst();
						recipeName.setText(cursor.getString(cursor.getColumnIndex(RECIPES.name)));
					} finally {
						cursor.close();
					}
					recipe.findViewById(R.id.menuDishButton).setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(final View view) {
							// Create dialog menu
							new AlertDialog.Builder(MainActivity.this)
								.setItems(new String[] {
										getString(R.string.menuDishEatDrink),
										getString(R.string.menuDishDelete)
								}, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										final LinearLayout layout = (LinearLayout) view.getParent();
										final TextView name = (TextView) layout.findViewById(R.id.menuDishName);
										switch (which) {
										case 0:
											// Eat/drink dish
											if (db.canEatDrinkRecipe(recipeId))
												new AlertDialog.Builder(MainActivity.this)
													.setTitle(R.string.menuDishEatDrink)
													.setMessage(R.string.dialogAreYouSure)
													.setNegativeButton(R.string.buttonNo, null)
													.setPositiveButton(R.string.buttonYes, new AlertDialog.OnClickListener() {
														@Override
														public void onClick(DialogInterface dialog, int which) {
														}
													}).show();
											else
												new AlertDialog.Builder(MainActivity.this)
													.setMessage(R.string.menuDishCantEatDrink)
													.setNegativeButton(R.string.buttonCancel, null)
													.setPositiveButton(R.string.menuDishEatDrinkAnyway,
															new AlertDialog.OnClickListener() {
																@Override
																public void onClick(DialogInterface dialog, int which) {
																}
													}).show();
											break;
										case 1:
											// Delete recipe
											new AlertDialog.Builder(MainActivity.this)
												.setTitle(R.string.menuDishDelete)
												.setMessage(R.string.menuDishDeleteQuestion)
												.setNegativeButton(R.string.buttonCancel, null)
												.setPositiveButton(R.string.buttonDelete, new AlertDialog.OnClickListener() {
													@Override
													public void onClick(DialogInterface dialog, int which) {
														String dishName = name.getText().toString().trim();
														db.deleteMenuDish(
																new SimpleDateFormat(DatabaseInterface.DATAFORMAT).format(day), meal, recipeId);
														String dishDelete = getString(R.string.menuDishDeleted)
																.replaceFirst("\\?", dishName);
														Toast.makeText(MainActivity.this, dishDelete, Toast.LENGTH_SHORT).show();
														// Updating the menu page
														inflateMenuMealsList(menuMealsList);
													}
												}).show();
											break;
										}
									}
								}).show();
						}
					});
					return recipe;
				}
				// Other methods
				@Override public Object getGroup(int groupPosition) { return meals[groupPosition]; }
				@Override public long getGroupId(int groupPosition) { return groupPosition; }
				@Override public int getGroupCount() { return meals.length; }
				@Override public Object getChild(int groupPosition, int childPosition) { return menuByMeal.get(groupPosition).get(childPosition).toString(); }
				@Override public long getChildId(int groupPosition, int childPosition) { return childPosition; }
				@Override public int getChildrenCount(int groupPosition) { return menuByMeal.get(groupPosition).size(); }
				@Override public boolean isChildSelectable(int groupPosition, int childPosition) { return true; }
				@Override public boolean hasStableIds() { return true; }
			});
			// Expand all the groups
			for (int i = 0; i < meals.length; i++) menuMealsList.expandGroup(i);
		}
		
		@Override public void refresh() { }
	}
	final class InventoryPage extends Page {
		public InventoryPage() { super(R.string.inventoryPage); }
		
		// Layout
		LinearLayout layout;
		
		@Override
		public View getView() {
			// Inflating view
			layout = (LinearLayout) getLayoutInflater().inflate(R.layout.inventory_page, null);
			layout.addView(getInventoryListViewFromDatabase(), 0);
			
			// Add ingredient's button
			Button addIngredientButton = (Button) layout.findViewById(R.id.listIngredientAdd);
			addIngredientButton.setOnClickListener(new OnClickListener() {
				@Override public void onClick(View v) { showIngredientDialog(null); }
			});
			
			return layout;
		}
		
		@Override
		public void refresh() {
			layout.removeViewAt(0);
			layout.addView(getInventoryListViewFromDatabase(), 0);
		}
		
		// Return a ListView filled with all ingredients in the inventory
		public View getInventoryListViewFromDatabase() {
			// Get the data from the database
			ArrayList<Map<String, Object>> inventoryList = db.getInventory();
			
			// Check if there are any ingredients stored
			if (inventoryList.isEmpty()) {
				TextView text = new TextView(MainActivity.this);
				text.setGravity(Gravity.CENTER);
				text.setText(R.string.inventoryNoIngredients);
				text.setTextSize(20 * getResources().getDisplayMetrics().density);
				text.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
				text.setPadding(20, 20, 20, 20);
				return text;
			}
			
			// Inflate rows using SimpleAdapter
			ListView list = new ListView(MainActivity.this);
			list.setScrollingCacheEnabled(false);
			list.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
			list.setPadding(10, 10, 10, 10);
			SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, inventoryList, R.layout.ingredient_list_item,
					new String[] { INVENTORY.name, INVENTORY.quantity, INVENTORY.unit, INVENTORY.id },
					new int[] { R.id.listIngredientName, R.id.listIngredientQuantity, R.id.listIngredientUnit, R.id.listIngredientOptions });
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
											showIngredientDialog(name.getText().toString().trim());
											break;
										case 1:
											// Delete ingredient
											new AlertDialog.Builder(MainActivity.this)
												.setTitle(R.string.ingredientDelete)
												.setMessage(R.string.ingredientDeleteQuestion)
												.setNegativeButton(R.string.buttonCancel, null)
												.setPositiveButton(R.string.buttonDelete, new AlertDialog.OnClickListener() {
													@Override
													public void onClick(DialogInterface dialog, int which) {
														String ingredientName = name.getText().toString().trim();
														db.deleteInventoryIngredient(db.getInventoryIngredientID(ingredientName));
														String ingredientDeleted = getString(R.string.ingredientDeleted)
																.replaceFirst("\\?", ingredientName);
														Toast.makeText(MainActivity.this, ingredientDeleted, Toast.LENGTH_SHORT).show();
														// Updating the inventory page
														inventoryPage.refresh();
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
	}
	final class RecipesPage extends Page {
		public RecipesPage() { super(R.string.recipesPage); }
		
		@Override
		public View getView() {
			// Setting view
			LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.recipes_page, null);
			final ListView list = (ListView) layout.findViewById(R.id.recipesDishesList);
			final ListView search = (ListView) layout.findViewById(R.id.recipesSearchList);
			final EditText searchField = (EditText) layout.findViewById(R.id.recipesSearch);
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
					String[] from = new String[] { RECIPES.id, RECIPES.name, RECIPES.readyTime };
					try { recipesList = db.cursorToMapArray(cursor, from); }
					finally { cursor.close(); }
					SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, recipesList, R.layout.recipes_list_item, from,
							new int[] { R.id.recipesListId, R.id.recipesListName, R.id.recipesListOther });
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

		@Override public void refresh() { }
	}
	final class ShoppingListPage extends Page {
		public ShoppingListPage() { super(R.string.shoppingListPage); }
		
		// Shopping list variables
		LinearLayout layout;
		int groupByPosition = 0;
		boolean refreshed = true;
		
		@Override
		public View getView() {
			layout = (LinearLayout) getLayoutInflater().inflate(R.layout.shopping_page, null);
			final Spinner groupBySpinner = (Spinner) layout.findViewById(R.id.shoppingListGroupBy);
			
			// Set the group-by spinner
			ArrayAdapter<CharSequence> groupByAdapter = ArrayAdapter.createFromResource(
					MainActivity.this, R.array.groupBy, R.layout.shopping_page_spinner_item);
			groupByAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			groupBySpinner.setAdapter(groupByAdapter);
			groupBySpinner.setSelection(groupByPosition);
			groupBySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
					groupByPosition = position;
					// The next control is needed due to the call of the this listener method
					// every time when the view is created
					if (!refreshed) refresh();
					else refreshed = false;
				}
				@Override public void onNothingSelected(AdapterView<?> parent) { }
			});
			
			// Get the inventory list
			ArrayList<Map<String, Object>> inventoryList = db.getInventory();
			
			// Get the last shopping day (+1)
			Date lastShoppingDayPlusOne = db.getLastShoppingDay();
			lastShoppingDayPlusOne.setDate(lastShoppingDayPlusOne.getDate() + 1);
			Date lastShoppingDayPlusTwo = new Date();
			lastShoppingDayPlusTwo.setDate(lastShoppingDayPlusOne.getDate() + 1);
			
			// Set the ListView adapter
			Date tomorrow = new Date();
			tomorrow.setDate(tomorrow.getDate() + 1);
			Locale.setDefault(new Locale(getString(R.string.language)));
			ArrayList<Map<String, Object>> sectionShoppingList = new ArrayList<Map<String, Object>>();
			final GroupedListAdapter adapter = new GroupedListAdapter(MainActivity.this, R.layout.shopping_list_header);
			for (Date day = new Date(), today = new Date(), lastDayGroupBy = new Date();
					day.getDate() != lastShoppingDayPlusTwo.getDate();
					day.setDate(day.getDate() + 1)) {
				// Check if it's the last day of the group
				if (day.getDate() == lastDayGroupBy.getDate() ||
					day.getDate() == lastShoppingDayPlusOne.getDate()) {
					// if the shopping list is not empty inflate the adapter 
					if (!sectionShoppingList.isEmpty()) {
						// Create the adapter
						final SimpleAdapter sectionList = new SimpleAdapter(MainActivity.this, sectionShoppingList,
								R.layout.shopping_list_item,
								new String[] {
										"i." + INGREDIENTS.name,
										"ri." + RECIPES_INGREDIENTS.ingredientNeed,
										"ri." + RECIPES_INGREDIENTS.unit,
										"i." + INGREDIENTS.id
								}, new int[] {
										R.id.shoppingListIngredient,
										R.id.shoppingListQuantity,
										R.id.shoppingListUnit,
										R.id.shoppingListInformations
								});
						sectionList.setViewBinder(new SimpleAdapter.ViewBinder() {
							@Override
							public boolean setViewValue(View view, final Object data, String textRepresentation) {
								if (view.getId() != R.id.shoppingListInformations) return false;
								((ImageView) view).setOnClickListener(new OnClickListener() {
									@Override
									@SuppressWarnings("unchecked")
									public void onClick(View view) {
										// Creating the dialog to show where the ingredient is need
										View dialogView = getLayoutInflater().inflate(R.layout.shopping_list_dialog, null);
										AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
										builder.setView(dialogView);
										
										// Get the ingredient informations
										int i = 0;
										for (i = 0; i < sectionList.getCount(); i++)
											if (((Map<String, Object>) sectionList.getItem(i)).get("i." + INGREDIENTS.id)
													.toString().equals(data.toString()))
												break;
										if (i == sectionList.getCount()) return;
										final Map<String, Object> ingredient = (Map<String, Object>) sectionList.getItem(i);
										String[] dates = ingredient.get("m." + MENU.date).toString().split("\\|");
										String[] meals = ingredient.get("m." + MENU.meal).toString().split("\\|");
										String[] recipes = ingredient.get("r." + RECIPES.name).toString().split("\\|");
										
										// Fill the ListView
										GroupedListAdapter shoppingListAdapter = new GroupedListAdapter(
												MainActivity.this, R.layout.shopping_list_dialog_header);
										for (int m = 0; m < meals.length; m++) {
											ArrayList<Map<String, Object>> recipesList = new ArrayList<Map<String,Object>>();
											Map<String, Object> recipeName = new HashMap<String, Object>();
											recipeName.put(RECIPES.name, recipes[m]);
											recipesList.add(recipeName);
											Date date = null;
											SimpleDateFormat format = new SimpleDateFormat(DatabaseInterface.DATAFORMAT);
											try { date = format.parse(dates[m]); }
											catch (ParseException e) { }
											shoppingListAdapter.addSection(meals[m].toUpperCase() + " (" +
													new SimpleDateFormat(getString(R.string.extendedDateFormat)).format(date) + ")",
													new SimpleAdapter(MainActivity.this, recipesList, R.layout.shopping_list_dialog_item,
															new String[] { RECIPES.name }, new int[] { R.id.text1 })
											);
										}
										((ListView) dialogView.findViewById(R.id.shoppingListDialogList)).setAdapter(shoppingListAdapter);
										
										// Show the dialog
										builder.setTitle(ingredient.get("i." + INGREDIENTS.name).toString());
										builder.setNegativeButton(R.string.buttonClose, null);
										builder.setPositiveButton(R.string.ingredientAdd, new DialogInterface.OnClickListener() {
											@Override
											public void onClick(DialogInterface dialog, int which) {
												showIngredientDialog(ingredient.get("i." + INGREDIENTS.name).toString().trim());
											}
										});
										builder.show();
									}
								});
								return true;
							}
						});
						
						// Set the header for this section
						String header;
						Date headerDate = removeDays(lastDayGroupBy);
						if (headerDate.getDate() == today.getDate()) header = getString(R.string.today); else
						if (headerDate.getDate() == tomorrow.getDate()) header = getString(R.string.tomorrow);
						else header = new SimpleDateFormat(getString(R.string.extendedDateFormat)).format(headerDate);
						adapter.addSection(header, sectionList);
					}
					// New section list
					lastDayGroupBy = addDays(lastDayGroupBy);
					sectionShoppingList = new ArrayList<Map<String, Object>>();
				}
				// Get the shopping list for this day
				String header = new SimpleDateFormat(DatabaseInterface.DATAFORMAT).format(day);
				mergeShoppingLists(sectionShoppingList, db.getShoppingList(header, inventoryList));
			}
			
			// Inflate the spinner or notify the shopping list is empty
			ListView listView = (ListView) layout.findViewById(R.id.shoppingList);
			if (adapter.getCount() > 0) listView.setAdapter(adapter);
			else {
				TextView text = new TextView(MainActivity.this);
				text.setGravity(Gravity.CENTER);
				text.setText(R.string.shoppingListNoIngredients);
				text.setTextSize(20 * getResources().getDisplayMetrics().density);
				text.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
				text.setPadding(20, 20, 20, 20);
				int index = layout.indexOfChild(listView);
				layout.removeView(listView);
				layout.addView(text, index);
			}
			
			return layout;
		}
		
		@Override
		public void refresh() {
			refreshed = true;
			if (layout != null) {
				ViewGroup parent = (ViewGroup) layout.getParent();
				int index = parent.indexOfChild(layout);
				parent.removeView(layout);
				parent.addView(getView(), index);
			}
		}
		
		// Remove days to a give date
		private Date addDays(Date date) {
			Date newDate = new Date();
			if (groupByPosition == 0) newDate.setDate(date.getDate() + 1);   // Day
			if (groupByPosition == 1) newDate.setDate(date.getDate() + 7);   // Week
			if (groupByPosition == 2) newDate.setMonth(date.getMonth() + 1); // Month
			return newDate;
		}
		
		// Remove days to a give date
		private Date removeDays(Date date) {
			Date newDate = new Date();
			if (groupByPosition == 0) newDate.setDate(date.getDate() - 1);   // Day
			if (groupByPosition == 1) newDate.setDate(date.getDate() - 7);   // Week
			if (groupByPosition == 2) newDate.setMonth(date.getMonth() - 1); // Month
			return newDate;
		}
		
		// Merge two shopping lists
		private void mergeShoppingLists(ArrayList<Map<String, Object>> oldList, ArrayList<Map<String, Object>> newList) {
			for (Map<String, Object> newItem : newList) {
				Map<String, Object> listItem = db.isIngredientInList(
						newItem.get("i." + INGREDIENTS.id).toString(), oldList, "i." + INGREDIENTS.id);
				if (listItem != null) db.addShoppingListIngredient(listItem, newItem);
				else oldList.add(newItem);
			}
		}
	}
	
	// Open the ingredient dialog
	@SuppressWarnings("unchecked")
	public void showIngredientDialog(String updateIngredientName) {
		int maxID = db.getMaxInventoryIngredientID() + 1;
		final int ingredientID;
		
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
		ArrayAdapter<String> textAdapter = new ArrayAdapter<String>(
				MainActivity.this, android.R.layout.simple_dropdown_item_1line);
		try {
			while (cursor.moveToNext())
				textAdapter.add(cursor.getString(cursor.getColumnIndex(INGREDIENTS.name)));
		} finally {
			cursor.close();
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
						if (calendar.before(Calendar.getInstance()))
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
		final StringBuffer ingredient = new StringBuffer();
		if (updateIngredientName != null) {
			name.setText(updateIngredientName);
			name.setAdapter(null);
			cursor = db.getInventoryIngredient(updateIngredientName);
			if (cursor.getCount() > 0) {
				builder.setTitle(R.string.ingredientUpdate);
				ingredient.append(updateIngredientName);
				String unitString;
				double quantityDouble;
				long expirationDateLong;
				try {
					cursor.moveToFirst();
					ingredientID = cursor.getInt(cursor.getColumnIndex(INVENTORY.id));
					unitString = cursor.getString(cursor.getColumnIndex(INVENTORY.unit));
					quantityDouble = cursor.getDouble(cursor.getColumnIndex(INVENTORY.quantity));
					expirationDateLong = cursor.getLong(cursor.getColumnIndex(INVENTORY.expirationDate));
				} finally {
					cursor.close();
				}
				quantity.setText(Double.toString(quantityDouble));
				unit.setSelection(((ArrayAdapter<String>) unit.getAdapter()).getPosition(unitString));
				if (expirationDateLong > 0) {
					Date date = new Date(expirationDateLong);
					expirationDate.setText(DateFormat.format(getString(R.string.simpleDateFormat), date.getTime()));
					Calendar expirationDateCalendar = Calendar.getInstance();
					expirationDateCalendar.setTime(new Date(expirationDateLong));
					if (expirationDateCalendar.before(Calendar.getInstance()))
						expirationDate.getBackground().setColorFilter(
								new PorterDuffColorFilter(Color.rgb(255, 102, 0), PorterDuff.Mode.SRC_ATOP));
					removeExpirationDate.setVisibility(View.VISIBLE);
				}
			} else {
				ingredientID = maxID;
			}
		} else ingredientID = maxID;
		
		// Setting buttons and open the dialog
		builder.setNegativeButton(R.string.buttonCancel, null);
		builder.setPositiveButton(R.string.buttonSave, new DialogInterface.OnClickListener() {
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
						.setNegativeButton(R.string.buttonCancel, null)
						.setPositiveButton(R.string.buttonSave, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								db.deleteInventoryIngredient(existingIngredientID);
								// New ingredient replace existing one
								if (ingredient.length() == 0) db.insertInventoryIngredient(
										ingredientName, ingredientQuantity, ingredientUnit, ingredientExpirationDate);
								// Existing ingredient replace another one
								else db.updateInventoryIngredient(ingredientID,
										ingredientName, ingredientQuantity, ingredientUnit, ingredientExpirationDate);
								String ingredientUpdate = getString(R.string.ingredientUpdated).replaceFirst("\\?", ingredientName);
								Toast.makeText(MainActivity.this, ingredientUpdate, Toast.LENGTH_SHORT).show();
								// Updating the inventory page
								inventoryPage.refresh();
								// Updating the shopping list page
								shoppingListPage.refresh();
							}
						}).show();
				} else {
					if (ingredient.length() == 0) {
						// Insert new ingredient
						db.insertInventoryIngredient(ingredientName, ingredientQuantity, ingredientUnit, ingredientExpirationDate);
						String ingredientAdded = getString(R.string.ingredientAdded).replaceFirst("\\?", ingredientName);
						Toast.makeText(MainActivity.this, ingredientAdded, Toast.LENGTH_SHORT).show();
					} else {
						// Update existing ingredient
						db.updateInventoryIngredient(ingredientID, ingredientName, ingredientQuantity, ingredientUnit, ingredientExpirationDate);
						String ingredientUpdate = getString(R.string.ingredientUpdated).replaceFirst("\\?", ingredientName);
						Toast.makeText(MainActivity.this, ingredientUpdate, Toast.LENGTH_SHORT).show();
					}
					// Updating the inventory page
					inventoryPage.refresh();
					// Updating the shopping list page
					shoppingListPage.refresh();
				}
			}
		});
		builder.show();
	}
}
