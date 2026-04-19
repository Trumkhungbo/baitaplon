package com.auction.server.service;

import com.auction.server.AuctionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuctionServiceTest {

    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        auctionService = new AuctionService();
    }

    @Test
    void shouldReturnAuctionList() {
        String result = auctionService.getAuctionList();

        assertNotNull(result);
        assertTrue(result.startsWith("AUCTION_LIST|"));
        assertTrue(result.contains("iPhone 15"));
        assertTrue(result.contains("MacBook Pro"));
        assertTrue(result.contains("Oil Painting"));
    }

    @Test
    void shouldReturnAuctionDetail() {
        String result = auctionService.getAuctionDetail("1");

        assertNotNull(result);
        assertTrue(result.startsWith("AUCTION_DETAIL|"));
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("itemName=iPhone 15"));
        assertTrue(result.contains("currentPrice=15000000"));
        assertTrue(result.contains("highestBidder=NONE"));
    }

    @Test
    void shouldAddAuctionSuccessfully() {
        String result = auctionService.addAuction("seller99", "AirPods Pro", 7000000);

        assertTrue(result.startsWith("ADD_AUCTION_SUCCESS|"));
        assertTrue(result.contains("seller=seller99"));
        assertTrue(result.contains("itemName=AirPods Pro"));
        assertTrue(result.contains("startPrice=7000000"));

        String list = auctionService.getAuctionList();
        assertTrue(list.contains("AirPods Pro"));
    }

    @Test
    void shouldPlaceBidSuccessfully() {
        String result = auctionService.placeBid("1", "abc", 17000000);

        assertTrue(result.startsWith("BID_SUCCESS|"));
        assertTrue(result.contains("auctionId=1"));
        assertTrue(result.contains("user=abc"));
        assertTrue(result.contains("amount=17000000"));

        String detail = auctionService.getAuctionDetail("1");
        assertTrue(detail.contains("currentPrice=17000000"));
        assertTrue(detail.contains("highestBidder=abc"));
    }

    @Test
    void shouldThrowWhenAuctionNotFound() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                auctionService.placeBid("99", "abc", 1000000)
        );

        assertEquals("Auction not found", ex.getMessage());
    }

    @Test
    void shouldThrowWhenBidIsTooLow() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                auctionService.placeBid("1", "abc", 1000)
        );

        assertTrue(ex.getMessage().contains("Bid amount must be greater than current price"));
    }

    @Test
    void shouldCloseAuctionSuccessfully() {
        String result = auctionService.closeAuction("1");

        assertTrue(result.startsWith("CLOSE_AUCTION_SUCCESS|"));
        assertTrue(result.contains("auctionId=1"));

        String detail = auctionService.getAuctionDetail("1");
        assertTrue(detail.contains("status=FINISHED"));
    }

    @Test
    void shouldThrowWhenBidAfterAuctionClosed() {
        auctionService.closeAuction("1");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                auctionService.placeBid("1", "abc", 20000000)
        );

        assertEquals("Auction is not open", ex.getMessage());
    }

    @Test
    void shouldReturnWinnerInfo() {
        auctionService.placeBid("2", "abc", 26000000);
        String result = auctionService.getWinner("2");

        assertTrue(result.startsWith("WINNER_INFO|"));
        assertTrue(result.contains("auctionId=2"));
        assertTrue(result.contains("winner=abc"));
        assertTrue(result.contains("finalPrice=26000000"));
    }
}