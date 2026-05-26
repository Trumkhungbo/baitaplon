package com.bidding.common.model;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.common.enums.ItemType;
import com.bidding.common.model.item.Electronics;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionTest {

    @Test
    void constructorShouldInitializeAuctionFromItem() {
        Electronics item = new Electronics("Laptop", 1_000, "laptop.png", "Dell", 12);
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusHours(2);

        Auction auction = new Auction(item, "seller1", startTime, endTime);

        assertEquals(item, auction.getItem());
        assertEquals("seller1", auction.getSellerUsername());
        assertEquals(startTime, auction.getStartTime());
        assertEquals(endTime, auction.getEndTime());
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertEquals(1_000, auction.getCurrentHighestBid());
        assertNotNull(auction.getBidHistory());
        assertTrue(auction.getBidHistory().isEmpty());
    }

    @Test
    void placeBidShouldAcceptHigherBidWhenAuctionIsRunning() {
        Auction auction = createRunningAuction();
        BidTransaction bid = new BidTransaction(auction.getId(), "bidder1", 1_200);

        boolean accepted = auction.placeBid(bid);

        assertTrue(accepted);
        assertEquals(1_200, auction.getCurrentHighestBid());
        assertEquals("bidder1", auction.getHighestBidderUsername());
        assertEquals(1, auction.getBidHistory().size());
        assertEquals(bid, auction.getBidHistory().getFirst());
    }

    @Test
    void placeBidShouldRejectBidThatDoesNotBeatCurrentPrice() {
        Auction auction = createRunningAuction();

        boolean accepted = auction.placeBid(new BidTransaction(auction.getId(), "bidder1", 900));

        assertFalse(accepted);
        assertEquals(1_000, auction.getCurrentHighestBid());
        assertTrue(auction.getBidHistory().isEmpty());
    }

    @Test
    void placeBidShouldRejectBidWhenAuctionIsNotRunning() {
        Auction auction = createRunningAuction();
        auction.setStatus(AuctionStatus.FINISHED);

        boolean accepted = auction.placeBid(new BidTransaction(auction.getId(), "bidder1", 1_200));

        assertFalse(accepted);
        assertEquals(1_000, auction.getCurrentHighestBid());
        assertTrue(auction.getBidHistory().isEmpty());
    }

    @Test
    void itemSettersShouldValidateInput() {
        Electronics item = new Electronics();

        assertThrows(IllegalArgumentException.class, () -> item.setName(" "));
        assertThrows(IllegalArgumentException.class, () -> item.setStartingPrice(-1));
        assertThrows(IllegalArgumentException.class, () -> item.setBrand(null));
        assertThrows(IllegalArgumentException.class, () -> item.setWarrantyMonths(-1));

        item.setName("Phone");
        item.setStartingPrice(500);
        item.setBrand("Samsung");
        item.setWarrantyMonths(24);
        item.setDescription("  Good condition  ");
        item.setImageUrl(null);

        assertEquals("Phone", item.getName());
        assertEquals(500, item.getStartingPrice());
        assertEquals("Samsung", item.getBrand());
        assertEquals(24, item.getWarrantyMonths());
        assertEquals("Good condition", item.getDescription());
        assertEquals("", item.getImageUrl());
        assertEquals(ItemType.ELECTRONICS, item.getItemType());
    }

    private Auction createRunningAuction() {
        Electronics item = new Electronics("Laptop", 1_000, "laptop.png", "Dell", 12);
        Auction auction = new Auction(
                item,
                "seller1",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(2)
        );
        auction.setId(1);
        auction.setStatus(AuctionStatus.RUNNING);
        return auction;
    }
}
