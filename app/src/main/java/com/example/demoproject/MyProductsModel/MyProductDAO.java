package com.example.demoproject.MyProductsModel;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.demoproject.model.Product;

import java.util.List;

@Dao

public interface MyProductDAO {

    @Insert
    void addMyProduct(MyProduct myproduct);

    @Delete
    void deleteProduct(MyProduct myproduct);
    @Query("SELECT * FROM MyProduct")
    List<MyProduct> getAllMyProducts();

    @Query("SELECT * FROM MyProduct product WHERE image = :productId")
    MyProduct getMyProductById(int productId);

    @Query("SELECT * FROM MyProduct WHERE name = :productName")
    MyProduct getMyProductByName(String productName);

    @Query("SELECT * FROM MyProduct WHERE amount = :productAmount")
    List<MyProduct> getMyProductsByAmount(String productAmount);

    @Query("DELETE FROM MyProduct")
    void deleteAllMyProducts();

    @Query("SELECT * FROM MyProduct WHERE userId = :userId")
    List<MyProduct> getMyProductsByUserId(String userId);
}
