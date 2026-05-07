package com.bidding.server.model.item;

import com.bidding.common.enums.ItemType;

public class Art extends Item {
    private String artist;
    private int creationYear;

    public Art() {
        super();
        setItemType(com.bidding.common.enums.ItemType.ART);
    }

    public Art(String name, String description, double startingPrice, String artist, int creationYear) {
        super(name, description, startingPrice);
        setItemType(com.bidding.common.enums.ItemType.ART);
        this.artist = artist;
        this.creationYear = creationYear;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getCreationYear() {
        return creationYear;
    }

    public void setCreationYear(int creationYear) {
        this.creationYear = creationYear;
    }

    @Override
    public String getItemDetails() {
        return "Art: " + getName() + " | Artist: " + artist + " | Year: " + creationYear + " | Starting Price: $" + getStartingPrice();
    }

    public void setItemType(ItemType itemType) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
