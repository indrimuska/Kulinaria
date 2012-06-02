package com.indrimuska.kulinaria;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
 
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
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
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ImageView;
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
	private static DatabaseInterface db;
	
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
									recipeAdapter.add(cursor.getString(cursor.getColumnIndex(DatabaseInterface.RECIPES.name)));
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
									recipesId.add(cursor.getInt(cursor.getColumnIndex(DatabaseInterface.RECIPES.id)));
								else return;
							} finally {
								cursor.close();
							}
							db.addMenuDish(
									new SimpleDateFormat("yyyy-MM-dd").format(day),
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
						menuByMeal.add(db.getMenu(new SimpleDateFormat("yyyy-MM-dd").format(day), meal));
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
				menuByMeal.add(db.getMenu(new SimpleDateFormat("yyyy-MM-dd").format(day), meal));
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
						recipeName.setText(cursor.getString(cursor.getColumnIndex(DatabaseInterface.RECIPES.name)));
					} finally {
						cursor.close();
					}
					recipe.findViewById(R.id.menuDishButton).setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(final View view) {
							// Create dialog menu
							new AlertDialog.Builder(MainActivity.this)
								.setItems(new String[] {
										getString(R.string.menuDishDelete)
								}, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										final LinearLayout layout = (LinearLayout) view.getParent();
										final TextView name = (TextView) layout.findViewById(R.id.menuDishName);
										switch (which) {
										case 0:
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
																new SimpleDateFormat("yyyy-MM-dd").format(day), meal, recipeId);
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
			LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.shopping_page, null);
			
			// Get the inventory list
			Cursor cursor = db.getInventory();
			ArrayList<Map<String, Object>> inventoryList = new ArrayList<Map<String,Object>>();
			try {
				inventoryList = db.cursorToMapArray(cursor, new String[] {
						DatabaseInterface.INVENTORY.id,
						DatabaseInterface.INVENTORY.name,
						DatabaseInterface.INVENTORY.quantity,
						DatabaseInterface.INVENTORY.unit
				});
			} finally {
				cursor.close();
			}
			
			// Set the ListView adapter
			String dayString;
			Date day = new Date();
			ArrayList<Map<String, Object>> dailyShoppingList;
			Locale.setDefault(new Locale(getString(R.string.language)));
			final GroupedListAdapter adapter = new GroupedListAdapter(MainActivity.this, R.layout.shopping_list_header);
			while (!(dailyShoppingList = db.getShoppingList(
						dayString = new SimpleDateFormat("yyyy-MM-dd").format(day), inventoryList)).isEmpty()) {
				final SimpleAdapter dailyList = new SimpleAdapter(MainActivity.this, dailyShoppingList, R.layout.shopping_list_item,
						new String[] {
								"i." + DatabaseInterface.INGREDIENTS.name,
								"ri." + DatabaseInterface.RECIPES_INGREDIENTS.ingredientNeed,
								"ri." + DatabaseInterface.RECIPES_INGREDIENTS.unit,
								"i." + DatabaseInterface.INGREDIENTS.id
						}, new int[] {
								R.id.shoppingListIngredient,
								R.id.shoppingListQuantity,
								R.id.shoppingListUnit,
								R.id.shoppingListInformations
						});
				dailyList.setViewBinder(new SimpleAdapter.ViewBinder() {
					@Override
					public boolean setViewValue(View view, final Object data, String textRepresentation) {
						if (view.getId() != R.id.shoppingListInformations) return false;
						Log.d(pageName, "textRepresentation=" + textRepresentation);
						((ImageView) view).setOnClickListener(new OnClickListener() {
							@Override
							@SuppressWarnings("unchecked")
							public void onClick(View view) {
								// Creating the dialog
								View dialogView = getLayoutInflater().inflate(R.layout.shopping_list_dialog, null);
								AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
								builder.setView(dialogView);
								
								// Get the ingredient informations
								int i = 0;
								for (i = 0; i < dailyList.getCount(); i++)
									if (((Map<String, Object>) dailyList.getItem(i)).get("i." + DatabaseInterface.INGREDIENTS.id)
											.toString().equals(data.toString()))
										break;
								if (i == dailyList.getCount()) return;
								Map<String, Object> ingredient = (Map<String, Object>) dailyList.getItem(i);
								String[] meals = ingredient.get("m." + DatabaseInterface.MENU.meal).toString().split("\\|");
								String[] recipes = ingredient.get("r." + DatabaseInterface.RECIPES.name).toString().split("\\|");
								
								// Fill the ListView
								GroupedListAdapter shoppingListAdapter = new GroupedListAdapter(
										MainActivity.this, R.layout.shopping_list_dialog_header);
								for (int m = 0; m < meals.length; m++) {
									ArrayList<Map<String, Object>> recipesList = new ArrayList<Map<String,Object>>();
									Map<String, Object> recipeName = new HashMap<String, Object>();
									recipeName.put(DatabaseInterface.RECIPES.name, recipes[m]);
									recipesList.add(recipeName);
									shoppingListAdapter.addSection(meals[m].toUpperCase(),
											new SimpleAdapter(MainActivity.this, recipesList, R.layout.shopping_list_dialog_item,
													new String[] { DatabaseInterface.RECIPES.name }, new int[] { R.id.text1 })
									);
								}
								((ListView) dialogView.findViewById(R.id.shoppingListDialogList)).setAdapter(shoppingListAdapter);
								
								// Show the dialog
								builder.setTitle(ingredient.get("i." + DatabaseInterface.INGREDIENTS.name).toString());
								builder.setNegativeButton(R.string.buttonClose, null);
								builder.setPositiveButton(R.string.ingredientAdd, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										new IngredientDialog(null).show(null);
									}
								});
								builder.show();
							}
						});
						return true;
					}
				});
				Date tomorrow = new Date();
				tomorrow.setDate(tomorrow.getDate() + 1);
				if (day.getDate() == new Date().getDate()) dayString = getString(R.string.today); else
				if (day.getDate() == tomorrow.getDate()) dayString = getString(R.string.tomorrow);
				else dayString = new SimpleDateFormat(getString(R.string.extendedDateFormat)).format(day);
				adapter.addSection(dayString, dailyList);
				day.setDate(day.getDate() + 1);
			}
			((ListView) layout.findViewById(R.id.shoppingList)).setAdapter(adapter);
			
			return layout;
		}
		
		private class GroupedListAdapter extends BaseAdapter {
			Map<String, Adapter> sections = new LinkedHashMap<String, Adapter>();
			ArrayAdapter<String> headers;
			
			static final int HEADER_VIEW_TYPE = 0;
			
			GroupedListAdapter(Context context, int header) {
				headers = new ArrayAdapter<String>(context, header);
			}
			
			public void addSection(String header, Adapter child) {
				headers.add(header);
				sections.put(header, child);
			}
			
			@Override
			public int getCount() {
				int elements = 0;
				for (Adapter section : sections.values()) elements += section.getCount() + 1;
				return elements;
			}
			
			@Override
			public int getViewTypeCount() {
				int items = 1;
				for (Adapter adapter : sections.values()) items += adapter.getViewTypeCount();
				return items;
			}
			
			@Override
			public Object getItem(int position) {
				for (Adapter section : sections.values()) {
					int size = section.getCount() + 1;
					// check if position inside this section
					if (position == 0) return section;
					if (position < size) return section.getItem(position - 1);
					// otherwise jump into next section
					position -= size;
				}
				return null;
			}
			
			@Override
			public long getItemId(int position) {
				return position;
			}
			
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				int section_number = 0;
				for (Adapter section : sections.values()) {
					int size = section.getCount() + 1;
					// check if position inside this section
					if (position == 0) return headers.getView(section_number, convertView, parent);
					if (position < size) return section.getView(position - 1, convertView, parent);
					// otherwise jump into next section
					position -= size;
					section_number++;
				}
				return null;
			}
			
			@Override
			public int getItemViewType(int position) {
				int type = 1;
				for (Adapter section : sections.values()) {
					int size = section.getCount() + 1;
					// check if position inside this section
					if (position == 0) return HEADER_VIEW_TYPE;
					if (position < size) return type + section.getItemViewType(position - 1);
					// otherwise jump into next section
					position -= size;
					type += section.getViewTypeCount();
				}
				return -1;
			}
			
			@Override
			public boolean isEnabled(int position) {
				return getItemViewType(position) != HEADER_VIEW_TYPE;
			}
		}
	}
	
	// Open the ingredient dialog
	class IngredientDialog {
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
			ArrayAdapter<String> textAdapter = new ArrayAdapter<String>(
					MainActivity.this, android.R.layout.simple_dropdown_item_1line);
			try {
				while (cursor.moveToNext())
					textAdapter.add(cursor.getString(cursor.getColumnIndex(DatabaseInterface.INGREDIENTS.name)));
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
									if (ingredientView == null) db.insertInventoryIngredient(
											ingredientName, ingredientQuantity, ingredientUnit, ingredientExpirationDate);
									// Existing ingredient replace another one
									else db.updateInventoryIngredient(ingredientID,
											ingredientName, ingredientQuantity, ingredientUnit, ingredientExpirationDate);
									String ingredientUpdate = getString(R.string.ingredientUpdated).replaceFirst("\\?", ingredientName);
									Toast.makeText(MainActivity.this, ingredientUpdate, Toast.LENGTH_SHORT).show();
									// Updating the inventory page
									if (layout != null) {
										layout.removeViewAt(0);
										layout.addView(getInventoryListViewFromDatabase(), 0);
									}
								}
							}).show();
					} else {
						if (ingredientView == null) {
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
						if (layout != null) {
							layout.removeViewAt(0);
							layout.addView(getInventoryListViewFromDatabase(), 0);
						}
					}
				}
			});
			dialog = builder.create();
			dialog.show();
		}
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
				TextView text = new TextView(this);
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
		
		// Inflate rows using SimpleAdapter
		ListView list = new ListView(this);
		list.setScrollingCacheEnabled(false);
		list.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1));
		list.setPadding(10, 10, 10, 10);
		SimpleAdapter adapter = new SimpleAdapter(this, inventoryList, R.layout.ingredient_list_item, from,
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
										new IngredientDialog((LinearLayout) layout.getParent().getParent()).show(name);
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
}
