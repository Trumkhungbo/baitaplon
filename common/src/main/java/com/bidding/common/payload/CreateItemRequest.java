package com.bidding.common.payload;

import com.bidding.common.enums.ItemType;

public class CreateItemRequest {
    private String name;
    private String description;
    private double startingPrice;
    private ItemType itemType;
    
    // Thuộc tính cho Art
    private String artist;
    private int creationYear;
    
    // Thuộc tính cho Electronics
    private String brand;
    private int warrantyMonths;
    
    // Thuộc tính cho Vehicle
    private String engineType;
    private int mileage;
// Getters và Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public int getCreationYear() { return creationYear; }
    public void setCreationYear(int creationYear) { this.creationYear = creationYear; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }

    public String getEngineType() { return engineType; }
    public void setEngineType(String engineType) { this.engineType = engineType; }

    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }
}
// gửi từ client lên server để tạo vật phẩm mới