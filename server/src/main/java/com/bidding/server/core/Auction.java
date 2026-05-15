package com.bidding.server.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import com.bidding.common.enums.AuctionStatus;
public class Auction {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final String id;
    private final String Infomation1;
    private final String Infomation2;
    private final String sellerUsername;
    private final String itemName;
    private final String ItemType;
    private final double startPrice;

    private double currentPrice;
    private AuctionStatus status;
    private String highestBidder;
    private long startTimeMillis;
    private int durationMinutes;
    private Long exactEndTimeMillis;
    private final List<BidRecord> bidHistory;

    public Auction(String id, String sellerUsername,String itemName,String ItemType,String Infomation1, String Infomation2, double startPrice, AuctionStatus status) {
        this.id = id;
        this.ItemType = ItemType;
        this.Infomation1 = Infomation1;
        this.Infomation2 = Infomation2;
        this.sellerUsername = sellerUsername;
        this.itemName = itemName;
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.status = status;
        this.highestBidder = null;
        this.startTimeMillis = System.currentTimeMillis();
        this.durationMinutes = 5;
        this.exactEndTimeMillis = null;
        this.bidHistory = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public String getItemName() {
        return itemName;
    }
    public String getItemType() {
        return ItemType;
    }
    public String getInfomation1() {
        return Infomation1;
    }
    public String getInfomation2() {
        return Infomation2;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public String getHighestBidder() {
        return highestBidder;
    }

    public void setHighestBidder(String highestBidder) {
        this.highestBidder = highestBidder;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
        this.exactEndTimeMillis = null;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = Math.max(durationMinutes, 0);
        this.exactEndTimeMillis = null;
    }

    public String getStartDate() {
        return Instant.ofEpochMilli(startTimeMillis)
                .atZone(ZONE_ID)
                .toLocalDate()
                .format(DATE_FORMATTER);
    }

    public String getStartClockTime() {
        return Instant.ofEpochMilli(startTimeMillis)
                .atZone(ZONE_ID)
                .toLocalTime()
                .format(TIME_FORMATTER);
    }

    public long getEndTime() {
        return exactEndTimeMillis != null
                ? exactEndTimeMillis
                : startTimeMillis + durationMinutes * 60_000L;
    }

    public void setEndTime(long endTime) {
        long delta = Math.max(0L, endTime - startTimeMillis);
        this.durationMinutes = (int) Math.ceil(delta / 60_000.0);
        this.exactEndTimeMillis = endTime;
    }

    public void extendEndTime(long extraTime) {
        long newEndTime = getEndTime() + extraTime;
        setEndTime(newEndTime);
    }

    public void setSchedule(LocalDate startDate, LocalTime startTime, int durationMinutes) {
        this.startTimeMillis = startDate.atTime(startTime).atZone(ZONE_ID).toInstant().toEpochMilli();
        this.durationMinutes = Math.max(durationMinutes, 0);
        this.exactEndTimeMillis = null;
    }

    public void addBidRecord(BidRecord bidRecord) {
        bidHistory.add(bidRecord);
    }

    public List<BidRecord> getBidHistorySnapshot() {
        return new ArrayList<>(bidHistory);
    }
}
