package com.example.demoproject.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "product")
public class Product implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    int id;

    String productId;


     String userId;

    String image;

    String name;

    String amount;

    boolean isBought;

    Double latitude;

    Double longitude;

    private String Parcelablename;
    private String Parcelabledescription;

    // Constructor, getters, setters, and other methods for your Product class

    // Parcelable implementation
    // No-argument constructor
    public Product() {
    }

    protected Product(Parcel in) {
        name = in.readString();
        Parcelabledescription = in.readString();
    }

    public Product(String image,String name, String amount,String userId , String productId){
        this.image = image;
        this.name = name;
        this.amount = amount;
        this.userId=userId;
        this.isBought=false;
        this.productId=productId;
    }

    //getters and setters for productId
    public String getProductId() {
        return productId;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(Parcelabledescription);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Product> CREATOR = new Creator<Product>() {
        @Override
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        @Override
        public Product[] newArray(int size) {
            return new Product[size];
        }
    };




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

    public int getId() {
        return id;
    }

    public void setId(int id) {this.id = id;}

    public String getUserId() {
        return userId;
    }


    public String getParcelablename() {
        return Parcelablename;
    }

    public String getParcelabledescription() {
        return Parcelabledescription;
    }

    public void setParcelablename(String parcelablename) {
        Parcelablename = parcelablename;
    }

    public void setParcelabledescription(String parcelabledescription) {
        Parcelabledescription = parcelabledescription;
    }
}