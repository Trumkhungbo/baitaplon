package com.bidding.common.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonModelTest {

    @Test
    void entityShouldAllowUpdatingBaseFieldsThroughSubclass() {
        AutoBid autoBid = new AutoBid();

        assertEquals(0, autoBid.getId());
        assertTrue(autoBid.getCreatedAt() > 0);

        autoBid.setId(10);
        autoBid.setCreatedAt(123_456);

        assertEquals(10, autoBid.getId());
        assertEquals(123_456, autoBid.getCreatedAt());
    }

    @Test
    void autoBidConstructorShouldSetActiveConfiguration() {
        AutoBid autoBid = new AutoBid(7, "bidder1", 5_000, 250);

        assertEquals(7, autoBid.getAuctionId());
        assertEquals("bidder1", autoBid.getBidderUsername());
        assertEquals(5_000, autoBid.getMaxBid());
        assertEquals(250, autoBid.getIncrement());
        assertTrue(autoBid.isActive());
    }

    @Test
    void autoBidSettersShouldUpdateConfiguration() {
        AutoBid autoBid = new AutoBid();

        autoBid.setAuctionId(8);
        autoBid.setBidderUsername("bidder2");
        autoBid.setMaxBid(6_000);
        autoBid.setIncrement(300);
        autoBid.setActive(false);

        assertEquals(8, autoBid.getAuctionId());
        assertEquals("bidder2", autoBid.getBidderUsername());
        assertEquals(6_000, autoBid.getMaxBid());
        assertEquals(300, autoBid.getIncrement());
        assertFalse(autoBid.isActive());
    }

    @Test
    void bidTransactionConstructorShouldSetBidDataAndTime() {
        BidTransaction bid = new BidTransaction(3, "bidder3", 1_500);

        assertEquals(3, bid.getAuctionId());
        assertEquals("bidder3", bid.getBidderUsername());
        assertEquals(1_500, bid.getBidAmount());
        assertNotNull(bid.getBidTime());
    }

    @Test
    void bidTransactionSettersShouldUpdateBidData() {
        BidTransaction bid = new BidTransaction();
        LocalDateTime bidTime = LocalDateTime.of(2026, 5, 25, 10, 0);

        bid.setAuctionId(4);
        bid.setBidderUsername("bidder4");
        bid.setBidAmount(2_000);
        bid.setBidTime(bidTime);

        assertEquals(4, bid.getAuctionId());
        assertEquals("bidder4", bid.getBidderUsername());
        assertEquals(2_000, bid.getBidAmount());
        assertEquals(bidTime, bid.getBidTime());
    }

    @Test
    void auctionSettersShouldUpdateAllMutableFields() {
        Auction auction = new Auction();
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 25, 9, 0);
        LocalDateTime endTime = startTime.plusHours(1);
        ArrayList<BidTransaction> history = new ArrayList<>();
        history.add(new BidTransaction(1, "bidder", 1_200));

        auction.setSellerUsername("seller");
        auction.setStartTime(startTime);
        auction.setEndTime(endTime);
        auction.setCurrentHighestBid(1_200);
        auction.setHighestBidderUsername("bidder");
        auction.setBidHistory(history);

        assertEquals("seller", auction.getSellerUsername());
        assertEquals(startTime, auction.getStartTime());
        assertEquals(endTime, auction.getEndTime());
        assertEquals(1_200, auction.getCurrentHighestBid());
        assertEquals("bidder", auction.getHighestBidderUsername());
        assertEquals(history, auction.getBidHistory());
    }
}
