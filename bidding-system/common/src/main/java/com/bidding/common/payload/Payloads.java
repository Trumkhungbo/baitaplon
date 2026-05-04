package com.bidding.common.payload;

import com.bidding.common.enums.ItemType;
import com.bidding.common.enums.UserRole;

/** Các DTO (Data Transfer Object) dùng cho payload của Request/Response */
public class Payloads {

    // ===================== AUTH =====================

    public static class RegisterRequest {
        public String username;
        public String password;
        public String email;
        public String fullName;
        public UserRole role; // BIDDER hoặc SELLER
        public double initialBalance; // chỉ dùng khi role = BIDDER
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class LoginResponse {
        public String token;
        public String username;
        public UserRole role;
        public long userId;
    }

    // ===================== ITEM =====================

    public static class CreateItemRequest {
        public String name;
        public String description;
        public double startingPrice;
        public ItemType itemType;
        // Art
        public String artist;
        public int creationYear;
        // Electronics
        public String brand;
        public int warrantyMonths;
        // Vehicle
        public String engineType;
        public int mileage;
    }

    // ===================== AUCTION =====================

    public static class CreateAuctionRequest {
        public long itemId;
        public String startTime; // ISO datetime string
        public String endTime;
    }

    public static class AuctionIdRequest {
        public long auctionId;
    }

    // ===================== BID =====================

    public static class PlaceBidRequest {
        public long auctionId;
        public double bidAmount;
    }

    // ===================== AUTOBID =====================

    public static class RegisterAutoBidRequest {
        public long auctionId;
        public double maxBid;
        public double increment;
    }

    public static class DisableAutoBidRequest {
        public long auctionId;
    }
}