package com.example.demoproject;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecipesFragment extends Fragment {
    private TextInputEditText recipeQuery;
    private Button btnSearch;
    private RecyclerView rvRecipes;
    private RecipeAdapter recipeAdapter;
    private List<Recipe> recipes = new ArrayList<>();
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipes, container, false);
        recipeQuery = view.findViewById(R.id.recipeQuery);
        btnSearch = view.findViewById(R.id.btnSearch);
        rvRecipes = view.findViewById(R.id.rvRecipes);
        progressBar = view.findViewById(R.id.progressBar);

        recipeAdapter = new RecipeAdapter(recipes);
        rvRecipes.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecipes.setAdapter(recipeAdapter);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = recipeQuery.getText().toString().trim();
                if (!query.isEmpty()) {
                    fetchRecipes(query);
                } else {
                    Toast.makeText(requireContext(), "Please enter a recipe name", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    private void fetchRecipes(String query) {
        progressBar.setVisibility(View.VISIBLE);  // Show ProgressBar
        ApiService apiService = RetrofitInstance.getApi();
        Call<List<Recipe>> call = apiService.getRecipes(query);
        call.enqueue(new Callback<List<Recipe>>() {
            @Override
            public void onResponse(Call<List<Recipe>> call, Response<List<Recipe>> response) {
                progressBar.setVisibility(View.GONE);  // Hide ProgressBar
                if (response.isSuccessful() && response.body() != null) {
                    recipes.clear();
                    recipes.addAll(response.body());
                    recipeAdapter.notifyDataSetChanged();
                } else {
                    Log.d("TAG", "Failed to fetch recipes");
                }
            }

            @Override
            public void onFailure(Call<List<Recipe>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);  // Hide ProgressBar
                Toast.makeText(requireContext(), "An error occurred", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
