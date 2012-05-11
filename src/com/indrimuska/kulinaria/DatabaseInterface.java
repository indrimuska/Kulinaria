package com.indrimuska.kulinaria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
		static final String id				= "id";
		static final String name			= "name";
		static final String quantity		= "quantity";
		static final String unit			= "unit";
		static final String expirationDate	= "expirationDate";
		static final String CREATE =
				"create table if not exists " + TABLE + " ( " +
						id + " int primary key, " +
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
		// Constructor
		public DbHelper(Context context) {
			super(context, DATABASE, null, VERSION);
		}

		// Called only once, first time the DB is created
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(INGREDIENTS.CREATE);
			db.execSQL(RECIPES.CREATE);
			db.execSQL(RECIPES_INGREDIENTS.CREATE);
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
			db.execSQL("alter table " + tableName + " rename to 'temp_" + tableName + ")");
			// create new table
			db.execSQL(createTable);
			// get the intersection with the new columns, this time columns taken from the upgraded table 
			columns.retainAll(getColumns(db, tableName));
			// restore data
			String cols = join(columns, ",");
			db.execSQL(String.format("insert into %s (%s) select %s from temp_%s", tableName, cols, cols, tableName));
			// remove backup table
			db.execSQL("drop table 'temp_" + tableName);
		}
		
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
	}
	
	final DbHelper dbHelper;

	public DatabaseInterface(Context context) {
		this.dbHelper = new DbHelper(context);
		Log.i(TAG, "Initialized data");
		insertIngredient("bread", 0.5, "kg", 0);
		Log.i(TAG, "Insert new ingredient");
	}
	
	public void close() {
		this.dbHelper.close();
	}
	
	public void insertIngredient(String name, double quantity, String unit, long expirationDate) {
		Log.i(TAG, "insertIngredient on " + name + "," + quantity + "," + unit + "," + expirationDate);
		SQLiteDatabase db = this.dbHelper.getWritableDatabase();
		try {
			ContentValues values = new ContentValues();
			values.put(INGREDIENTS.name, name);
			values.put(INGREDIENTS.quantity, quantity);
			values.put(INGREDIENTS.unit, quantity);
			values.put(INGREDIENTS.expirationDate, expirationDate);
			db.insertWithOnConflict(INGREDIENTS.TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
		} finally {
			db.close();
		}
	}
}
