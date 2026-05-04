package com.bidding.common.model.item;

import com.bidding.common.enums.ItemType;

public class Art extends Item {

    private String artist;
    private int creationYear;

    public Art() { super(); }

    public Art(String name, String description, double startingPrice,
               String artist, int creationYear) {
        super(name, description, startingPrice, ItemType.ART);
        this.artist = artist;
        this.creationYear = creationYear;
    }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public int getCreationYear() { return creationYear; }
    public void setCreationYear(int creationYear) { this.creationYear = creationYear; }

    @Override
    public String getItemDetails() {
        return String.format("Tác phẩm: %s | Nghệ sĩ: %s | Năm: %d | Giá khởi điểm: $%.2f",
                getName(), artist, creationYear, getStartingPrice());
    }
}