package com.indrimuska.kulinaria;

import java.util.ArrayList;
import java.util.Arrays;
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
	public static final class INGREDIENTS {
		static final String TABLE			= "Ingredients";
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
		static final String TABLE		= "Recipes";
		static final String id			= "id";
		static final String name		= "name";
		static final String dish		= "dish";
		static final String time		= "time";
		static final String description	= "description";
		static final String ORDER_BY	= name + " ASC";
		static final String CREATE =
				"create table if not exists " + TABLE + " ( " +
						id + " int primary key, " +
						name + " text, " +
						dish + " text, " +
						time + " int, " +
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
			db.execSQL(INGREDIENTS.CREATE);
			db.execSQL(RECIPES.CREATE);
			db.execSQL(RECIPES_INGREDIENTS.CREATE);
			Log.d(TAG, "tables creted");
			
			// Populate database (from XML)
			Map<String, ArrayList<ArrayList<String>>> tables = getTablesFromXML(context, R.raw.populate_db);
			for (Map.Entry<String, ArrayList<ArrayList<String>>> table : tables.entrySet()) {
				ArrayList<ArrayList<String>> rows = table.getValue();
				for (ArrayList<String> value : rows) {
					if (table.getKey().equals(INGREDIENTS.TABLE)) {
						ContentValues contentValues = ingredientContentValues(value.get(0), new Float(value.get(1)), value.get(2), new Long(value.get(3)));
						db.insertWithOnConflict(INGREDIENTS.TABLE, null, contentValues, SQLiteDatabase.CONFLICT_IGNORE);
					}
				}
			}
		}
		
		// Called whenever newVersion != oldVersion
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			onUpgradeTable(db, INGREDIENTS.TABLE, INGREDIENTS.CREATE);
			onUpgradeTable(db, RECIPES.TABLE, RECIPES.CREATE);
			onUpgradeTable(db, RECIPES_INGREDIENTS.TABLE, RECIPES_INGREDIENTS.CREATE);
		}
		
		// General onUpgrade (valid for each table)
		private void onUpgradeTable(SQLiteDatabase db, String tableName, String createTable) {
			// table might not exists yet, it will fail alter and drop
			onCreate(db);
			// put in a list the existing columns
			List<String> columns = getColumns(db, tableName);
			// backup table
			db.execSQL("alter table " + tableName + " rename to 'temp_" + tableName);
			// create new table
			db.execSQL(createTable);
			// get the intersection with the new columns, this time columns taken from the upgraded table 
			columns.retainAll(getColumns(db, tableName));
			// restore data
			String cols = join(columns, ",");
			db.execSQL(String.format("insert into %s (%s) select %s from temp_%s", tableName, cols, cols, tableName));
			// remove backup table
			db.execSQL("drop table 'temp_" + tableName);
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
		Map<String, ArrayList<ArrayList<String>>> getTablesFromXML(Context context, int XML) {
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
	
	// Convert ingredient values to ContentValues
	private ContentValues ingredientContentValues(String name, double quantity, String unit, long expirationDate) {
		ContentValues values = new ContentValues();
		values.put(INGREDIENTS.name, name);
		values.put(INGREDIENTS.quantity, quantity);
		values.put(INGREDIENTS.unit, unit);
		values.put(INGREDIENTS.expirationDate, expirationDate);
		return values;
	}
	
	// Insert an ingredient in database
	public void insertIngredient(String name, double quantity, String unit, long expirationDate) {
		Log.d(TAG, "insertIngredient: " + name + "," + quantity + "," + unit + "," + expirationDate);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			ContentValues values = ingredientContentValues(name, quantity, unit, expirationDate);
			db.insert(INGREDIENTS.TABLE, null, values);
		} finally {
			db.close();
		}
	}
	
	// Get ingredient
	public Cursor getIngredient(String ingredientName) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		return db.query(INGREDIENTS.TABLE, null, INGREDIENTS.name+"=?", new String[] { ingredientName }, null, null, null);
	}
	
	// Get ingredient ID
	public int getIngredientID(String ingredientName) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			Cursor cursor = db.query(INGREDIENTS.TABLE, new String[] { INGREDIENTS.id },
					INGREDIENTS.name+"=?", new String[] { ingredientName }, null, null, null);
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
	public Cursor getIngredientList() {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		return db.query(INGREDIENTS.TABLE, null, null, null, null, null, INGREDIENTS.ORDER_BY);
	}
	
	// Check if an ingredient is already stored
	public boolean ingredientAlreadyExists(String ingredientName) {
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		try {
			Cursor cursor = db.query(INGREDIENTS.TABLE, null,
					INGREDIENTS.name+"=?", new String[] { ingredientName }, null, null, null);
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
	public void updateIngredient(int id, String name, double quantity, String unit, long expirationDate) {
		Log.d(TAG, "updateIngredient: " + name + "," + quantity + "," + unit + "," + expirationDate);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try {
			ContentValues values = ingredientContentValues(name, quantity, unit, expirationDate);
			db.update(INGREDIENTS.TABLE, values, INGREDIENTS.id+"=?", new String[] { Integer.toString(id) });
		} finally {
			db.close();
		}
	}
	
}
