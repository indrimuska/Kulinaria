package com.indrimuska.kulinaria;

import java.util.ArrayList;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
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
		
		// Filling recipes ListView
		Cursor cursor = db.getRecipes(dish);
		ArrayList<Map<String, Object>> recipesList = new ArrayList<Map<String,Object>>();
		String[] from = new String[] {
				DatabaseInterface.RECIPES.id,
				DatabaseInterface.RECIPES.name,
				DatabaseInterface.RECIPES.readyTime
		};
		
		try {
			if (cursor.getCount() > 0) recipesList = new ArrayCursorAdapter(this, cursor, from).getList();
			else {
				TextView noRecipes = new TextView(this);
				noRecipes.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1));
				noRecipes.setGravity(Gravity.CENTER);
				noRecipes.setText(R.string.recipesNoRecipes);
				noRecipes.setTextSize(20 * getResources().getDisplayMetrics().density);
				noRecipes.setPadding(20, 20, 20, 20);			
				LinearLayout linearLayout = (LinearLayout) list.getParent();
				linearLayout.removeViewAt(1);
				linearLayout.addView(noRecipes, 1);
				cursor.close();
				list = null;
				return;
			}
		} finally {
			cursor.close();
		}
		SimpleAdapter adapter = new SimpleAdapter(this, recipesList, R.layout.recipes_list_item, from, new int[] {
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
		list.setAdapter(adapter);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				startActivity(new Intent(RecipesListActivity.this, RecipeActivity.class)
					.putExtra("recipeId", Integer.parseInt(((TextView) view.findViewById(R.id.recipesListId)).getText().toString())));
			}
		});
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
}
