package com.bidding.common.model;

public class Art extends Item {
    private String artist;
    private int creationYear;

    public Art(String name, String description, double startingPrice, String artist, int creationYear) {
        // Gọi super() để truyền name, description, startingPrice lên lớp cha Item
        super(name, description, startingPrice); 
        this.artist = artist;
        this.creationYear = creationYear;
    }

    // Getters & Setters
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public int getCreationYear() { return creationYear; }
    public void setCreationYear(int creationYear) { this.creationYear = creationYear; }

    // Tính đa hình (Polymorphism): Ghi đè phương thức của lớp cha
    @Override
    public String getItemDetails() {
        return "Art: " + getName() + " | Artist: " + artist + " | Year: " + creationYear + " | Starting Price: $" + getStartingPrice();
    }
}