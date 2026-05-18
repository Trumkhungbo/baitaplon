package com.bidding.common.model.item;

import com.bidding.common.enums.ItemType;

public class Art extends Item {

    private String artist;
    private int creationYear;

    public Art() {
        super();
        setItemType(ItemType.ART);
    }

    public Art(
            String name,
            double startingPrice,
            String imageUrl,
            String artist,
            int creationYear
    ) {

        super(
                name,
                startingPrice,
                ItemType.ART,
                imageUrl
        );

        this.artist = artist;
        this.creationYear = creationYear;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {

        if (artist == null || artist.trim().isEmpty()) {
            throw new IllegalArgumentException("Artist cannot be empty");
        }

        this.artist = artist;
    }

    public int getCreationYear() {
        return creationYear;
    }

    public void setCreationYear(int creationYear) {

        if (creationYear <= 0) {
            throw new IllegalArgumentException("Invalid creation year");
        }

        this.creationYear = creationYear;
    }

    @Override
    public String getItemDetails() {

        return "Art{" +
                "name='" + getName() + '\'' +
                ", artist='" + artist + '\'' +
                ", creationYear=" + creationYear +
                ", startingPrice=$" + getStartingPrice() +
                '}';
    }

    @Override
    public String toString() {
        return getItemDetails();
    }
}