package com.indrimuska.kulinaria;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
		((Button) findViewById(R.id.dishBackButton)).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View view) { finish(); }
		});
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		// Close recipes ListView's cursor
		if (list != null) ((SimpleCursorAdapter) list.getAdapter()).getCursor().close();
		
		// Close database
		db.close();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// Filling recipes ListView
		fillRecipesList();
	}
	
	private void fillRecipesList() {
		Cursor cursor = db.getRecipes(dish);
		startManagingCursor(cursor);
		
		if (cursor.getCount() <= 0) {
			TextView noRecipes = new TextView(this);
			noRecipes.setGravity(Gravity.CENTER);
			noRecipes.setText(R.string.recipesNoRecipes);
			noRecipes.setTextSize(20 * getResources().getDisplayMetrics().density);
			noRecipes.setPadding(20, 20, 20, 20);			
			LinearLayout linearLayout = (LinearLayout) list.getParent();
			linearLayout.removeViewAt(1);
			linearLayout.addView(noRecipes);
			cursor.close();
			list = null;
		} else {
			list.setAdapter(new SimpleCursorAdapter(this, R.layout.recipes_list_item, cursor, new String[] {
					DatabaseInterface.RECIPES.id,
					DatabaseInterface.RECIPES.name,
					DatabaseInterface.RECIPES.dish
			}, new int[] {
					R.id.recipesListId,
					R.id.recipesListName,
					R.id.recipesListOther
			}));
			list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					startActivity(new Intent(RecipesListActivity.this, RecipeActivity.class)
						.putExtra("recipeId", Integer.parseInt(((TextView) view.findViewById(R.id.recipesListId)).getText().toString())));
				}
			});
		}
	}
}
