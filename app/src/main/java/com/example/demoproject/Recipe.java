package com.example.demoproject;

public class Recipe {
    private String title;
    private String ingredients;
    private String servings;
    private String instructions;

    // Constructor
    public Recipe(String title, String ingredients, String servings, String instructions) {
        this.title = title;
        this.ingredients = ingredients;
        this.servings = servings;
        this.instructions = instructions;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getIngredients() {
        return ingredients;
    }

    public String getServings() {
        return servings;
    }

    public String getInstructions() {
        return instructions;
    }
}
