package com.indrimuska.kulinaria;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseInterface {
	static final String TAG = DatabaseInterface.class.getSimpleName();
	
	static final int VERSION = 1;
	static final String DATABASE = "kulinaria.db";
	
	public static final String DATAFORMAT = "yyyy-MM-dd";
	
	// Tables definition
	public static final class INVENTORY {
		static final String TABLE			= "Inventory";
		static final String id				= "_id";
		static final String name			= "name";
		static final String quantity		= "quantity";
		static final String unit			= "unit";
		static final String expirationDate	= "expirationDate";
		static final String ORDER_BY		= name + " ASC";
		static final String CREATE =
				"create table if not exists " + TABLE + " ( " +
						id + " integer primary key autoincrement, " +
						name + " text, " +
						quantity + " float, " +
						unit + " text, " +
						expirationDate + " int )";
	}
	public static final class RECIPES {
		static final String TABLE			= "Recipes";
		static final String id				= "_id";
		static final String name			= "name";
		static final String dish			= "dish";
		static final String preparationTime	= "preparationTime";
		static final String readyTime		= "readyTime";
		static final String servings		= "servings";
		static final String description		= "description";
		static final String ORDER_BY		= name + " ASC";
		static final String CREATE =
				"create table if not exists " + TABLE + " ( " +
						id + " integer primary key autoincrement, " +
						name + " text, " +
						dish + " text, " +
						preparationTime + " int, " +
						readyTime + " int, " +
						servings + " int, " +
						description + " text )";
	}
	public static final class RECIPES_INGREDIENTS {
		static final String TABLE			= "RecipesIngredients";
		static final String recipeId		= "recipeId";
		static final String ingredient		= "ingredient";
		static final String ingredientNeed	= "ingredientNeed";
		static final String unit			= "unit";
		static final String ORDER_BY		= recipeId + " ASC";
		static final String CREATE =
				"create table if not exists " + TABLE + " ( " +
						recipeId + " int, " +
						ingredient + " text, " +
						ingredientNeed + " float, " +
						unit + " text, " +
						"primary key( " +
							recipeId + ", " +
							ingredient + " ) " +
						")";
	}
	public static final class MENU {
		static final String TABLE		= "Menu";
		static final String date		= "date";
		static final String meal		= "meal";
		static final String recipeId	= "recipeId";
		static final String eatenDrunk	= "eatenDrunk";
		static final String ORDER_BY	= date + " ASC";
		static final String CREATE =
				"create table if not exists " + TABLE + " (" +
						date +" date, " +
						meal + " text, " +
						recipeId + " int, " +
						eatenDrunk + " tinyint, " +
						"primary key( " +
							date + ", " +
							meal + ", " +
							recipeId + " ) " +
						")";
	}
	public static final class SHOPPING_LIST {
		static final String TABLE	= "ShoppingList";
		static final String CREATE = "";
	}
	
	// DbHelper implementation
	class DbHelper extends SQLiteOpenHelper {
		Context context;
		
		// Constructor
		public DbHelper(Context context) {
			super(context, DATABASE, null, VERSION);
			this.context = context;
		}
		
		// Called only once, first time the DB is created
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(INVENTORY.CREATE);
			db.execSQL(RECIPES.CREATE);
			db.execSQL(RECIPES_INGREDIENTS.CREATE);
			db.execSQL(MENU.CREATE);
			Log.d(TAG, "tables created");
			
			// Populate database (from XML)
			Map<String, ArrayList<ArrayList<String>>> tables = getTablesFromXML(context, R.raw.populate_db);
			for (Map.Entry<String, ArrayList<ArrayList<String>>> table : tables.entrySet()) {
				ArrayList<ArrayList<String>> rows = table.getValue();
				for (ArrayList<String> value : rows) {
					if (table.getKey().equals(INVENTORY.TABLE))
						db.insert(INVENTORY.TABLE, null, inventoryIngredientContentValues(
								value.get(0), Float.parseFloat(value.get(1)), value.get(2), Long.parseLong(value.get(3))));
					if (table.getKey().equals(RECIPES.TABLE))
						db.insert(RECIPES.TABLE, null, recipeContentValues(
								value.get(0), value.get(1), Integer.parseInt(value.get(2)), Integer.parseInt(value.get(3)),
								Integer.parseInt(value.get(4)), value.get(5)));
					if (table.getKey().equals(RECIPES_INGREDIENTS.TABLE))
						db.insert(RECIPES_INGREDIENTS.TABLE, null, recipeIngredientContentValues(
								Integer.parseInt(value.get(0)), value.get(1), Float.parseFloat(value.get(2)), value.get(3)));
				}
			}
		}
		
		// Called whenever newVersion != oldVersion
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			onUpgradeTable(db, INVENTORY.TABLE, INVENTORY.CREATE);
			onUpgradeTable(db, RECIPES.TABLE, RECIPES.CREATE);
			onUpgradeTable(db, RECIPES_INGREDIENTS.TABLE, RECIPES_INGREDIENTS.CREATE);
			onUpgradeTable(db, MENU.TABLE, MENU.CREATE);
		}
		
		// General onUpgrade (valid for each table)
		private void onUpgradeTable(SQLiteDatabase db, String tableName, String createTable) {
			// put in a list the existing columns
			List<String> columns = getColumns(db, tableName);
			// backup table
			db.execSQL("alter table " + tableName + " rename to temp_" + tableName);
			// create new table
			db.execSQL(createTable);
			// get the intersection with the new columns, this time columns taken from the upgraded table 
			columns.retainAll(getColumns(db, tableName));
			// restore data
			String cols = join(columns, ",");
			db.execSQL(String.format("insert into %s (%s) select %s from temp_%s", tableName, cols, cols, tableName));
			// remove backup table
			db.execSQL("drop table temp_" + tableName);
			Log.d(TAG, "database updated");
		}
		
		// Returns column names of a table
		private List<String> getColumns(SQLiteDatabase db, String tableName) {
			List<String> columns = null;
			Cursor c = null;
			try {
				c = db.rawQuery("select * from " + tableName + " limit 1", null);
				if (c != null) columns = new ArrayList<String>(Arrays.asList(c.getColumnNames()));
			} catch (Exception e) {
				Log.e(TAG, "getColumns: " + e.getMessage(), e);
				e.printStackTrace();
			} finally {
				if (c != null) c.close();
			}
			return columns;
		}
		
		// Convert a List to a String with a delimiter string
		private String join(List<String> list, String delimiter) {
			StringBuilder buf = new StringBuilder();
			int num = list.size();
			for (int i = 0; i < num; i++) {
				if (i != 0) buf.append(delimiter);
				buf.append((String) list.get(i));
			}
			return buf.toString();
		}
		
		// Get table contents from an XML file
		private Map<String, ArrayList<ArrayList<String>>> getTablesFromXML(Context context, int XML) {
			Map<String, ArrayList<ArrayList<String>>> tables = new HashMap<String, ArrayList<ArrayList<String>>>();
			try {
				// Getting XML contents
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
				Document doc = documentBuilder.parse(context.getResources().openRawResource(XML));
				doc.getDocumentElement().normalize();
				
				// Save XML content into ArrayList<String>
				NodeList tableList = doc.getElementsByTagName("table");
				for (int t = 0; t < tableList.getLength(); t++) {
					ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
					NodeList rowList = ((Element) tableList.item(t)).getElementsByTagName("row");
					for (int r = 0; r < rowList.getLength(); r++) {
						ArrayList<String> values = new ArrayList<String>();
						NodeList columns = ((Element) rowList.item(r)).getChildNodes();
						for (int c = 0; c < columns.getLength(); c++) {
							Node column = columns.item(c);
							if (column.getNodeType() == 1)
								values.add(column.getChildNodes().item(0).getNodeValue());
						}
						rows.add(values);
					}
					Log.d(TAG, ((Element) tableList.item(t)).getAttribute("name"));
					tables.put(((Element) tableList.item(t)).getAttribute("name"), rows);
				}
			} catch (Exception e) {
				Log.e(TAG, e.getMessage(), e);
			}
			return tables;
		}
	}
	
	final DbHelper dbHelper;
	
	public DatabaseInterface(Context context) {
		dbHelper = new DbHelper(context);
		Log.i(TAG, "Initialized data");
	}
	
	public void close() {
		dbHelper.close();
	}
	
	// Convert cursor to map-array
	public ArrayList<Map<String, Object>> cursorToMapArray(Cursor cursor, String[] columns) {
		ArrayList<Map<String, Object>> array = new ArrayList<Map<String, Object>>();
		while (cursor.moveToNext()) {
			Map<String, Object> element = new HashMap<String, Object>();
			for (String column : columns) element.put(column, cursor.getString(cursor.getColumnIndex(column)));
			array.add(element);
		}
		return array;
	}
	
	// Convert cursor to map-array using indexes
	public ArrayList<Map<String, Object>> cursorToMapArrayIndexed(Cursor cursor, String[] columns) {
		ArrayList<Map<String, Object>> array = new ArrayList<Map<String, Object>>();
		while (cursor.moveToNext()) {
			Map<String, Object> element = new HashMap<String, Object>();
			for (int i = 0; i < columns.length; i++) element.put(columns[i], cursor.getString(i));
			array.add(element);
		}
		return array;
	}
	
	// Convert inventory ingredient informations to ContentValues
	private ContentValues inventoryIngredientContentValues(String name, float quantity, String unit, long expirationDate) {
		ContentValues values = new ContentValues();
		values.put(INVENTORY.name, name);
		values.put(INVENTORY.quantity, quantity);
		values.put(INVENTORY.unit, unit);
		values.put(INVENTORY.expirationDate, expirationDate);
		return values;
	}
	
	// Insert an ingredient in database
	public void insertInventoryIngredient(String name, float quantity, String unit, long expirationDate) {
		Log.d(TAG, "insertInventoryIngredient: " + name + "," + quantity + "," + unit + "," + expirationDate);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try { db.insert(INVENTORY.TABLE, null, inventoryIngredientContentValues(name, quantity, unit, expirationDate)); }
		finally { db.close(); }
	}
	
	// Get max ingredient ID from inventory
	public int getMaxInventoryIngredientID() {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			Cursor cursor = db.query(INVENTORY.TABLE,new String[] { "max(" + INVENTORY.id + ")" },
					null, null, null, null, null);
			try {
				cursor.moveToFirst();
				return cursor.getInt(0);
			} finally {
				cursor.close();
			}
		} finally {
			db.close();
		}
	}
	
	// Get ingredient from inventory
	public Cursor getInventoryIngredient(String ingredientName) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		return db.query(INVENTORY.TABLE, null, INVENTORY.name+"=?", new String[] { ingredientName }, null, null, null);
	}
	
	// Get ingredient ID
	public int getInventoryIngredientID(String ingredientName) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			Cursor cursor = db.query(INVENTORY.TABLE, new String[] { INVENTORY.id },
					INVENTORY.name+"=?", new String[] { ingredientName }, null, null, null);
			try {
				cursor.moveToFirst();
				return cursor.getInt(0);
			} finally {
				cursor.close();
			}
		} finally {
			db.close();
		}
	}
	
	// Get the list of ingredients stored in the inventory
	public ArrayList<Map<String, Object>> getInventory() {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		String[] columns = { INVENTORY.id, INVENTORY.name, INVENTORY.quantity, INVENTORY.unit };
		Cursor cursor = db.query(INVENTORY.TABLE, null, null, null, null, null, INVENTORY.ORDER_BY);
		try { return cursorToMapArray(cursor, columns); }
		finally { cursor.close(); }
	}
	
	// Check if an ingredient is already stored
	public boolean inventoryIngredientAlreadyExists(String ingredientName) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			Cursor cursor = db.query(INVENTORY.TABLE, null,
					INVENTORY.name+"=?", new String[] { ingredientName }, null, null, null);
			try {
				return cursor.getCount() > 0;
			} finally {
				cursor.close();
			}
		} finally {
			db.close();
		}
	}
	
	// Update a stored ingredient
	public void updateInventoryIngredient(int id, String name, float quantity, String unit, long expirationDate) {
		Log.d(TAG, "updateInventoryIngredient: " + name + "," + quantity + "," + unit + "," + expirationDate);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			ContentValues values = inventoryIngredientContentValues(name, quantity, unit, expirationDate);
			db.update(INVENTORY.TABLE, values, INVENTORY.id+"=?", new String[] { Integer.toString(id) });
		} finally {
			db.close();
		}
	}
	
	// Update the amount of an ingredient stored in the inventory
	public void updateInventoryIngredientQuantity(int id, float quantity) {
		Log.d(TAG, "updateInventoryIngredientQuantity: " + Integer.toString(id) + "," + Float.toString(quantity));
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(INVENTORY.quantity, quantity);
			db.update(INVENTORY.TABLE, values, INVENTORY.id+"=?", new String[] { Integer.toString(id) });
		} finally {
			db.close();
		}
	}
	
	// Delete a stored ingredient
	public void deleteInventoryIngredient(int id) {
		Log.d(TAG, "deleteInventoryIngredient: " + id);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			db.delete(INVENTORY.TABLE, INVENTORY.id+"=?", new String[] { Integer.toString(id) });
		} finally {
			db.close();
		}
	}
	
	// Get the list of ingredients stored
	public ArrayList<String> getIngredients() {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		ArrayList<String> ingredients = new ArrayList<String>();
		Cursor cursor = db.rawQuery(
				"select distinct " + RECIPES_INGREDIENTS.ingredient + " " +
				"from " + RECIPES_INGREDIENTS.TABLE + " " +
				"order by " + RECIPES_INGREDIENTS.ingredient + " asc", null);
		try {
			while (cursor.moveToNext())
				ingredients.add(cursor.getString(cursor.getColumnIndex(RECIPES_INGREDIENTS.ingredient)));
		} finally {
			cursor.close();
		}
		return ingredients;
	}
	
	// Convert recipe informations to ContentValues
	private ContentValues recipeContentValues(String name, String dish, int preparationTime, int readyTime,
			int servings, String description) {
		ContentValues values = new ContentValues();
		values.put(RECIPES.name, name);
		values.put(RECIPES.dish, dish);
		values.put(RECIPES.preparationTime, preparationTime);
		values.put(RECIPES.readyTime, readyTime);
		values.put(RECIPES.servings, servings);
		values.put(RECIPES.description, description);
		return values;
	}
	
	// Get the number of recipes for a dish
	public int getRecipeCount(String dish) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			Cursor cursor = db.query(RECIPES.TABLE, null, RECIPES.dish+"=?", new String[] { dish }, null, null, null);
			try {
				return cursor.moveToFirst() ? cursor.getCount() : 0;
			} finally {
				cursor.close();
			}
		} finally {
			db.close();
		}
	}
	
	// Get all the recipes for a dish
	public Cursor getRecipes(String dish) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		return db.query(RECIPES.TABLE, null, RECIPES.dish+"=?", new String[] { dish }, null, null, RECIPES.ORDER_BY);
	}
	
	// Get all the recipes for a dish except a specific subset
	public Cursor getRecipesExcept(String dish, ArrayList<Map<String, Object>> recipes) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		String selectString = RECIPES.dish+"=?";
		ArrayList<String> selectValuesArray = new ArrayList<String>();
		selectValuesArray.add(dish);
		for (Map<String, Object> recipe : recipes) {
			selectString += " and "+RECIPES.id+"!=?";
			selectValuesArray.add(recipe.get(MENU.recipeId).toString());
		}
		String[] selectValues = new String[selectValuesArray.size()];
		selectValuesArray.toArray(selectValues);
		return db.query(RECIPES.TABLE, null, selectString, selectValues, null, null, RECIPES.ORDER_BY);
	}
	
	// Get recipe informations
	public Cursor getRecipe(int recipeId) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		return db.query(RECIPES.TABLE, null, RECIPES.id+"=?", new String[] { Integer.toString(recipeId) }, null, null, null);
	}
	
	// Search one or more recipes in database
	public Cursor searchRecipe(String name) {
		Log.d(TAG, "searchRecipe: " + name);
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		return db.query(RECIPES.TABLE, null, RECIPES.name+" like ?", new String[] { '%'+name+'%' }, null, null, null);
	}
	
	// Convert recipe-ingredient informations to ContentValues
	private ContentValues recipeIngredientContentValues(int recipeId, String ingredient, float ingredientNeed, String unit) {
		ContentValues values = new ContentValues();
		values.put(RECIPES_INGREDIENTS.recipeId, recipeId);
		values.put(RECIPES_INGREDIENTS.ingredient, ingredient);
		values.put(RECIPES_INGREDIENTS.ingredientNeed, ingredientNeed);
		values.put(RECIPES_INGREDIENTS.unit, unit);
		return values;
	}
	
	// Get all the ingredients of a recipe
	public ArrayList<Map<String, Object>> getRecipeIngredients(int recipeId) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			Cursor cursor = db.query(RECIPES_INGREDIENTS.TABLE, null, RECIPES_INGREDIENTS.recipeId+"=?",
					new String[] { Integer.toString(recipeId) }, null, null, null);
			try {
				return cursorToMapArray(cursor, new String[] {
						RECIPES_INGREDIENTS.ingredient,
						RECIPES_INGREDIENTS.ingredientNeed,
						RECIPES_INGREDIENTS.unit
				});
			} finally {
				cursor.close();
			}
		} finally {
			db.close();
		}
	}
	
	// Convert menu informations to ContentValues
	private ContentValues menuContentValues(String date, String meal, int recipeId, boolean eatenDrunk) {
		ContentValues values = new ContentValues();
		values.put(MENU.date, date);
		values.put(MENU.meal, meal);
		values.put(MENU.recipeId, recipeId);
		values.put(MENU.eatenDrunk, eatenDrunk ? "1" : "0");
		return values;
	}
	
	// Get the menu chosen for a meal
	public ArrayList<Map<String, Object>> getMenu(String date, String meal) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			Cursor cursor = db.query(MENU.TABLE, new String[] { MENU.recipeId, MENU.eatenDrunk },
					MENU.date+"=? and "+MENU.meal+"=?", new String[] { date, meal }, null, null, null);
			try {
				ArrayList<Map<String, Object>> recipes = new ArrayList<Map<String, Object>>();
				while (cursor.moveToNext()) {
					Map<String, Object> recipe = new HashMap<String, Object>();
					recipe.put(MENU.recipeId, cursor.getInt(cursor.getColumnIndex(MENU.recipeId)));
					recipe.put(MENU.eatenDrunk, cursor.getInt(cursor.getColumnIndex(MENU.eatenDrunk)));
					recipes.add(recipe);
				}
				return recipes;
			} finally {
				cursor.close();
			}
		} finally {
			db.close();
		}
	}
	
	// Delete a dish from a menu
	public void deleteMenuDish(String date, String meal, int recipeId) {
		Log.d(TAG, "deleteMenuDish: " + date + "," + meal + "," + Integer.toString(recipeId));
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			db.delete(MENU.TABLE, MENU.date+"=? and "+MENU.meal+"=? and "+MENU.recipeId+"=?",
					new String[] { date, meal, Integer.toString(recipeId) });
		} finally {
			db.close();
		}
	}
	
	// Add a dish in the menu
	public void addMenuDish(String date, String meal, int recipeId) {
		Log.d(TAG, "addMenuDish: " + date + "," + meal + "," + Integer.toString(recipeId));
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try { db.insert(MENU.TABLE, null, menuContentValues(date, meal, recipeId, false)); }
		finally { db.close(); }
	}
	
	// Set a dish as eaten/drunk
	public void eatDrinkMenuDish(String date, String meal, int recipeId) {
		Log.d(TAG, "eatDrinkMenuDish: " + date + "," + meal + "," + Integer.toString(recipeId));
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			db.update(MENU.TABLE, menuContentValues(date, meal, recipeId, true),
					MENU.date+"=? and "+MENU.meal+"=? and "+MENU.recipeId+"=?",
					new String[] { date, meal, Integer.toString(recipeId) });
		} finally {
			db.close();
		}
	}
	
	// Check if there are enough ingredients to eat a recipe
	public boolean canEatDrinkRecipe(int recipeId) {
		// Get the lists
		ArrayList<Map<String, Object>> recipeIngredients = getRecipeIngredients(recipeId);
		ArrayList<Map<String, Object>> inventoryList     = getInventory();
		
		// Iterate the lists
		for (Map<String, Object> ingredient : recipeIngredients) {
			Map<String, Object> inventoryIngredient;
			if ((inventoryIngredient = isIngredientInList(
					ingredient.get(RECIPES_INGREDIENTS.ingredient).toString(),
					inventoryList, INVENTORY.name)) == null ||
					!inventoryIngredient.get(INVENTORY.unit).toString().equals(
							ingredient.get(RECIPES_INGREDIENTS.unit).toString()) ||
					Float.parseFloat(ingredient.get(RECIPES_INGREDIENTS.ingredientNeed).toString()) >
					Float.parseFloat(inventoryIngredient.get(INVENTORY.quantity).toString()))
				return false;
		}
		return true;
	}
	
	// Delete ingredients from inventory after a menu dish has eaten/drunk
	public void eatDrinkRecipe(int recipeId) {
		// Get the lists
		ArrayList<Map<String, Object>> recipeIngredients = getRecipeIngredients(recipeId);
		ArrayList<Map<String, Object>> inventoryList     = getInventory();
		
		// Iterate the lists
		for (Map<String, Object> ingredient : recipeIngredients) {
			Map<String, Object> inventoryIngredient;
			if ((inventoryIngredient = isIngredientInList(
					ingredient.get(RECIPES_INGREDIENTS.ingredient).toString(),
					inventoryList, INVENTORY.name)) != null &&
					inventoryIngredient.get(INVENTORY.unit).toString().equals(
						ingredient.get(RECIPES_INGREDIENTS.unit).toString())) {
				Float difference =
						Float.parseFloat(inventoryIngredient.get(INVENTORY.quantity).toString()) -
						Float.parseFloat(ingredient.get(RECIPES_INGREDIENTS.ingredientNeed).toString());
				if (difference <= 0)
					deleteInventoryIngredient(Integer.parseInt(inventoryIngredient.get(INVENTORY.id).toString()));
				else updateInventoryIngredientQuantity(
						Integer.parseInt(inventoryIngredient.get(INVENTORY.id).toString()), difference);
			}
		}
	}
	
	// Get the last day of shopping
	public Date getLastShoppingDay() {
		Date last = new Date();
		ArrayList<Map<String, Object>> inventoryList = getInventory();
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATAFORMAT);
		
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor cursor = db.query(MENU.TABLE, null, MENU.date + " >= date('now') and " + MENU.eatenDrunk + " = 0",
				null, null, null, MENU.date + " asc");
		try {
			while (cursor.moveToNext()) {
				String dateString = cursor.getString(cursor.getColumnIndex(MENU.date));
				if (!getShoppingList(dateString, inventoryList).isEmpty())
					try { last = dateFormat.parse(dateString); }
					catch (ParseException e) { }
			}
		} finally {
			cursor.close();
		}
		return last;
	}
	
	// Get the shopping list for a day
	public ArrayList<Map<String, Object>> getShoppingList(String date, ArrayList<Map<String, Object>> inventoryIngredients) {
		ArrayList<Map<String, Object>> shoppingList, recipeIngredients = new ArrayList<Map<String, Object>>();
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor cursor;
		
		// Get ingredients list for each recipe
		String[] recipeColumns = new String[] {
				"m." + MENU.date,
				"m." + MENU.meal,
				"r." + RECIPES.id,
				"r." + RECIPES.name,
				"ri." + RECIPES_INGREDIENTS.ingredient,
				"ri." + RECIPES_INGREDIENTS.ingredientNeed,
				"ri." + RECIPES_INGREDIENTS.unit
		};
		String recipeColumnString = Arrays.asList(recipeColumns).toString().replaceAll("^\\[|\\]$", ", ");
		cursor = db.rawQuery(
				"select " + recipeColumnString.substring(2, recipeColumnString.length()-2) + " " +
				"from " +
						MENU.TABLE + " as m, " +
						RECIPES.TABLE + " as r, " +
						RECIPES_INGREDIENTS.TABLE + " as ri " +
				"where " +
						"m." + MENU.date + " = '" + date + "' and " +
						"m." + MENU.eatenDrunk + " = 0 and " +
						"r." + RECIPES.id + " = m." + MENU.recipeId + " and " +
						"r." + RECIPES.id + " = ri." + RECIPES_INGREDIENTS.recipeId, null);
		try { recipeIngredients = cursorToMapArrayIndexed(cursor, recipeColumns); }
		finally { cursor.close(); }
		
		// Subtracts inventory list from ingredients list
		shoppingList = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> ingredient : recipeIngredients) {
			Map<String, Object> inventoryIngredient = isIngredientInList(
					ingredient.get("ri." + RECIPES_INGREDIENTS.ingredient).toString(), inventoryIngredients, INVENTORY.name);
			if (inventoryIngredient != null &&
					inventoryIngredient.get(INVENTORY.unit).toString().equals(
							ingredient.get("ri." + RECIPES_INGREDIENTS.unit).toString())) {
				// ingredient is already in the inventory list -> check the shopping list
				float difference =
						Float.parseFloat(inventoryIngredient.get(INVENTORY.quantity).toString()) -
						Float.parseFloat(ingredient.get("ri." + RECIPES_INGREDIENTS.ingredientNeed).toString());
				inventoryIngredient.put(INVENTORY.quantity, Math.max(0, difference));
				if (difference < 0) {
					Map<String, Object> shoppingListIngredient = isIngredientInList(
							ingredient.get("ri." + RECIPES_INGREDIENTS.ingredient).toString(),
								shoppingList, "ri." + RECIPES_INGREDIENTS.ingredient);
					if (shoppingListIngredient != null) {
						// ingredient is already in the shopping list -> update quantities
						addShoppingListIngredient(shoppingListIngredient, ingredient);
						shoppingListIngredient.put("ri." + RECIPES_INGREDIENTS.ingredientNeed, -difference +
								Float.parseFloat(shoppingListIngredient.get("ri." + RECIPES_INGREDIENTS.ingredientNeed).toString()));
					} else {
						// ingredient is not in the shopping list
						Map<String, Object> newIngredient = new HashMap<String, Object>(ingredient);
						newIngredient.put("ri." + RECIPES_INGREDIENTS.ingredientNeed, -difference);
						shoppingList.add(newIngredient);
					}
				}
			} else {
				// ingredient is not in the inventory list
				Map<String, Object> shoppingListIngredient = isIngredientInList(
						ingredient.get("ri." + RECIPES_INGREDIENTS.ingredient).toString(),
							shoppingList, "ri." + RECIPES_INGREDIENTS.ingredient);
				if (shoppingListIngredient != null)
					// ingredient is already in the shopping list -> update quantities
					addShoppingListIngredient(shoppingListIngredient, ingredient);
				else
					// ingredient is not in the shopping list
					shoppingList.add(ingredient);
			}
		}
		return shoppingList;
	}
	
	// Search an ingredient inside a list
	public Map<String, Object> isIngredientInList(String ingredient, ArrayList<Map<String, Object>> list, String field) {
		for (Map<String, Object> listIngredient : list)
			if (listIngredient.get(field).toString().equals(ingredient))
				return listIngredient;
		return null;
	}
	
	// Add an ingredient into the shopping list
	public void addShoppingListIngredient(Map<String, Object> shoppingListIngredient, Map<String, Object> ingredient) {
		shoppingListIngredient.put("m." + MENU.date,
				shoppingListIngredient.get("m." + MENU.date) + "|" + ingredient.get("m." + MENU.date));
		shoppingListIngredient.put("m." + MENU.meal,
				shoppingListIngredient.get("m." + MENU.meal) + "|" + ingredient.get("m." + MENU.meal));
		shoppingListIngredient.put("r." + RECIPES.id,
				shoppingListIngredient.get("r." + RECIPES.id) + "|" + ingredient.get("r." + RECIPES.id));
		shoppingListIngredient.put("r." + RECIPES.name,
				shoppingListIngredient.get("r." + RECIPES.name) + "|" + ingredient.get("r." + RECIPES.name));
		shoppingListIngredient.put("ri." + RECIPES_INGREDIENTS.ingredientNeed,
				Float.parseFloat(ingredient.get("ri." + RECIPES_INGREDIENTS.ingredientNeed).toString()) +
				Float.parseFloat(shoppingListIngredient.get("ri." + RECIPES_INGREDIENTS.ingredientNeed).toString()));
	}
}
