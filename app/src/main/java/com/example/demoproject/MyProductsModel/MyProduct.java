package com.example.demoproject.MyProductsModel;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.demoproject.model.Product;


@Entity(tableName = "MyProduct")
public class MyProduct {


    @PrimaryKey(autoGenerate = true)
    int id;


    String userId;

    String image;

    String name;

    String amount;

    boolean isBought;


    public MyProduct(String userId, String image, String name, String amount, boolean isBought) {
        this.userId = userId;
        this.image = image;
        this.name = name;
        this.amount = amount;
        this.isBought = isBought;

    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getUid() {
        return userId;
    }

    public void setUid(String userId) {
        this.userId = userId;
    }

    public boolean isBought() {
        return isBought;
    }

    public void setBought(boolean bought) {
        isBought = bought;
    }



}