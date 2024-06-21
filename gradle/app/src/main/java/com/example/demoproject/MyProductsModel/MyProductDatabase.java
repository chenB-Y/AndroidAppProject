package com.example.demoproject.MyProductsModel;


import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.demoproject.model.Product;
import com.example.demoproject.model.ProductDAO;

@Database(entities = {MyProduct.class}, version = 1)
public abstract class MyProductDatabase extends RoomDatabase {
    public abstract MyProductDAO getmyproductDao();

}


