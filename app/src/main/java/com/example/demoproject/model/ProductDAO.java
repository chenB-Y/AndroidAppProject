package com.example.demoproject.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ProductDAO {

    @Insert
    void addProduct(Product product);

    @Update
    void updateProduct(Product product);

    @Delete
    void deleteProduct(Product product);

    @Query("SELECT * FROM product")
    List<Product> getAllProducts();

    @Query("SELECT * FROM product WHERE id = :productId")
    Product getProductById(int productId);

    @Query("SELECT * FROM product WHERE name = :productName")
    Product getProductByName(String productName);

    @Query("SELECT * FROM product WHERE amount = :productAmount")
    List<Product> getProductsByAmount(String productAmount);

    @Query("DELETE FROM product")
    void deleteAllProducts();

    @Query("SELECT * FROM product WHERE userId = :userId")
    List<Product> getProductsByUserId(String userId);

}
