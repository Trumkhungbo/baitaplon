package com.bidding.server.core;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.server.database.DatabaseInitializer;
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

/**
 * Bộ test cho {@link AuctionService} — lớp nghiệp vụ trung tâm xử lý đấu giá.
 *
 * <p>Mỗi test case đều reset database trước khi chạy ({@code @BeforeEach}) để đảm bảo
 * trạng thái sạch. Seed data tự động tạo 3 phiên đấu giá mẫu (iPhone 15, MacBook Pro, Oil Painting)
 * với status OPEN và startTimeMillis = thời điểm hiện tại.
 *
 * <p>Các nhóm test:
 * <ul>
 *   <li>Query: list, detail, productInfo, bidHistory, winner</li>
 *   <li>Command: placeBid, addAuction, closeAuction, setAutoBid</li>
 *   <li>Edge case: bid thấp, auction đã đóng, auction hết hạn</li>
 *   <li>Concurrency: 20 threads đặt giá đồng thời</li>
 * </ul>
 */
public class AuctionServiceTest {

    private AuctionService auctionService;

    /**
     * Reset DB và tạo AuctionService mới trước mỗi test.
     * resetAuctionRuntimeData() xóa bảng auction_state và bid_history
     * để các test không ảnh hưởng lẫn nhau.
     */
    @BeforeEach
    void setUp() {
        DatabaseInitializer.initialize();
        DatabaseInitializer.resetAuctionRuntimeData();
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
    void shouldReturnProductInfo() {
        String result = auctionService.getProductInfo("1");

        assertNotNull(result);
        assertTrue(result.startsWith("PRODUCT_INFO|"));
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("itemName=iPhone 15"));
        assertTrue(result.contains("seller=seller1"));
        assertTrue(result.contains("startPrice=15000000"));
        assertTrue(result.contains("currentPrice=15000000"));
        // Seed data khởi tạo OPEN nhưng startTimeMillis = now, nên applyTimeBasedStatus
        // có thể tự chuyển sang RUNNING trước khi test kịp assert.
        assertTrue(result.contains("status=OPEN") || result.contains("status=RUNNING"));
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
    void shouldLoadAddedAuctionFromDatabaseAcrossServiceInstances() {
        String result = auctionService.addAuction("seller99", "AirPods Pro", 7000000);
        String auctionId = result.split("\\|")[1].split("=")[1];

        AuctionService anotherService = new AuctionService();
        String list = anotherService.getAuctionList();
        String detail = anotherService.getAuctionDetail(auctionId);

        assertTrue(list.contains("AirPods Pro"));
        assertTrue(detail.contains("itemName=AirPods Pro"));
        assertTrue(detail.contains("seller=seller99"));
        assertTrue(detail.contains("startPrice=7000000"));
    }

    @Test
    void shouldPlaceBidSuccessfully() {
        String result = auctionService.placeBid("1", "abc", 17000000);

        // placeBid() trả về "BID_RESULT|status=SUCCESS|..." chứ không phải "BID_SUCCESS"
        assertTrue(result.startsWith("BID_RESULT|"));
        assertTrue(result.contains("status=SUCCESS"));
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
    void shouldConfigureAutoBidSuccessfully() {
        String result = auctionService.setAutoBid("1", "auto-user", 20000000, 500000);

        assertTrue(result.startsWith("AUTO_BID_SET|"));
        assertTrue(result.contains("auctionId=1"));
        assertTrue(result.contains("user=auto-user"));
    }

    @Test
    void shouldAutoBidWhenAnotherUserOutbids() {
        auctionService.setAutoBid("1", "auto-user", 20000000, 500000);
        auctionService.placeBid("1", "manual-user", 17000000);

        String detail = auctionService.getAuctionDetail("1");
        String history = auctionService.getBidHistory("1");

        assertTrue(detail.contains("currentPrice=17500000"));
        assertTrue(detail.contains("highestBidder=auto-user"));
        assertTrue(detail.contains("bidCount=2"));
        assertTrue(history.contains("manual-user,17000000,"));
        assertTrue(history.contains("auto-user,17500000,"));
    }

    @Test
    void shouldReadAuctionStateFromDatabaseAcrossServiceInstances() {
        auctionService.placeBid("1", "persist-user", 19000000);
        auctionService.closeAuction("1");

        AuctionService anotherService = new AuctionService();
        String detail = anotherService.getAuctionDetail("1");

        assertTrue(detail.contains("currentPrice=19000000"));
        assertTrue(detail.contains("highestBidder=persist-user"));
        assertTrue(detail.contains("status=FINISHED"));
        assertTrue(detail.contains("bidCount=1"));
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
        // getWinner() chỉ trả kết quả khi phiên đã FINISHED, còn RUNNING sẽ báo lỗi.
        auctionService.closeAuction("2");
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
        // Giả lập phiên hết giờ: đặt endTime về quá khứ.
        // Phải set trực tiếp trên field vì closeExpiredAuctions() sẽ syncAuctionFromDatabase().
        // Ta cũng cần ghi endTime mới vào DB để sync không ghi đè giá trị cũ.
        long expiredEnd = System.currentTimeMillis() - 1;
        auction.setEndTime(expiredEnd);

        // Lọc notification: chỉ lấy những cái thuộc auction "1" (vì seed data tạo 3 auctions).
        List<String> notifications = auctionService.closeExpiredAuctions()
                .stream()
                .filter(n -> n.contains("auctionId=1"))
                .toList();

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
