package com.indrimuska.kulinaria;

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
	public static final class INGREDIENTS {
		static final String TABLE			= "Ingredients";
		static final String id				= "_id";
		static final String name			= "name";
		static final String ORDER_BY		= name + " ASC";
		static final String CREATE =
				"create table if not exists " + TABLE + " ( " +
						id + " integer primary key autoincrement, " +
						name + " text )";
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
		static final String ingredientId	= "ingredientId";
		static final String ingredientNeed	= "ingredientNeed";
		static final String unit			= "unit";
		static final String ORDER_BY		= recipeId + " ASC";
		static final String CREATE =
				"create table if not exists " + TABLE + " ( " +
						recipeId + " int, " +
						ingredientId + " int, " +
						ingredientNeed + " float, " +
						unit + " text, " +
						"primary key( " +
							recipeId + ", " +
							ingredientId + " ) " +
						")";
	}
	public static final class MENU {
		static final String TABLE		= "Menu";
		static final String date		= "date";
		static final String meal		= "meal";
		static final String recipeId	= "recipeId";
		static final String ORDER_BY	= date + " ASC";
		static final String CREATE =
				"create table if not exists " + TABLE + " (" +
						date +" date, " +
						meal + " text, " +
						recipeId + " int, " +
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
			db.execSQL(INGREDIENTS.CREATE);
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
								value.get(0), new Float(value.get(1)), value.get(2), new Long(value.get(3))));
					if (table.getKey().equals(INGREDIENTS.TABLE))
						db.insert(INGREDIENTS.TABLE, null, ingredientContentValues(value.get(0)));
					if (table.getKey().equals(RECIPES.TABLE))
						db.insert(RECIPES.TABLE, null, recipeContentValues(
								value.get(0), value.get(1), new Integer(value.get(2)), new Integer(value.get(3)),
								new Integer(value.get(4)), value.get(5)));
					if (table.getKey().equals(RECIPES_INGREDIENTS.TABLE))
						db.insert(RECIPES_INGREDIENTS.TABLE, null, recipeIngredientContentValues(
								new Integer(value.get(0)), new Integer(value.get(1)), new Integer(value.get(2)), value.get(3)));
				}
			}
		}
		
		// Called whenever newVersion != oldVersion
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			onUpgradeTable(db, INVENTORY.TABLE, INVENTORY.CREATE);
			onUpgradeTable(db, INGREDIENTS.TABLE, INGREDIENTS.CREATE);
			onUpgradeTable(db, RECIPES.TABLE, RECIPES.CREATE);
			onUpgradeTable(db, RECIPES_INGREDIENTS.TABLE, RECIPES_INGREDIENTS.CREATE);
			onUpgradeTable(db, MENU.TABLE, MENU.CREATE);
		}
		
		// General onUpgrade (valid for each table)
		private void onUpgradeTable(SQLiteDatabase db, String tableName, String createTable) {
			// table might not exists yet, it will fail alter and drop
			//onCreate(db);
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

		//SQLiteDatabase db = dbHelper.getReadableDatabase();
		//db.execSQL("insert into Menu values ('2012-05-28', 'Breakfast', 1)");
	}
	
	public void close() {
		dbHelper.close();
	}
	
	// Convert cursor to a map-array
	public ArrayList<Map<String, Object>> cursorToMapArray(Cursor cursor, String[] columns) {
		ArrayList<Map<String, Object>> array = new ArrayList<Map<String, Object>>();
		while (cursor.moveToNext()) {
			Map<String, Object> element = new HashMap<String, Object>();
			for (String column : columns) element.put(column, cursor.getString(cursor.getColumnIndex(column)));
			array.add(element);
		}
		return array;
	}
	
	// Convert inventory ingredient informations to ContentValues
	private ContentValues inventoryIngredientContentValues(String name, double quantity, String unit, long expirationDate) {
		ContentValues values = new ContentValues();
		values.put(INVENTORY.name, name);
		values.put(INVENTORY.quantity, quantity);
		values.put(INVENTORY.unit, unit);
		values.put(INVENTORY.expirationDate, expirationDate);
		return values;
	}
	
	// Insert an ingredient in database
	public void insertInventoryIngredient(String name, double quantity, String unit, long expirationDate) {
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
	
	// Get the list of ingredients stored
	public Cursor getInventory() {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		return db.query(INVENTORY.TABLE, null, null, null, null, null, INVENTORY.ORDER_BY);
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
	public void updateInventoryIngredient(int id, String name, double quantity, String unit, long expirationDate) {
		Log.d(TAG, "updateInventoryIngredient: " + name + "," + quantity + "," + unit + "," + expirationDate);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			ContentValues values = inventoryIngredientContentValues(name, quantity, unit, expirationDate);
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
	
	// Convert ingredient informations to ContentValues
	private ContentValues ingredientContentValues(String name) {
		ContentValues values = new ContentValues();
		values.put(INGREDIENTS.name, name);
		return values;
	}
	
	// Get the list of ingredients stored
	public Cursor getIngredients() {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		return db.query(INGREDIENTS.TABLE, null, null, null, null, null, INGREDIENTS.ORDER_BY);
	}
	
	// Get the name of an ingredient
	public String getIngredientName(int ingredientId) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			Cursor cursor = db.query(INGREDIENTS.TABLE, new String[] { INGREDIENTS.name },
					INGREDIENTS.id+"=?", new String[] { Integer.toString(ingredientId) }, null, null, null);
			try {
				return cursor.moveToFirst() ? cursor.getString(0) : null;
			} finally {
				cursor.close();
			}
		} finally {
			db.close();
		}
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
	public Cursor getRecipesExcept(String dish, ArrayList<Integer> recipesId) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		String selectString = RECIPES.dish+"=?";
		ArrayList<String> selectValuesArray = new ArrayList<String>();
		selectValuesArray.add(dish);
		for (int recipeId : recipesId) {
			selectString += " and "+RECIPES.id+"!=?";
			selectValuesArray.add(Integer.toString(recipeId));
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
	private ContentValues recipeIngredientContentValues(int recipeId, int ingredientId, int ingredientNeed, String unit) {
		ContentValues values = new ContentValues();
		values.put(RECIPES_INGREDIENTS.recipeId, recipeId);
		values.put(RECIPES_INGREDIENTS.ingredientId, ingredientId);
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
						RECIPES_INGREDIENTS.ingredientId,
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
	private ContentValues menuContentValues(String date, String meal, int recipeId) {
		ContentValues values = new ContentValues();
		values.put(MENU.date, date);
		values.put(MENU.meal, meal);
		values.put(MENU.recipeId, recipeId);
		return values;
	}
	
	// Get the menu chosen for a meal
	public ArrayList<Integer> getMenu(String date, String meal) {
		Log.d(TAG, "getMenu: " + meal);
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			Cursor cursor = db.query(MENU.TABLE, new String[] { MENU.recipeId },
					MENU.date+"=? and "+MENU.meal+"=?", new String[] { date, meal }, null, null, null);
			try {
				ArrayList<Integer> recipesId = new ArrayList<Integer>();
				while (cursor.moveToNext()) recipesId.add(cursor.getInt(cursor.getColumnIndex(MENU.recipeId)));
				return recipesId;
			} finally {
				cursor.close();
			}
		} finally {
			db.close();
		}
	}
	
	// Delete a dish from today's menu
	public void deleteTodayMenuDish(String meal, int recipeId) {
		deleteMenuDish(new SimpleDateFormat("yyyy-MM-dd").format(new Date()), meal, recipeId);
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
		try { db.insert(MENU.TABLE, null, menuContentValues(date, meal, recipeId)); }
		finally { db.close(); }
	}
}
