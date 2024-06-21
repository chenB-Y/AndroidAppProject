package com.example.demoproject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;
import java.util.List;

public interface ApiService {
    @Headers("X-Api-Key:r9k2OVE1wygvgv5tuiQ0vw==kM8eaWRcCELwjpte")
    @GET("recipe")
    Call<List<Recipe>> getRecipes(@Query("query") String query);
}
