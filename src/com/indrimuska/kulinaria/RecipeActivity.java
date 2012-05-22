package com.indrimuska.kulinaria;

import java.util.ArrayList;
import java.util.Map;

import com.indrimuska.kulinaria.DatabaseInterface.RECIPES;
import com.indrimuska.kulinaria.DatabaseInterface.RECIPES_INGREDIENTS;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class RecipeActivity extends Activity {
	private int recipeId;
	private DatabaseInterface db;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recipe);
		
		// Get recipe id
		recipeId = 1;//(Integer) getIntent().getExtras().get("recipeId");

		// Open the database
		db = new DatabaseInterface(this);
		
		// Set activity layout
		((ImageView) findViewById(R.id.recipeBackButton)).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View view) { finish(); }
		});
		Cursor cursor = db.getRecipe(recipeId);
		try {
			startManagingCursor(cursor);
			cursor.moveToFirst();
			((TextView) findViewById(R.id.recipeName)).setText(cursor.getString(cursor.getColumnIndex(RECIPES.name)));
			((TextView) findViewById(R.id.recipeDish)).setText(cursor.getString(cursor.getColumnIndex(RECIPES.dish)));
			((TextView) findViewById(R.id.recipePreparationTime)).setText(cursor.getString(cursor.getColumnIndex(RECIPES.preparationTime)));
			((TextView) findViewById(R.id.recipeReadyTime)).setText(cursor.getString(cursor.getColumnIndex(RECIPES.readyTime)));
			((TextView) findViewById(R.id.recipeServings)).setText(cursor.getString(cursor.getColumnIndex(RECIPES.servings)));
			((TextView) findViewById(R.id.recipeDescription)).setText(cursor.getString(cursor.getColumnIndex(RECIPES.description)));
		} finally {
			cursor.close();
		}
		String ingredients = "";
		ArrayList<Map<String, Object>> recipeIngredients = db.getRecipeIngredients(recipeId);
		final String ingredientsChild[] = new String[recipeIngredients.size()];
		int i = 0;
		for (Map<String, Object> ingredient : recipeIngredients) {
			ingredients += "\n" + ingredient.get(RECIPES_INGREDIENTS.ingredientNeed).toString() + " " +
					ingredient.get(RECIPES_INGREDIENTS.unit) + " " +
					db.getIngredientName((Integer) ingredient.get(RECIPES_INGREDIENTS.ingredientId));
			ingredientsChild[i] = ingredient.get(RECIPES_INGREDIENTS.ingredientNeed).toString() + " " +
					ingredient.get(RECIPES_INGREDIENTS.unit) + " " +
					db.getIngredientName((Integer) ingredient.get(RECIPES_INGREDIENTS.ingredientId));
			i++;
		}
		((TextView) findViewById(R.id.recipeIngredients)).setText(ingredients.substring(1));
	}
	
	@Override
	protected void onPause() {
		super.onStop();

		// Close database
		db.close();
	}
}
