package com.bidding.server.core;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.common.model.AutoBid;
import com.bidding.common.model.item.Art;
import com.bidding.common.model.item.Item;
import com.bidding.server.exception.AuctionClosedException;
import com.bidding.server.exception.AuctionNotFoundException;
import com.bidding.server.exception.InvalidBidException;
import com.bidding.server.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuctionService.
 *
 * Strategy: Use reflection to inject mocked DAOs directly into
 * AuctionService fields, bypassing the constructor's DatabaseInitializer
 * and seed-data calls. An in-memory auctions map is also injected.
 *
 * Dependencies: JUnit 5 + Mockito 5
 *
 * Add to pom.xml / build.gradle:
 *   junit-jupiter 5.10+
 *   mockito-junit-jupiter 5.x
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AuctionServiceTest {

    // ── Mocks ────────────────────────────────────────────────────────────────

    @Mock private BidHistoryDAO bidHistoryDAO;
    @Mock private AuctionStateDAO auctionStateDAO;
    @Mock private AuctionRecordDAO auctionRecordDAO;
    @Mock private AutoBidDAO autoBidDAO;
    @Mock private ItemDAO itemDAO;
    @Mock private SellerAuctionDAO sellerAuctionDAO;
    @Mock private UserDAO userDAO;
    @Mock private TransactionDAO transactionDAO;

    // ── System under test ────────────────────────────────────────────────────

    private AuctionService service;

    /** In-memory auctions map that is injected into the service. */
    private Map<String, Auction> auctions;

    // ── Helpers ──────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() throws Exception {
        // Instantiate without calling the real constructor
        service = (AuctionService) createInstanceWithoutConstructor(AuctionService.class);

        auctions = new ConcurrentHashMap<>();
        injectField(service, "auctions",       auctions);
        injectField(service, "nextAuctionId",  new AtomicInteger(100));
        injectField(service, "bidHistoryDAO",  bidHistoryDAO);
        injectField(service, "auctionStateDAO",auctionStateDAO);
        injectField(service, "auctionRecordDAO",auctionRecordDAO);
        injectField(service, "autoBidDAO",     autoBidDAO);
        injectField(service, "itemDAO",        itemDAO);
        injectField(service, "sellerAuctionDAO",sellerAuctionDAO);
        injectField(service, "userDAO",        userDAO);
        injectField(service, "transactionDAO", transactionDAO);
    }

    // ════════════════════════════════════════════════════════════════════════
    // createAuction
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class CreateAuction {

        @Test
        void nullItem_returnsError() {
            String result = service.createAuction("seller1", null);
            assertEquals("ERROR|Item is null", result);
        }

        @Test
        void blankSeller_returnsError() {
            Item item = artItem("Vase", 1_000_000);
            assertEquals("ERROR|Seller username required",
                    service.createAuction("  ", item));
        }

        @Test
        void nullSeller_returnsError() {
            Item item = artItem("Vase", 1_000_000);
            assertEquals("ERROR|Seller username required",
                    service.createAuction(null, item));
        }

        @Test
        void zeroPrice_returnsError() {
            Item item = artItem("Vase", 0);
            assertEquals("ERROR|Invalid start price",
                    service.createAuction("seller1", item));
        }

        @Test
        void negativePrice_returnsError() {
            Item item = artItem("Vase", -500);
            assertEquals("ERROR|Invalid start price",
                    service.createAuction("seller1", item));
        }

        @Test
        void validItem_returnsSuccess() {
            Item item = artItem("Painting", 5_000_000);
            Item savedItem = artItemWithId(1L, "Painting", 5_000_000);

            when(itemDAO.save(item, "alice")).thenReturn(savedItem);
            stubPersistCalls("100");

            String result = service.createAuction("alice", item);

            assertTrue(result.startsWith("CREATE_AUCTION_SUCCESS"),
                    "Expected success, got: " + result);
            assertTrue(result.contains("|auctionId=100"));
            assertTrue(result.contains("|itemId=1"));
            assertEquals(1, auctions.size());
        }

        @Test
        void validItem_auctionIsOpenStatus() {
            Item item = artItem("Watch", 2_000_000);
            Item saved = artItemWithId(2L, "Watch", 2_000_000);
            when(itemDAO.save(item, "bob")).thenReturn(saved);
            stubPersistCalls("100");

            service.createAuction("bob", item);

            Auction created = auctions.get("100");
            assertNotNull(created);
            assertEquals(AuctionStatus.OPEN, created.getStatus());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // createPendingAuction
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class CreatePendingAuction {

        @Test
        void nullItem_returnsError() {
            assertEquals("ERROR|Item is null",
                    service.createPendingAuction("s", null, futureTime(), 10L));
        }

        @Test
        void blankSeller_returnsError() {
            assertEquals("ERROR|Seller username required",
                    service.createPendingAuction("", artItem("X", 1), futureTime(), 10L));
        }

        @Test
        void invalidPrice_returnsError() {
            assertEquals("ERROR|Invalid start price",
                    service.createPendingAuction("s", artItem("X", 0), futureTime(), 10L));
        }

        @Test
        void startTimeInPast_returnsError() {
            long past = System.currentTimeMillis() - 10_000;
            assertEquals("ERROR|Start time cannot be in the past",
                    service.createPendingAuction("s", artItem("X", 1000), past, 5L));
        }

        @Test
        void invalidDuration_returnsError() {
            assertEquals("ERROR|Invalid duration",
                    service.createPendingAuction("s", artItem("X", 1000), futureTime(), 0L));
        }

        @Test
        void valid_returnsPendingSuccess() {
            Item item = artItem("Sculpture", 3_000_000);
            Item saved = artItemWithId(5L, "Sculpture", 3_000_000);
            when(itemDAO.save(item, "carol")).thenReturn(saved);
            stubPersistCalls("100");

            long start = futureTime();
            String result = service.createPendingAuction("carol", item, start, 30L);

            assertTrue(result.startsWith("ADD_AUCTION_PENDING"),
                    "Unexpected: " + result);
            assertTrue(result.contains("|auctionId=100"));
            assertEquals(AuctionStatus.PENDING, auctions.get("100").getStatus());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // addAuction (legacy shortcut)
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class AddAuction {

        @Test
        void validArgs_returnsAddSuccess() {
            Item saved = artItemWithId(7L, "Laptop", 10_000_000);
            when(itemDAO.save(any(), eq("dave"))).thenReturn(saved);
            stubPersistCalls("100");

            String result = service.addAuction("dave", "Laptop", 10_000_000);
            assertTrue(result.startsWith("ADD_AUCTION_SUCCESS"), result);
            assertTrue(result.contains("|seller=dave"));
            assertTrue(result.contains("|itemName=Laptop"));
        }

        @Test
        void zeroPrice_propagatesError() {
            String result = service.addAuction("dave", "Laptop", 0);
            assertEquals("ERROR|Invalid start price", result);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // placeBid
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class PlaceBid {

        @Test
        void nullUsername_throwsInvalidBid() {
            assertThrows(InvalidBidException.class,
                    () -> service.placeBid("1", null, 5000));
        }

        @Test
        void blankUsername_throwsInvalidBid() {
            assertThrows(InvalidBidException.class,
                    () -> service.placeBid("1", "  ", 5000));
        }

        @Test
        void negativeAmount_throwsInvalidBid() {
            assertThrows(InvalidBidException.class,
                    () -> service.placeBid("1", "user", -100));
        }

        @Test
        void zeroAmount_throwsInvalidBid() {
            assertThrows(InvalidBidException.class,
                    () -> service.placeBid("1", "user", 0));
        }

        @Test
        void auctionNotFound_throwsAuctionNotFound() {
            assertThrows(AuctionNotFoundException.class,
                    () -> service.placeBid("999", "user", 1000));
        }

        @Test
        void sellerBidsOnOwnAuction_throwsInvalidBid() {
            Auction auction = runningAuction("1", "alice", 1_000_000);
            auctions.put("1", auction);

            assertThrows(InvalidBidException.class,
                    () -> service.placeBid("1", "alice", 2_000_000));
        }

        @Test
        void bidNotHigherThanCurrentPrice_throwsInvalidBid() {
            Auction auction = runningAuction("1", "seller", 5_000_000);
            auctions.put("1", auction);
            stubSyncReturnsNull("1");
            stubNoAutoBids("1");

            assertThrows(InvalidBidException.class,
                    () -> service.placeBid("1", "buyer", 4_000_000));
        }

        @Test
        void equalToCurrentPrice_throwsInvalidBid() {
            Auction auction = runningAuction("1", "seller", 5_000_000);
            auctions.put("1", auction);
            stubSyncReturnsNull("1");
            stubNoAutoBids("1");

            assertThrows(InvalidBidException.class,
                    () -> service.placeBid("1", "buyer", 5_000_000));
        }

        @Test
        void belowMinimumIncrement_throwsInvalidBid() {
            Auction auction = runningAuction("1", "seller", 1_000_000);
            auctions.put("1", auction);
            stubNoAutoBids("1");

            InvalidBidException ex = assertThrows(InvalidBidException.class,
                    () -> service.placeBid("1", "buyer", 1_004_000));

            assertEquals("Minimum valid bid is 1005000", ex.getMessage());
        }

        @Test
        void calculateMinIncrement_usesHalfPercentWithMinimumFloor() {
            assertEquals(1_000, service.calculateMinIncrement(100_000), 0.01);
            assertEquals(100_000, service.calculateMinIncrement(20_000_000), 0.01);
            assertEquals(2_500_000, service.calculateMinIncrement(500_000_000), 0.01);
        }

        @Test
        void auctionPending_throwsAuctionClosed() {
            Auction auction = auctionWithStatus("1", "seller", AuctionStatus.PENDING, 1_000_000);
            auctions.put("1", auction);

            assertThrows(AuctionClosedException.class,
                    () -> service.placeBid("1", "buyer", 2_000_000));
        }

        @Test
        void auctionFinished_throwsAuctionClosed() {
            Auction auction = auctionWithStatus("1", "seller", AuctionStatus.FINISHED, 1_000_000);
            auctions.put("1", auction);

            assertThrows(AuctionClosedException.class,
                    () -> service.placeBid("1", "buyer", 2_000_000));
        }

        @Test
        void auctionCanceled_throwsAuctionClosed() {
            Auction auction = auctionWithStatus("1", "seller", AuctionStatus.CANCELED, 1_000_000);
            auctions.put("1", auction);

            assertThrows(AuctionClosedException.class,
                    () -> service.placeBid("1", "buyer", 2_000_000));
        }

        @Test
        void auctionPaid_throwsAuctionClosed() {
            Auction auction = auctionWithStatus("1", "seller", AuctionStatus.PAID, 1_000_000);
            auctions.put("1", auction);

            assertThrows(AuctionClosedException.class,
                    () -> service.placeBid("1", "buyer", 2_000_000));
        }

        @Test
        void validBid_returnsSuccess() {
            Auction auction = runningAuction("1", "seller", 1_000_000);
            auctions.put("1", auction);
            stubSyncReturnsNull("1");
            stubNoAutoBids("1");
            when(bidHistoryDAO.countByAuctionId("1")).thenReturn(1);

            String result = service.placeBid("1", "buyer", 2_000_000);

            assertTrue(result.startsWith("BID_RESULT|status=SUCCESS"), result);
            assertEquals(2_000_000, auction.getCurrentPrice(), 0.01);
            assertEquals("buyer", auction.getHighestBidder());
        }

        @Test
        void validBid_updatesBidder() {
            Auction auction = runningAuction("2", "sellerX", 500_000);
            auctions.put("2", auction);
            stubSyncReturnsNull("2");
            stubNoAutoBids("2");
            when(bidHistoryDAO.countByAuctionId("2")).thenReturn(1);

            service.placeBid("2", "bidder1", 600_000);

            assertEquals("bidder1", auction.getHighestBidder());
        }

        @Test
        void regularBidBelowAutoBidMax_isOvertakenByProxyAutoBid() {
            Auction auction = runningAuction("20", "seller", 10_300_000);
            auction.setHighestBidder("B");
            auctions.put("20", auction);
            when(autoBidDAO.findActiveByAuction(20L))
                    .thenReturn(List.of(autoBid(20L, "B", 15_000_000, 500_000)));
            when(bidHistoryDAO.countByAuctionId("20")).thenReturn(2);

            service.placeBid("20", "C", 12_000_000);

            assertEquals("B", auction.getHighestBidder());
            assertEquals(12_060_000, auction.getCurrentPrice(), 0.01);
            verify(bidHistoryDAO).save(eq("20"), eq("C"), eq(12_000_000.0), anyLong());
            verify(bidHistoryDAO).save(eq("20"), eq("B"), eq(12_060_000.0), anyLong());
        }

        @Test
        void regularBidAboveAutoBidMax_winsImmediately() {
            Auction auction = runningAuction("21", "seller", 12_060_000);
            auction.setHighestBidder("B");
            auctions.put("21", auction);
            when(autoBidDAO.findActiveByAuction(21L))
                    .thenReturn(List.of(autoBid(21L, "B", 15_000_000, 500_000)));
            when(bidHistoryDAO.countByAuctionId("21")).thenReturn(3);

            service.placeBid("21", "C", 16_000_000);

            assertEquals("C", auction.getHighestBidder());
            assertEquals(16_000_000, auction.getCurrentPrice(), 0.01);
            verify(bidHistoryDAO, never()).save(eq("21"), eq("B"), eq(15_000_000.0), anyLong());
        }

        @Test
        void expiredAuction_throwsAuctionClosed() {
            Auction auction = expiredRunningAuction("3", "seller", 1_000_000);
            auctions.put("3", auction);
            stubSyncReturnsNull("3");
            when(bidHistoryDAO.countByAuctionId("3")).thenReturn(0);

            assertThrows(AuctionClosedException.class,
                    () -> service.placeBid("3", "buyer", 2_000_000));
        }

        @Test
        void openAuctionNotStartedYet_throwsAuctionClosed() {
            long futureStart = System.currentTimeMillis() + 60_000;
            Auction auction = new Auction("4", "seller", "Item", 1_000_000, AuctionStatus.OPEN);
            auction.setStartTimeMillis(futureStart);
            auction.setEndTime(futureStart + 300_000);
            auctions.put("4", auction);

            assertThrows(AuctionClosedException.class,
                    () -> service.placeBid("4", "buyer", 2_000_000));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // closeAuction
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class CloseAuction {

        @Test
        void notFound_returnsError() {
            assertEquals("ERROR|Auction not found", service.closeAuction("999"));
        }

        @Test
        void alreadyFinished_returnsError() {
            Auction auction = auctionWithStatus("5", "s", AuctionStatus.FINISHED, 1_000_000);
            auctions.put("5", auction);
            when(auctionStateDAO.findByAuctionId("5")).thenReturn(null);

            String result = service.closeAuction("5");
            assertTrue(result.contains("ERROR|Auction is not open"), result);
        }

        @Test
        void openAuction_closesSuccessfully() {
            Auction auction = runningAuction("6", "seller", 2_000_000);
            auction.setHighestBidder("bidder1");
            auctions.put("6", auction);
            // Return null from sync so in-memory state is used directly
            when(auctionStateDAO.findByAuctionId("6")).thenReturn(null);
            when(bidHistoryDAO.countByAuctionId("6")).thenReturn(3);

            String result = service.closeAuction("6");

            assertTrue(result.startsWith("CLOSE_AUCTION_SUCCESS"), result);
            assertTrue(result.contains("|winner=bidder1"), result);
            assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        }

        @Test
        void noHighestBidder_winnerIsNone() {
            Auction auction = runningAuction("7", "seller", 1_000_000);
            auctions.put("7", auction);
            when(auctionStateDAO.findByAuctionId("7")).thenReturn(null);
            when(bidHistoryDAO.countByAuctionId("7")).thenReturn(0);

            String result = service.closeAuction("7");
            assertTrue(result.contains("|winner=NONE"), result);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // getWinner
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class GetWinner {

        @Test
        void notFound_returnsError() {
            assertEquals("ERROR|Auction not found", service.getWinner("999"));
        }

        @Test
        void stillRunning_returnsError() {
            Auction auction = runningAuction("8", "seller", 1_000_000);
            auctions.put("8", auction);
            stubSyncReturnsNull("8");

            String result = service.getWinner("8");
            assertEquals("ERROR|Auction is still running", result);
        }

        @Test
        void finishedWithWinner_returnsWinnerInfo() {
            Auction auction = auctionWithStatus("9", "seller", AuctionStatus.FINISHED, 5_000_000);
            auction.setHighestBidder("winnerUser");
            auctions.put("9", auction);
            stubSyncReturnsNull("9");

            String result = service.getWinner("9");
            assertTrue(result.startsWith("WINNER_INFO"), result);
            assertTrue(result.contains("|winner=winnerUser"));
            assertTrue(result.contains("|finalPrice=5000000"));
        }

        @Test
        void finishedWithNoWinner_returnsNone() {
            Auction auction = auctionWithStatus("10", "seller", AuctionStatus.FINISHED, 1_000_000);
            auctions.put("10", auction);
            stubSyncReturnsNull("10");

            String result = service.getWinner("10");
            assertTrue(result.contains("|winner=NONE"), result);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // updateStatus
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class UpdateStatus {

        @Test
        void notFound_returnsFailedResult() {
            String result = service.updateStatus("999", AuctionStatus.FINISHED);
            assertTrue(result.contains("status=FAILED"), result);
            assertTrue(result.contains("Auction not found"), result);
        }

        @Test
        void nullStatus_returnsFailedResult() {
            Auction auction = runningAuction("11", "seller", 1_000_000);
            auctions.put("11", auction);
            String result = service.updateStatus("11", null);
            assertTrue(result.contains("status=FAILED"), result);
        }

        @Test
        void validStatus_returnsSuccess() {
            Auction auction = runningAuction("12", "seller", 1_000_000);
            auctions.put("12", auction);
            stubSyncReturnsNull("12");
            when(bidHistoryDAO.countByAuctionId("12")).thenReturn(0);

            String result = service.updateStatus("12", AuctionStatus.CANCELED);
            assertTrue(result.contains("status=SUCCESS"), result);
            assertTrue(result.contains("newStatus=CANCELED"), result);
            assertEquals(AuctionStatus.CANCELED, auction.getStatus());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // deleteSellerAuction
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class DeleteSellerAuction {

        @Test
        void notFound_returnsError() {
            assertEquals("ERROR|Auction not found",
                    service.deleteSellerAuction("seller", "999"));
        }

        @Test
        void wrongSeller_returnsError() {
            Auction auction = openAuction("13", "alice", 1_000_000);
            auctions.put("13", auction);

            assertEquals("ERROR|You can only delete your own auction",
                    service.deleteSellerAuction("bob", "13"));
        }

        @Test
        void hasBids_returnsError() {
            Auction auction = openAuction("14", "alice", 1_000_000);
            auctions.put("14", auction);
            when(bidHistoryDAO.countByAuctionId("14")).thenReturn(1);

            assertEquals("ERROR|Auction already has bids and cannot be deleted",
                    service.deleteSellerAuction("alice", "14"));
        }

        @Test
        void highestBidderSet_returnsError() {
            Auction auction = openAuction("15", "alice", 1_000_000);
            auction.setHighestBidder("someone");
            auctions.put("15", auction);
            when(bidHistoryDAO.countByAuctionId("15")).thenReturn(0);

            assertEquals("ERROR|Auction already has bids and cannot be deleted",
                    service.deleteSellerAuction("alice", "15"));
        }

        @Test
        void runningAuction_returnsError() {
            Auction auction = runningAuction("16", "alice", 1_000_000);
            auctions.put("16", auction);
            when(bidHistoryDAO.countByAuctionId("16")).thenReturn(0);

            assertEquals("ERROR|Auction cannot be deleted after it has started",
                    service.deleteSellerAuction("alice", "16"));
        }

        @Test
        void validDelete_removesAuction() {
            Auction auction = openAuction("17", "alice", 1_000_000);
            auctions.put("17", auction);
            when(bidHistoryDAO.countByAuctionId("17")).thenReturn(0);

            String result = service.deleteSellerAuction("alice", "17");
            assertEquals("DELETE_AUCTION_SUCCESS|auctionId=17", result);
            assertFalse(auctions.containsKey("17"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // updateSellerAuction
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class UpdateSellerAuction {

        @Test
        void notFound_returnsError() {
            assertEquals("ERROR|Auction not found",
                    service.updateSellerAuction("s", "999", artItem("X", 1000), futureTime(), 5L));
        }

        @Test
        void wrongSeller_returnsError() {
            Auction auction = openAuction("20", "alice", 1_000_000);
            auctions.put("20", auction);

            assertEquals("ERROR|You can only edit your own auction",
                    service.updateSellerAuction("bob", "20", artItem("X", 1000), futureTime(), 5L));
        }

        @Test
        void hasBids_returnsError() {
            Auction auction = openAuction("21", "alice", 1_000_000);
            auctions.put("21", auction);
            when(bidHistoryDAO.countByAuctionId("21")).thenReturn(2);

            assertEquals("ERROR|Auction already has bids and cannot be edited",
                    service.updateSellerAuction("alice", "21", artItem("X", 1000), futureTime(), 5L));
        }

        @Test
        void runningStatus_returnsError() {
            Auction auction = runningAuction("22", "alice", 1_000_000);
            auctions.put("22", auction);
            when(bidHistoryDAO.countByAuctionId("22")).thenReturn(0);

            assertEquals("ERROR|Auction cannot be edited after it has started",
                    service.updateSellerAuction("alice", "22", artItem("X", 1000), futureTime(), 5L));
        }

        @Test
        void validUpdate_returnsSuccess() {
            Auction auction = openAuction("23", "alice", 1_000_000);
            auctions.put("23", auction);
            when(bidHistoryDAO.countByAuctionId("23")).thenReturn(0);
            Item updatedItem = artItemWithId(23L, "NewItem", 2_000_000);
            when(bidHistoryDAO.countByAuctionId("23")).thenReturn(0);
            stubPersistCalls("23");

            String result = service.updateSellerAuction("alice", "23", updatedItem, futureTime(), 10L);
            assertTrue(result.startsWith("UPDATE_AUCTION_SUCCESS"), result);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // approveAuction
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class ApproveAuction {

        @Test
        void notFound_returnsError() {
            assertEquals("ERROR|Auction not found", service.approveAuction("999"));
        }

        @Test
        void notPending_returnsError() {
            Auction auction = openAuction("30", "seller", 1_000_000);
            auctions.put("30", auction);
            stubSyncReturnsNull("30");

            assertEquals("ERROR|Auction is not pending", service.approveAuction("30"));
        }

        @Test
        void pendingApprovalFails_returnsError() {
            Auction auction = auctionWithStatus("31", "seller", AuctionStatus.PENDING, 1_000_000);
            auctions.put("31", auction);
            when(auctionStateDAO.findByAuctionId("31")).thenReturn(null);
            when(auctionRecordDAO.approvePendingAuction("31")).thenReturn(false);

            assertEquals("ERROR|Auction approval failed", service.approveAuction("31"));
        }

        @Test
        void pendingApprovalSucceeds_returnsSuccess() {
            Auction auction = auctionWithStatus("32", "seller", AuctionStatus.PENDING, 1_000_000);
            auctions.put("32", auction);
            when(auctionStateDAO.findByAuctionId("32")).thenReturn(null);
            when(auctionRecordDAO.approvePendingAuction("32")).thenReturn(true);
            when(bidHistoryDAO.countByAuctionId("32")).thenReturn(0);

            String result = service.approveAuction("32");
            assertEquals("APPROVE_AUCTION_SUCCESS|auctionId=32", result);
            assertEquals(AuctionStatus.OPEN, auction.getStatus());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // findAuctionById
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class FindAuctionById {

        @Test
        void notFound_returnsNull() {
            assertNull(service.findAuctionById("999"));
        }

        @Test
        void found_returnsAuction() {
            Auction auction = runningAuction("40", "seller", 1_000_000);
            auctions.put("40", auction);
            stubSyncReturnsNull("40");

            Auction result = service.findAuctionById("40");
            assertNotNull(result);
            assertEquals("40", result.getId());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // getAuctionList
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class GetAuctionList {

        @Test
        void emptyAuctions_returnsHeaderOnly() {
            String result = service.getAuctionList();
            assertEquals("AUCTION_LIST|", result);
        }

        @Test
        void pendingAuction_excludedByDefault() {
            Auction pending = auctionWithStatus("50", "s", AuctionStatus.PENDING, 1_000_000);
            auctions.put("50", pending);

            String result = service.getAuctionList(false);
            assertFalse(result.contains(":50:") || result.startsWith("AUCTION_LIST|50:"),
                    "Pending auction should be excluded");
        }

        @Test
        void pendingAuction_includedWhenFlagSet() {
            Auction pending = auctionWithStatus("51", "s", AuctionStatus.PENDING, 1_000_000);
            pending.setItemId(51L);
            auctions.put("51", pending);
            when(itemDAO.findById(51L)).thenReturn(artItemWithId(51L, "Pending Item", 1_000_000));

            String result = service.getAuctionList(true);
            assertTrue(result.contains("51"), "Pending auction should be included");
        }

        @Test
        void openAuction_alwaysIncluded() {
            Auction open = openAuction("52", "s", 2_000_000);
            open.setItemId(52L);
            auctions.put("52", open);
            when(itemDAO.findById(52L)).thenReturn(artItemWithId(52L, "Open Item", 2_000_000));

            String result = service.getAuctionList(false);
            assertTrue(result.contains("52"), result);
        }

        @Test
        void canceledAuction_excludedByDefault() {
            Auction canceled = auctionWithStatus("53", "s", AuctionStatus.CANCELED, 1_000_000);
            auctions.put("53", canceled);

            String result = service.getAuctionList(false);
            assertFalse(result.contains("53"), "Canceled auction should be excluded");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // getAuctionDetail
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class GetAuctionDetail {

        @Test
        void notFound_returnsError() {
            assertEquals("ERROR|Auction not found", service.getAuctionDetail("999"));
        }

        @Test
        void found_returnsDetailString() {
            Auction auction = runningAuction("60", "seller", 3_000_000);
            auction.setHighestBidder("topBidder");
            auction.setItemId(60L);
            auctions.put("60", auction);
            stubSyncReturnsNull("60");
            when(itemDAO.findById(60L)).thenReturn(artItemWithId(60L, "Jade Vase", 3_000_000));
            when(itemDAO.resolveInformation1(any())).thenReturn("Unknown");
            when(itemDAO.resolveInformation2(any())).thenReturn("2024");
            when(bidHistoryDAO.countByAuctionId("60")).thenReturn(5);

            String result = service.getAuctionDetail("60");
            assertTrue(result.startsWith("AUCTION_DETAIL"), result);
            assertTrue(result.contains("|seller=seller"));
            assertTrue(result.contains("|highestBidder=topBidder"));
            assertTrue(result.contains("|bidCount=5"));
        }

        @Test
        void nullHighestBidder_showsNone() {
            Auction auction = runningAuction("61", "seller", 1_000_000);
            auction.setItemId(61L);
            auctions.put("61", auction);
            stubSyncReturnsNull("61");
            when(itemDAO.findById(61L)).thenReturn(null);
            when(bidHistoryDAO.countByAuctionId("61")).thenReturn(0);

            String result = service.getAuctionDetail("61");
            assertTrue(result.contains("|highestBidder=NONE"), result);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // getProductInfo
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class GetProductInfo {

        @Test
        void notFound_returnsError() {
            assertEquals("ERROR|Auction not found", service.getProductInfo("999"));
        }

        @Test
        void found_returnsProductInfo() {
            Auction auction = runningAuction("70", "sellerA", 4_000_000);
            auction.setHighestBidder("bidderZ");
            auctions.put("70", auction);
            stubSyncReturnsNull("70");
            when(bidHistoryDAO.countByAuctionId("70")).thenReturn(2);

            String result = service.getProductInfo("70");
            assertTrue(result.startsWith("PRODUCT_INFO"), result);
            assertTrue(result.contains("|seller=sellerA"));
            assertTrue(result.contains("|highestBidder=bidderZ"));
            assertTrue(result.contains("|bidCount=2"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // getBidHistory
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class GetBidHistory {

        @Test
        void notFound_returnsError() {
            assertEquals("ERROR|Auction not found", service.getBidHistory("999"));
        }

        @Test
        void noHistory_returnsEmptyEntries() {
            auctions.put("80", runningAuction("80", "s", 1_000_000));
            when(bidHistoryDAO.findByAuctionId("80")).thenReturn(Collections.emptyList());

            String result = service.getBidHistory("80");
            assertTrue(result.startsWith("BID_HISTORY|auctionId=80"), result);
            assertTrue(result.endsWith("|entries="), result);
        }

        @Test
        void withHistory_returnsEntries() {
            auctions.put("81", runningAuction("81", "s", 1_000_000));
            BidRecord r1 = new BidRecord("user1", 1_500_000, 1000L);
            BidRecord r2 = new BidRecord("user2", 2_000_000, 2000L);
            when(bidHistoryDAO.findByAuctionId("81")).thenReturn(List.of(r1, r2));

            String result = service.getBidHistory("81");
            assertTrue(result.contains("user1,1500000"), result);
            assertTrue(result.contains("user2,2000000"), result);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // setAutoBid
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class SetAutoBid {

        @Test
        void auctionNotFound_returnsError() {
            assertEquals("ERROR|Auction not found",
                    service.setAutoBid("999", "user", 5_000_000, 100_000));
        }

        @Test
        void negativeIncrement_returnsError() {
            Auction auction = runningAuction("90", "seller", 1_000_000);
            auctions.put("90", auction);
            when(auctionStateDAO.findByAuctionId("90")).thenReturn(null);

            assertEquals("ERROR|Increment invalid",
                    service.setAutoBid("90", "user", 5_000_000, -1));
        }

        @Test
        void notRunning_returnsError() {
            // PENDING status — cannot become RUNNING via applyTimeBasedStatus
            Auction auction = auctionWithStatus("91", "seller", 1_000_000, AuctionStatus.PENDING);
            auctions.put("91", auction);
            when(auctionStateDAO.findByAuctionId("91")).thenReturn(null);

            assertEquals("ERROR|Auto-bid is only available when auction is running",
                    service.setAutoBid("91", "user", 5_000_000, 100_000));
        }

        @Test
        void maxBidNotHigherThanCurrentPrice_returnsError() {
            Auction auction = runningAuction("92", "seller", 3_000_000);
            auctions.put("92", auction);
            when(auctionStateDAO.findByAuctionId("92")).thenReturn(null);

            assertEquals("ERROR|Max bid must be at least 3015000",
                    service.setAutoBid("92", "user", 2_000_000, 100_000));
        }

        @Test
        void incrementBelowMinimumIncrement_returnsError() {
            Auction auction = runningAuction("94", "seller", 1_000_000);
            auctions.put("94", auction);
            when(auctionStateDAO.findByAuctionId("94")).thenReturn(null);

            assertEquals("ERROR|Increment too small",
                    service.setAutoBid("94", "buyer", 5_000_000, 4_000));
        }

        @Test
        void valid_noCurrentHighestBidder_setsAutoBid() {
            Auction auction = runningAuction("93", "seller", 1_000_000);
            auctions.put("93", auction);
            when(auctionStateDAO.findByAuctionId("93")).thenReturn(null);
            when(autoBidDAO.findActiveByAuction(93L)).thenReturn(Collections.emptyList());

            String result = service.setAutoBid("93", "buyer", 5_000_000, 100_000);
            assertTrue(result.startsWith("AUTO_BID_SET"), result);
            assertTrue(result.contains("|active=true"), result);
        }

        @Test
        void twoAutoBids_highestMaxWinsAtRunnerUpPlusMinimumIncrement() {
            Auction auction = runningAuction("95", "seller", 1_000_000);
            auctions.put("95", auction);
            when(auctionStateDAO.findByAuctionId("95")).thenReturn(null);
            when(autoBidDAO.findActiveByAuction(95L)).thenReturn(List.of(
                    autoBid(95L, "A", 10_000_000, 500_000),
                    autoBid(95L, "B", 15_000_000, 500_000)
            ));
            when(bidHistoryDAO.countByAuctionId("95")).thenReturn(2);

            String result = service.setAutoBid("95", "B", 15_000_000, 500_000);

            assertTrue(result.startsWith("AUTO_BID_SET"), result);
            assertEquals("B", auction.getHighestBidder());
            assertEquals(10_050_000, auction.getCurrentPrice(), 0.01);
            assertTrue(auction.getCurrentPrice() < 15_000_000);
            verify(bidHistoryDAO).save(eq("95"), eq("B"), eq(10_050_000.0), anyLong());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // getAutoBid / disableAutoBid
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class AutoBidStatus {

        @Test
        void getAutoBid_blankAuctionId_returnsError() {
            assertEquals("ERROR|Auction id required",
                    service.getAutoBid("  ", "user"));
        }

        @Test
        void getAutoBid_notFound_returnsInactive() {
            when(autoBidDAO.findOne(100L, "user")).thenReturn(null);
            String result = service.getAutoBid("100", "user");
            assertTrue(result.contains("|active=false"), result);
        }

        @Test
        void getAutoBid_found_returnsDetails() {
            AutoBid ab = new AutoBid(100L, "user", 5_000_000, 200_000);
            when(autoBidDAO.findOne(100L, "user")).thenReturn(ab);
            String result = service.getAutoBid("100", "user");
            assertTrue(result.contains("|maxBid=5000000"), result);
            assertTrue(result.contains("|increment=200000"), result);
        }

        @Test
        void disableAutoBid_blankAuctionId_returnsError() {
            assertEquals("ERROR|Auction id required",
                    service.disableAutoBid("", "user"));
        }

        @Test
        void disableAutoBid_valid_returnsDisabled() {
            String result = service.disableAutoBid("100", "user");
            assertTrue(result.startsWith("AUTO_BID_DISABLED"), result);
            assertTrue(result.contains("|active=false"), result);
            verify(autoBidDAO).disable(100L, "user");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // payAuction
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class PayAuction {

        @Test
        void blankAuctionId_returnsFailed() {
            assertTrue(service.payAuction("", "user").contains("status=FAILED"));
        }

        @Test
        void blankUsername_returnsFailed() {
            assertTrue(service.payAuction("1", "").contains("status=FAILED"));
        }

        @Test
        void notFound_returnsFailed() {
            assertTrue(service.payAuction("999", "user").contains("status=FAILED"));
        }

        @Test
        void alreadyPaid_returnsFailed() {
            Auction auction = auctionWithStatus("101", "seller", AuctionStatus.PAID, 1_000_000);
            auctions.put("101", auction);
            stubSyncReturnsNull("101");

            String result = service.payAuction("101", "winner");
            assertTrue(result.contains("status=FAILED"), result);
            assertTrue(result.contains("already paid"), result);
        }

        @Test
        void canceled_returnsFailed() {
            Auction auction = auctionWithStatus("102", "seller", AuctionStatus.CANCELED, 1_000_000);
            auctions.put("102", auction);
            stubSyncReturnsNull("102");

            assertTrue(service.payAuction("102", "winner").contains("status=FAILED"));
        }

        @Test
        void notFinished_returnsFailed() {
            Auction auction = runningAuction("103", "seller", 1_000_000);
            auctions.put("103", auction);
            stubSyncReturnsNull("103");

            assertTrue(service.payAuction("103", "winner").contains("status=FAILED"));
        }

        @Test
        void notTheWinner_returnsFailed() {
            Auction auction = auctionWithStatus("104", "seller", AuctionStatus.FINISHED, 1_000_000);
            auction.setHighestBidder("realWinner");
            auctions.put("104", auction);
            stubSyncReturnsNull("104");

            assertTrue(service.payAuction("104", "otherUser").contains("status=FAILED"));
        }

        @Test
        void insufficientBalance_canceledAndFailed() {
            Auction auction = auctionWithStatus("105", "seller", AuctionStatus.FINISHED, 2_000_000);
            auction.setHighestBidder("buyer");
            auctions.put("105", auction);
            stubSyncReturnsNull("105");
            when(userDAO.getBalanceByUsername("buyer")).thenReturn(500_000.0);
            when(bidHistoryDAO.countByAuctionId("105")).thenReturn(1);

            String result = service.payAuction("105", "buyer");
            assertTrue(result.contains("status=FAILED"), result);
            assertTrue(result.contains("CANCELED"), result);
            assertEquals(AuctionStatus.CANCELED, auction.getStatus());
        }

        @Test
        void sufficientBalance_paysSuccessfully() {
            Auction auction = auctionWithStatus("106", "seller", AuctionStatus.FINISHED, 2_000_000);
            auction.setHighestBidder("buyer");
            auctions.put("106", auction);
            stubSyncReturnsNull("106");
            when(userDAO.getBalanceByUsername("buyer")).thenReturn(5_000_000.0);
            when(userDAO.getBalanceByUsername("seller")).thenReturn(1_000_000.0);
            when(bidHistoryDAO.countByAuctionId("106")).thenReturn(1);

            String result = service.payAuction("106", "buyer");
            assertTrue(result.contains("status=SUCCESS"), result);
            assertTrue(result.contains("newStatus=PAID"), result);
            assertEquals(AuctionStatus.PAID, auction.getStatus());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // getSellerAuctionList
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class GetSellerAuctionList {

        @Test
        void blankSeller_returnsError() {
            assertEquals("ERROR|Seller username required",
                    service.getSellerAuctionList(""));
        }

        @Test
        void sellerWithAuctions_returnsMyAuctions() {
            Auction a1 = openAuction("110", "alice", 1_000_000);
            a1.setItemId(110L);
            auctions.put("110", a1);
            stubSyncReturnsNull("110");
            when(itemDAO.findById(110L)).thenReturn(artItemWithId(110L, "Vase", 1_000_000));
            when(itemDAO.resolveInformation1(any())).thenReturn("Info1");
            when(itemDAO.resolveInformation2(any())).thenReturn("Info2");
            when(bidHistoryDAO.countByAuctionId("110")).thenReturn(0);

            String result = service.getSellerAuctionList("alice");
            assertTrue(result.startsWith("MY_AUCTIONS|"), result);
            assertTrue(result.contains("110"), result);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // getAccountAuctionList
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class GetAccountAuctionList {

        @Test
        void blankUsername_returnsError() {
            assertEquals("ERROR|Username required",
                    service.getAccountAuctionList(""));
        }

        @Test
        void noBidHistory_returnsEmptyList() {
            when(bidHistoryDAO.findLatestBidTimesByBidder("alice")).thenReturn(Collections.emptyMap());
            String result = service.getAccountAuctionList("alice");
            assertEquals("ACCOUNT_AUCTIONS|", result);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // closeExpiredAuctions
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    class CloseExpiredAuctions {

        @Test
        void noAuctions_returnsEmptyList() {
            assertTrue(service.closeExpiredAuctions().isEmpty());
        }

        @Test
        void expiredRunningAuction_getsClosedAndNotified() {
            Auction auction = expiredRunningAuction("120", "seller", 2_000_000);
            auction.setHighestBidder("topBidder");
            auctions.put("120", auction);
            when(auctionStateDAO.findByAuctionId("120")).thenReturn(null);
            when(bidHistoryDAO.countByAuctionId("120")).thenReturn(2);

            List<String> notifications = service.closeExpiredAuctions();

            assertTrue(notifications.stream().anyMatch(n -> n.startsWith("AUCTION_CLOSED")),
                    "Expected AUCTION_CLOSED notification, got: " + notifications);
            assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        }

        @Test
        void pendingAuctionBeforeStart_notClosed() {
            Auction auction = auctionWithStatus("121", "seller", AuctionStatus.PENDING, 1_000_000);
            long futureStart = System.currentTimeMillis() + 600_000;
            auction.setStartTimeMillis(futureStart);
            auction.setEndTime(futureStart + 300_000);
            auctions.put("121", auction);
            when(auctionStateDAO.findByAuctionId("121")).thenReturn(null);

            List<String> notifications = service.closeExpiredAuctions();
            assertFalse(notifications.stream().anyMatch(n -> n.contains("AUCTION_CLOSED")));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helper factories
    // ════════════════════════════════════════════════════════════════════════

    private Item artItem(String name, double price) {
        return new Art(name, price, "", "Unknown", 2024);
    }

    private Item artItemWithId(long id, String name, double price) {
        Art art = new Art(name, price, "", "Unknown", 2024);
        setFieldSilently(art, "id", id);
        return art;
    }

    private Auction openAuction(String id, String seller, double price) {
        Auction a = new Auction(id, seller, "TestItem", price, AuctionStatus.OPEN);
        long start = System.currentTimeMillis() - 1_000;
        a.setStartTimeMillis(start);
        a.setEndTime(start + 300_000);
        return a;
    }

    private Auction runningAuction(String id, String seller, double price) {
        Auction a = new Auction(id, seller, "TestItem", price, AuctionStatus.RUNNING);
        long start = System.currentTimeMillis() - 60_000;
        a.setStartTimeMillis(start);
        a.setEndTime(System.currentTimeMillis() + 300_000);
        return a;
    }

    private Auction expiredRunningAuction(String id, String seller, double price) {
        Auction a = new Auction(id, seller, "TestItem", price, AuctionStatus.RUNNING);
        long past = System.currentTimeMillis() - 600_000;
        a.setStartTimeMillis(past);
        a.setEndTime(past + 300_000); // end time already passed
        return a;
    }

    private Auction auctionWithStatus(String id, String seller, AuctionStatus status, double price) {
        Auction a = new Auction(id, seller, "TestItem", price, status);
        a.setStartTimeMillis(System.currentTimeMillis() - 60_000);
        a.setEndTime(System.currentTimeMillis() + 300_000);
        return a;
    }

    private Auction auctionWithStatus(String id, String seller, double price, AuctionStatus status) {
        return auctionWithStatus(id, seller, status, price);
    }

    private long futureTime() {
        return System.currentTimeMillis() + 600_000;
    }

    // ── Stub helpers ─────────────────────────────────────────────────────────

    private void stubPersistCalls(String auctionId) {
        when(bidHistoryDAO.countByAuctionId(auctionId)).thenReturn(0);
        doNothing().when(auctionStateDAO).upsert(any(), anyInt());
        doNothing().when(auctionRecordDAO).updateState(any());
    }

    private void stubSyncReturnsNull(String auctionId) {
        when(auctionStateDAO.findByAuctionId(auctionId)).thenReturn(null);
    }

    private void stubNoAutoBids(String auctionId) {
        when(autoBidDAO.findActiveByAuction(Long.parseLong(auctionId)))
                .thenReturn(Collections.emptyList());
    }

    private AutoBid autoBid(long auctionId, String username, double maxBid, double increment) {
        return new AutoBid(auctionId, username, maxBid, increment);
    }

    private void stubSyncSnapshotWithStatus(
            String auctionId, AuctionStatus status, double price, String seller, int bidCount) {

        AuctionStateDAO.AuctionStateSnapshot snap = mock(AuctionStateDAO.AuctionStateSnapshot.class);
        when(snap.currentPrice()).thenReturn(price);
        when(snap.status()).thenReturn(status);
        when(snap.highestBidder()).thenReturn(null);
        when(snap.startTimeMillis()).thenReturn(System.currentTimeMillis() - 60_000L);
        when(snap.durationMinutes()).thenReturn(5);
        when(snap.endTimeMillis()).thenReturn(System.currentTimeMillis() + 300_000L);
        when(snap.bidCount()).thenReturn(bidCount);
        when(snap.sellerUsername()).thenReturn(seller);
        when(snap.itemName()).thenReturn("TestItem");
        when(snap.startPrice()).thenReturn(price);
        when(snap.auctionId()).thenReturn(auctionId);
        when(auctionStateDAO.findByAuctionId(auctionId)).thenReturn(snap);
        doNothing().when(auctionStateDAO).upsert(any(), anyInt());
        doNothing().when(auctionRecordDAO).updateState(any());
    }

    // ── Reflection utilities ──────────────────────────────────────────────

    private static Object createInstanceWithoutConstructor(Class<?> clazz) throws Exception {
        // Use Objenesis-style if available, otherwise sun.misc.Unsafe
        try {
            Class<?> unsafe = Class.forName("sun.misc.Unsafe");
            Field f = unsafe.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object u = f.get(null);
            return unsafe.getMethod("allocateInstance", Class.class).invoke(u, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Cannot instantiate " + clazz + " without constructor", e);
        }
    }

    private static void injectField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field '" + fieldName + "' not found in " + target.getClass());
    }

    private static void setFieldSilently(Object target, String fieldName, Object value) {
        try {
            injectField(target, fieldName, value);
        } catch (Exception ignored) { /* best-effort for item ID */ }
    }
}
