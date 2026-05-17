package com.bidding.server.core;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.common.model.AutoBid;
import com.bidding.common.model.item.Art;
import com.bidding.common.model.item.Item;
import com.bidding.server.database.DatabaseInitializer;
import com.bidding.server.exception.AuctionClosedException;
import com.bidding.server.exception.AuctionNotFoundException;
import com.bidding.server.exception.InvalidBidException;
import com.bidding.server.repository.AuctionRecordDAO;
import com.bidding.server.repository.AuctionStateDAO;
import com.bidding.server.repository.AutoBidDAO;
import com.bidding.server.repository.BidHistoryDAO;
import com.bidding.server.repository.ItemDAO;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AuctionService {

    private static final long ANTI_SNIPE_THRESHOLD_MS = 30_000L;
    private static final long ANTI_SNIPE_EXTENSION_MS = 60_000L;
    private static final int DEFAULT_DURATION_MINUTES = 5;

    private final Map<String, Auction> auctions =
            new ConcurrentHashMap<>();

    private final AtomicInteger nextAuctionId;

    private final BidHistoryDAO bidHistoryDAO;
    private final AuctionStateDAO auctionStateDAO;
    private final AuctionRecordDAO auctionRecordDAO;
    private final AutoBidDAO autoBidDAO;
    private final ItemDAO itemDAO;

    public AuctionService() {

        DatabaseInitializer.initialize();

        this.bidHistoryDAO = new BidHistoryDAO();
        this.auctionStateDAO = new AuctionStateDAO();
        this.auctionRecordDAO = new AuctionRecordDAO();
        this.autoBidDAO = new AutoBidDAO();
        this.itemDAO = new ItemDAO();

        loadPersistedRuntimeAuctions();

        if (auctions.isEmpty()) {
            seedData();
        }

        long maxId = auctionRecordDAO.findMaxAuctionId();

        this.nextAuctionId =
                new AtomicInteger(Math.toIntExact(maxId + 1));
    }

    private void seedData() {

        addInitialAuction("seller1", "iPhone 15", 15000000, AuctionStatus.OPEN);
        addInitialAuction("seller2", "MacBook Pro", 25000000, AuctionStatus.OPEN);
        addInitialAuction("seller3", "Oil Painting", 5000000, AuctionStatus.OPEN);
    }

    private void addInitialAuction(
            String sellerUsername,
            String itemName,
            double startPrice,
            AuctionStatus status
    ) {

        String id =
                String.valueOf(
                        auctionRecordDAO.findMaxAuctionId() + 1
                );

        if (auctionRecordDAO.existsById(id)) {
            return;
        }

        Auction auction = new Auction(
                id,
                sellerUsername,
                itemName,
                startPrice,
                status
        );
        auction.setDurationMinutes(DEFAULT_DURATION_MINUTES);
        Item item = new Art(
                itemName,
                "",
                startPrice,
                "",
                "Unknown",
                Calendar.getInstance().get(Calendar.YEAR)
        );
        Item savedItem = itemDAO.save(item, sellerUsername);
        auction.setItemId(savedItem.getId());
        auctions.put(id, auction);

        auctionRecordDAO.save(
                id,
                savedItem.getId(),
                sellerUsername,
                startPrice,
                auction.getStartTimeMillis(),
                auction.getEndTime(),
                auction.getDurationMinutes(),
                status
        );

        persistAuctionState(auction, 0);
    }

    private void loadPersistedRuntimeAuctions() {

        for (AuctionStateDAO.AuctionStateSnapshot snapshot
                : auctionStateDAO.findAll()) {

            Auction auction = new Auction(
                    snapshot.auctionId(),
                    snapshot.sellerUsername(),
                    snapshot.itemName(),
                    snapshot.startPrice(),
                    snapshot.status()
            );

            auction.setCurrentPrice(
                    snapshot.currentPrice()
            );

            auction.setHighestBidder(
                    snapshot.highestBidder()
            );

            auction.setStartTimeMillis(
                    snapshot.startTimeMillis()
            );

            auction.setEndTime(
                    snapshot.endTimeMillis()
            );

            auctions.put(
                    snapshot.auctionId(),
                    auction
            );
        }
    }

    public String createAuction(
            String sellerUsername,
            Item item
    ) {

        if (item == null) {
            return "ERROR|Item is null";
        }

        if (sellerUsername == null
                || sellerUsername.isBlank()) {

            return "ERROR|Seller username required";
        }

        if (item.getStartingPrice() <= 0) {
            return "ERROR|Invalid start price";
        }

        Item savedItem =
                itemDAO.save(item, sellerUsername);

        String auctionId =
                String.valueOf(
                        nextAuctionId.getAndIncrement()
                );

        Auction auction = new Auction(
                auctionId,
                sellerUsername,
                savedItem.getName(),
                savedItem.getStartingPrice(),
                AuctionStatus.OPEN
        );
        auction.setDurationMinutes(DEFAULT_DURATION_MINUTES);

        auction.setItemId(savedItem.getId());

        auctions.put(auctionId, auction);

        auctionRecordDAO.save(
                auctionId,
                savedItem.getId(),
                sellerUsername,
                savedItem.getStartingPrice(),
                auction.getStartTimeMillis(),
                auction.getEndTime(),
                auction.getDurationMinutes(),
                AuctionStatus.OPEN
        );

        persistAuctionState(auction, 0);

        return "CREATE_AUCTION_SUCCESS"
                + "|auctionId=" + auctionId
                + "|itemId=" + savedItem.getId()
                + "|itemType=" + savedItem.getItemType();
    }

    public String createPendingAuction(
            String sellerUsername,
            Item item,
            long startTime,
            long durationMinutes
    ) {

        if (item == null) {
            return "ERROR|Item is null";
        }

        if (sellerUsername == null || sellerUsername.isBlank()) {
            return "ERROR|Seller username required";
        }

        if (item.getStartingPrice() <= 0) {
            return "ERROR|Invalid start price";
        }

        if (startTime <= 0) {
            return "ERROR|Invalid start time";
        }

        if (durationMinutes <= 0) {
            return "ERROR|Invalid duration";
        }

        Item savedItem = itemDAO.save(item, sellerUsername);

        String auctionId = String.valueOf(nextAuctionId.getAndIncrement());

        Auction auction = new Auction(
                auctionId,
                sellerUsername,
                savedItem.getName(),
                savedItem.getStartingPrice(),
                AuctionStatus.PENDING
        );
        auction.setItemId(savedItem.getId());
        auction.setStartTimeMillis(startTime);
        auction.setDurationMinutes(Math.toIntExact(durationMinutes));

        long endTime = startTime + durationMinutes * 60_000L;
        auction.setEndTime(endTime);

        auctions.put(auctionId, auction);

        auctionRecordDAO.save(
                auctionId,
                savedItem.getId(),
                sellerUsername,
                savedItem.getStartingPrice(),
                startTime,
                endTime,
                auction.getDurationMinutes(),
                AuctionStatus.PENDING
        );

        persistAuctionState(auction, 0);

        return "ADD_AUCTION_PENDING|auctionId=" + auctionId;
    }

    public String addAuction(
            String sellerUsername,
            String itemName,
            double startPrice
    ) {

        Item item = new Art(
                itemName,
                "No description",
                startPrice,
                "",
                "Unknown",
                Calendar.getInstance().get(Calendar.YEAR)
        );

        String response = createAuction(sellerUsername, item);

        if (!response.startsWith("CREATE_AUCTION_SUCCESS")) {
            return response;
        }

        String auctionId = extractField(response, "auctionId");

        return "ADD_AUCTION_SUCCESS"
                + "|id=" + auctionId
                + "|seller=" + sellerUsername
                + "|itemName=" + itemName
                + "|startPrice=" + (long) startPrice;
    }

    public String closeAuction(String auctionId) {

        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        synchronized (auction) {

            AuctionStateDAO.AuctionStateSnapshot state =
                    syncAuctionFromDatabase(auction);

            if (!isActiveAuction(auction)) {
                return "ERROR|Auction is not open";
            }

            return finishAuction(
                    auction,
                    resolveBidCount(auctionId, state),
                    "CLOSE_AUCTION_SUCCESS"
            );
        }
    }

    public String updateStatus(String auctionId, AuctionStatus newStatus) {
        JsonObject response = new JsonObject();
        response.addProperty("command", "UPDATE_STATUS_RESULT");
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            response.addProperty("status", "FAILED");
            response.addProperty("message", "Auction not found");
            return response.toString();
        }
        if (newStatus == null) {
            response.addProperty("status", "FAILED");
            response.addProperty("message", "Invalid status");
            return response.toString();
        }
        synchronized (auction) {
            syncAuctionFromDatabase(auction);
            auction.setStatus(newStatus);
            persistAuctionState(auction);
        }
        response.addProperty("status", "SUCCESS");
        response.addProperty("message", "Auction " + auctionId + " is now " + newStatus.name());
        return response.toString();
    }

    public String getWinner(String auctionId) {

        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        syncAuctionFromDatabase(auction);

        if (isActiveAuction(auction)) {
            return "ERROR|Auction is still running";
        }

        String winner =
                auction.getHighestBidder() == null
                        ? "NONE"
                        : auction.getHighestBidder();

        return "WINNER_INFO|auctionId="
                + auctionId
                + "|winner=" + winner
                + "|finalPrice="
                + (long) auction.getCurrentPrice()
                + "|status=" + auction.getStatus();
    }

    public String getAuctionList() {
        return getAuctionList(false);
    }

    public String getAuctionList(boolean includePending) {
        JsonObject response = new JsonObject();
        response.addProperty("command", "AUCTION_LIST_RESULT");
        JsonArray itemsArray = new JsonArray();

        boolean first = true;
        for (Auction auction : auctions.values()) {
            syncAuctionFromDatabase(auction);

            if (!includePending
                    && auction.getStatus() != AuctionStatus.OPEN
                    && auction.getStatus() != AuctionStatus.RUNNING) {
                continue;
            }

            // Tạo 1 thẻ JSON cho mỗi sản phẩm
            JsonObject item = new JsonObject();
            item.addProperty("id", auction.getId());
            item.addProperty("itemName", auction.getItemName());
            item.addProperty("currentPrice", auction.getCurrentPrice());
            item.addProperty("status", auction.getStatus().name());

            // Nhét vào mảng
            itemsArray.add(item);
        }

        response.add("items", itemsArray);
        return response.toString();
    }

    public String approveAuction(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        synchronized (auction) {
            syncAuctionFromDatabase(auction);

            if (auction.getStatus() != AuctionStatus.PENDING) {
                return "ERROR|Auction is not pending";
            }

            if (!auctionRecordDAO.approvePendingAuction(auctionId)) {
                return "ERROR|Auction approval failed";
            }

            auction.setStatus(AuctionStatus.OPEN);
        }

        return "APPROVE_AUCTION_SUCCESS|auctionId=" + auctionId;
    }

    public Auction findAuctionById(String auctionId) {

        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return null;
        }

        syncAuctionFromDatabase(auction);

        return auction;
    }

    public String placeBid(
            String auctionId,
            String username,
            double amount
    ) {

        if (username == null
                || username.isBlank()) {

            throw new InvalidBidException(
                    "Username is required"
            );
        }

        if (amount <= 0) {
            throw new InvalidBidException(
                    "Invalid bid amount"
            );
        }

        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            throw new AuctionNotFoundException(
                    "Auction not found"
            );
        }

        if (auction.getSellerUsername().equals(username)) {
            throw new InvalidBidException(
                    "Seller cannot bid on their own auction"
            );
        }

        synchronized (auction) {

            long now = System.currentTimeMillis();

            if (isActiveAuction(auction)
                    && now >= auction.getEndTime()) {

                finishAuction(
                        auction,
                        bidHistoryDAO.countByAuctionId(
                                auctionId
                        ),
                        "AUCTION_CLOSED"
                );

                throw new AuctionClosedException(
                        "Auction is not available"
                );
            }

            if (auction.getStatus()
                    == AuctionStatus.PENDING
                    || auction.getStatus()
                    == AuctionStatus.FINISHED
                    || auction.getStatus()
                    == AuctionStatus.PAID
                    || auction.getStatus()
                    == AuctionStatus.CANCELED) {

                throw new AuctionClosedException(
                        "Auction is not available"
                );
            }

            if (auction.getStatus()
                    == AuctionStatus.OPEN) {

                auction.setStatus(
                        AuctionStatus.RUNNING
                );
            }

            if (amount <= auction.getCurrentPrice()) {

                throw new InvalidBidException(
                        "Bid amount must be greater than current price"
                );
            }

            AuctionStatus previousStatus =
                    auction.getStatus();

            double previousPrice =
                    auction.getCurrentPrice();

            String previousHighestBidder =
                    auction.getHighestBidder();

            long previousEndTime =
                    auction.getEndTime();

            auction.setCurrentPrice(amount);
            auction.setHighestBidder(username);

            applyAntiSniping(auction, now);

            try {

                bidHistoryDAO.save(
                        auctionId,
                        username,
                        amount,
                        now
                );

                persistAuctionState(auction);

            } catch (RuntimeException e) {

                auction.setStatus(previousStatus);

                auction.setCurrentPrice(previousPrice);

                auction.setHighestBidder(
                        previousHighestBidder
                );

                auction.setEndTime(previousEndTime);

                throw e;
            }

            auction.addBidRecord(
                    new BidRecord(
                            username,
                            amount,
                            now
                    )
            );

            processAutoBidChain(auction, now);

            JsonObject response = new JsonObject();
            response.addProperty("command", "BID_RESULT");
            response.addProperty("status", "SUCCESS");
            response.addProperty("auctionId", auctionId);
            response.addProperty("user", username);
            response.addProperty("amount", (long) amount);
            response.addProperty("message", "Bid placed successfully");

            return response.toString();
        }
    }

    public String setAutoBid(
            String auctionId,
            String username,
            double maxBid,
            double increment
    ) {

        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        synchronized (auction) {

            syncAuctionFromDatabase(auction);

            if (increment <= 0) {
                return "ERROR|Increment invalid";
            }

            if (maxBid <= auction.getCurrentPrice()) {

                return "ERROR|Max bid must be greater than current price";
            }

            AutoBid autoBid = new AutoBid(
                    Long.parseLong(auctionId),
                    username,
                    maxBid,
                    increment
            );

            autoBidDAO.upsert(autoBid);

            if (auction.getHighestBidder() != null
                    && !username.equals(
                    auction.getHighestBidder()
            )) {

                processAutoBidChain(
                        auction,
                        System.currentTimeMillis()
                );
            }

            return "AUTO_BID_SET"
                    + "|auctionId=" + auctionId
                    + "|user=" + username;
        }
    }

    public List<String> closeExpiredAuctions() {

        List<String> notifications =
                new ArrayList<>();

        long now = System.currentTimeMillis();

        for (Auction auction : auctions.values()) {

            synchronized (auction) {

                long localEndTime = auction.getEndTime();
                syncAuctionFromDatabase(auction);
                if (localEndTime < auction.getEndTime()) {
                    auction.setEndTime(localEndTime);
                }

                if (isActiveAuction(auction)
                        && now >= auction.getEndTime()) {

                    notifications.add(
                            finishAuction(
                                    auction,
                                    bidHistoryDAO.countByAuctionId(
                                            auction.getId()
                                    ),
                                    "AUCTION_CLOSED"
                            )
                    );
                }
            }
        }

        return notifications;
    }

    private boolean isActiveAuction(Auction auction) {

        return auction.getStatus()
                == AuctionStatus.OPEN
                || auction.getStatus()
                == AuctionStatus.RUNNING
                || auction.getStatus()
                == AuctionStatus.PENDING;
    }

    private String finishAuction(
            Auction auction,
            int bidCount,
            String messageType
    ) {

        if (auction.getStatus()
                == AuctionStatus.FINISHED) {

            return messageType
                    + "|auctionId="
                    + auction.getId();
        }

        auction.setStatus(AuctionStatus.FINISHED);

        persistAuctionState(auction, bidCount);

        String winner =
                auction.getHighestBidder() == null
                        ? "NONE"
                        : auction.getHighestBidder();

        return messageType
                + "|auctionId=" + auction.getId()
                + "|winner=" + winner
                + "|finalPrice="
                + (long) auction.getCurrentPrice();
    }

    private AuctionStateDAO.AuctionStateSnapshot
    syncAuctionFromDatabase(Auction auction) {

        AuctionStateDAO.AuctionStateSnapshot state =
                auctionStateDAO.findByAuctionId(
                        auction.getId()
                );

        if (state == null) {
            return null;
        }

        auction.setCurrentPrice(
                state.currentPrice()
        );

        auction.setStatus(
                state.status()
        );

        auction.setHighestBidder(
                state.highestBidder()
        );

        auction.setStartTimeMillis(
                state.startTimeMillis()
        );

        auction.setDurationMinutes(
                state.durationMinutes()
        );

        auction.setEndTime(
                state.endTimeMillis()
        );

        return state;
    }

    private int resolveBidCount(
            String auctionId,
            AuctionStateDAO.AuctionStateSnapshot state
    ) {

        return state != null
                ? state.bidCount()
                : bidHistoryDAO.countByAuctionId(
                auctionId
        );
    }

    private void persistAuctionState(Auction auction) {

        persistAuctionState(
                auction,
                bidHistoryDAO.countByAuctionId(
                        auction.getId()
                )
        );
    }

    private void persistAuctionState(
            Auction auction,
            int bidCount
    ) {

        auctionStateDAO.upsert(
                auction,
                bidCount
        );

        auctionRecordDAO.updateState(auction);
    }

    private void processAutoBidChain(
            Auction auction,
            long now
    ) {

        int safetyCounter = 0;

        while (safetyCounter < 100) {

            safetyCounter++;

            AutoBid nextAutoBid =
                    findNextAutoBidder(auction);

            if (nextAutoBid == null) {
                return;
            }

            double nextAmount = Math.min(
                    nextAutoBid.getMaxBid(),
                    auction.getCurrentPrice()
                            + nextAutoBid.getIncrement()
            );

            if (nextAmount
                    <= auction.getCurrentPrice()) {

                return;
            }

            String previousBidder =
                    auction.getHighestBidder();

            auction.setCurrentPrice(nextAmount);

            auction.setHighestBidder(
                    nextAutoBid.getBidderUsername()
            );

            applyAntiSniping(auction, now);

            bidHistoryDAO.save(
                    auction.getId(),
                    nextAutoBid.getBidderUsername(),
                    nextAmount,
                    now
            );

            auction.addBidRecord(
                    new BidRecord(
                            nextAutoBid.getBidderUsername(),
                            nextAmount,
                            now
                    )
            );

            persistAuctionState(auction);

            if (previousBidder != null
                    && previousBidder.equals(
                    auction.getHighestBidder()
            )) {

                return;
            }
        }

        throw new RuntimeException(
                "AutoBid exceeded safety limit"
        );
    }

    private AutoBid findNextAutoBidder(
            Auction auction
    ) {

        AutoBid chosen = null;

        double chosenTarget = 0;

        for (AutoBid autoBid
                : autoBidDAO.findActiveByAuction(
                Long.parseLong(auction.getId())
        )) {

            if (!autoBid.isActive()) {
                continue;
            }

            if (autoBid.getBidderUsername()
                    .equals(
                            auction.getHighestBidder()
                    )) {

                continue;
            }

            if (autoBid.getMaxBid()
                    <= auction.getCurrentPrice()) {

                continue;
            }

            double targetBid = Math.min(
                    autoBid.getMaxBid(),
                    auction.getCurrentPrice()
                            + autoBid.getIncrement()
            );

            if (targetBid
                    <= auction.getCurrentPrice()) {

                continue;
            }

            if (chosen == null
                    || targetBid > chosenTarget
                    || (
                    targetBid == chosenTarget
                            && autoBid.getMaxBid()
                            > chosen.getMaxBid()
            )) {

                chosen = autoBid;
                chosenTarget = targetBid;
            }
        }

        return chosen;
    }

    private void applyAntiSniping(
            Auction auction,
            long now
    ) {

        long remaining =
                auction.getEndTime() - now;

        long oldEndTime =
                auction.getEndTime();

        if (remaining > 0
                && remaining < ANTI_SNIPE_THRESHOLD_MS) {

            auction.extendEndTime(ANTI_SNIPE_EXTENSION_MS);

            System.out.println(
                    "[ANTI-SNIPING] Auction "
                            + auction.getId()
                            + " extended from "
                            + oldEndTime
                            + " to "
                            + auction.getEndTime()
            );
        }
    }

    public String getAuctionDetail(String auctionId) {
        JsonObject response = new JsonObject();
        response.addProperty("command", "AUCTION_DETAIL_RESULT");
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            response.addProperty("status", "FAILED");
            response.addProperty("message", "Auction not found");
            return response.toString();
        }

        syncAuctionFromDatabase(auction);

        String bidder = auction.getHighestBidder() == null ? "NONE" : auction.getHighestBidder();
        response.addProperty("status", "SUCCESS");
        response.addProperty("id", auction.getId());
        response.addProperty("seller", auction.getSellerUsername());
        response.addProperty("itemName", auction.getItemName());
        response.addProperty("startPrice", (long) auction.getStartPrice());
        response.addProperty("currentPrice", (long) auction.getCurrentPrice());
        response.addProperty("highestBidder", bidder);
        response.addProperty("auctionStatus", auction.getStatus().name());

        // Thời gian
        response.addProperty("startDate", String.valueOf(auction.getStartDate()));
        response.addProperty("startTime", String.valueOf(auction.getStartClockTime()));
        response.addProperty("duration", auction.getDurationMinutes());
        response.addProperty("bidCount", bidHistoryDAO.countByAuctionId(auctionId));

        return response.toString();
    }

    public String getProductInfo(String auctionId) {

        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        syncAuctionFromDatabase(auction);

        String bidder =
                auction.getHighestBidder() == null
                        ? "NONE"
                        : auction.getHighestBidder();

        return "PRODUCT_INFO"
                + "|id=" + auction.getId()
                + "|seller=" + auction.getSellerUsername()
                + "|itemName=" + auction.getItemName()
                + "|startPrice=" + (long) auction.getStartPrice()
                + "|currentPrice=" + (long) auction.getCurrentPrice()
                + "|highestBidder=" + bidder
                + "|status=" + auction.getStatus()
                + "|bidCount="
                + bidHistoryDAO.countByAuctionId(
                auctionId
        );
    }

    public String getBidHistory(String auctionId) {

        if (!auctions.containsKey(auctionId)) {
            return "ERROR|Auction not found";
        }

        StringBuilder sb = new StringBuilder(
                "BID_HISTORY|auctionId="
                        + auctionId
                        + "|entries="
        );

        boolean first = true;

        for (BidRecord record
                : bidHistoryDAO.findByAuctionId(auctionId)) {

            if (!first) {
                sb.append(";");
            }

            sb.append(record.getBidderUsername())
                    .append(",")
                    .append((long) record.getAmount())
                    .append(",")
                    .append(record.getTimestamp());

            first = false;
        }

        return sb.toString();
    }

    private String extractField(String message, String key) {

        for (String part : message.split("\\|")) {
            if (part.startsWith(key + "=")) {
                return part.substring((key + "=").length());
            }
        }

        return "";
    }
}