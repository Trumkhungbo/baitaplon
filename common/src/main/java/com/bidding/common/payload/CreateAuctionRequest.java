package com.bidding.common.payload;

public class CreateAuctionRequest {
    private long itemId;
    private String startTime; // ISO datetime string
    private String endTime;

    public long getItemId() { return itemId; }
    public void setItemId(long itemId) { this.itemId = itemId; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
//yêu cầu của client tạo đấu giá mới lên server