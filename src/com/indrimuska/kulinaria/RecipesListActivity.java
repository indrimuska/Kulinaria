package com.indrimuska.kulinaria;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class RecipesListActivity extends Activity {
	private String dish;
	private DatabaseInterface db;
	
	private ListView list;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recipes_list);
		
		// Get dish name
		dish = (String) getIntent().getExtras().get("dish");
		
		// Open the database
		db = new DatabaseInterface(this);
		
		// Set activity layout
		((ImageView) findViewById(R.id.dishImage)).setImageResource(
				getResources().getIdentifier("drawable/" + dish.toLowerCase().replace(" ", "_"), "drawable", getPackageName()));
		((TextView) findViewById(R.id.dishName)).setText(dish);
		list = (ListView) findViewById(R.id.dishList);
		fillRecipesList();
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		// Close recipes ListView's cursor
		((SimpleCursorAdapter) list.getAdapter()).getCursor().close();
		
		// Close database
		db.close();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Filling recipes ListView
		if (list != null) {
			SimpleCursorAdapter adapter = (SimpleCursorAdapter) list.getAdapter();
			if (adapter.getCursor() != null) adapter.getCursor().close();
		}
		fillRecipesList();
	}
	
	private void fillRecipesList() {
		Cursor cursor = db.getRecipes(dish);
		startManagingCursor(cursor);
		list.setAdapter(new SimpleCursorAdapter(this, R.layout.recipe_list_item, cursor, new String[] {
				DatabaseInterface.RECIPES.name,
				DatabaseInterface.RECIPES.dish
		}, new int[] {
				R.id.listDishName,
				R.id.listDishOther
		}));
	}
}
