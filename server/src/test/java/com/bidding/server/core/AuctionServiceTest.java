package com.bidding.server.core;

import com.bidding.server.exception.InvalidBidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AuctionServiceTest {

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
        assertTrue(result.contains("bidCount=0"));
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
        assertTrue(detail.contains("status=RUNNING"));
        assertTrue(detail.contains("bidCount=1"));
    }

    @Test
    void shouldReturnBidHistory() {
        auctionService.placeBid("1", "abc", 17000000);
        auctionService.placeBid("1", "xyz", 18000000);

        String result = auctionService.getBidHistory("1");

        assertTrue(result.startsWith("BID_HISTORY|auctionId=1|entries="));
        assertTrue(result.contains("abc,17000000,"));
        assertTrue(result.contains("xyz,18000000,"));
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

        assertEquals("Auction is not available", ex.getMessage());
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

    @Test
    void shouldCloseRunningAuctionSuccessfully() {
        auctionService.placeBid("1", "abc", 17000000);

        String result = auctionService.closeAuction("1");

        assertTrue(result.startsWith("CLOSE_AUCTION_SUCCESS|"));
        assertTrue(result.contains("auctionId=1"));
        assertTrue(auctionService.getAuctionDetail("1").contains("status=FINISHED"));
    }

    @Test
    void shouldCloseExpiredRunningAuction() {
        auctionService.placeBid("1", "abc", 17000000);
        Auction auction = auctionService.findAuctionById("1");
        auction.setEndTime(System.currentTimeMillis() - 1);

        List<String> notifications = auctionService.closeExpiredAuctions();

        assertEquals(1, notifications.size());
        assertTrue(notifications.get(0).startsWith("AUCTION_CLOSED|auctionId=1"));
        assertTrue(notifications.get(0).contains("winner=abc"));
        assertTrue(auctionService.getAuctionDetail("1").contains("status=FINISHED"));
    }

    @Test
    void shouldRejectBidWhenAuctionAlreadyExpired() {
        Auction auction = auctionService.findAuctionById("1");
        long expiredEndTime = System.currentTimeMillis() - 1;
        auction.setEndTime(expiredEndTime);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                auctionService.placeBid("1", "abc", 17000000)
        );

        assertEquals("Auction is not available", ex.getMessage());
        assertEquals(expiredEndTime, auction.getEndTime());
        assertTrue(auctionService.getAuctionDetail("1").contains("status=FINISHED"));
    }

    @Test
    void shouldRespectStatusPassedToAuctionConstructor() {
        Auction auction = new Auction("99", "seller", "item", 1000, AuctionStatus.CANCELED);

        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }

    @Test
    void shouldKeepHighestBidWhenManyClientsBidAtSameTime() throws Exception {
        String addResponse = auctionService.addAuction("seller-concurrent", "Concurrent Item", 1_000);
        String auctionId = addResponse.split("\\|")[1].split("=")[1];

        int bidderCount = 20;
        double highestBid = 1_000 + bidderCount;
        String expectedWinner = "bidder-" + bidderCount;

        CountDownLatch ready = new CountDownLatch(bidderCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(bidderCount);
        ExecutorService pool = Executors.newFixedThreadPool(bidderCount);
        ConcurrentLinkedQueue<Throwable> unexpectedFailures = new ConcurrentLinkedQueue<>();
        List<Future<?>> tasks = new ArrayList<>();

        for (int i = 1; i <= bidderCount; i++) {
            int bidderNo = i;
            double bidAmount = 1_000 + bidderNo;
            String bidder = "bidder-" + bidderNo;

            tasks.add(pool.submit(() -> {
                ready.countDown();
                try {
                    assertTrue(start.await(5, TimeUnit.SECONDS), "Test start gate timed out");
                    auctionService.placeBid(auctionId, bidder, bidAmount);
                } catch (InvalidBidException ignored) {
                    // Lower bids may lose the race after a higher bid has been accepted.
                } catch (Throwable t) {
                    unexpectedFailures.add(t);
                } finally {
                    done.countDown();
                }
            }));
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS), "Not all bidder threads became ready");
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "Not all bidder threads finished");

        for (Future<?> task : tasks) {
            task.get(1, TimeUnit.SECONDS);
        }

        pool.shutdownNow();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS), "Executor did not shut down cleanly");
        assertTrue(unexpectedFailures.isEmpty(), () -> "Unexpected failures: " + unexpectedFailures);

        Auction auction = auctionService.findAuctionById(auctionId);
        assertEquals(highestBid, auction.getCurrentPrice());
        assertEquals(expectedWinner, auction.getHighestBidder());
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
    }
}
