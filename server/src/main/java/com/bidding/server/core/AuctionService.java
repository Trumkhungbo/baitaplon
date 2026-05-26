
package com.bidding.server.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.common.model.AutoBid;
import com.bidding.server.database.DatabaseInitializer;
import com.bidding.server.exception.AuctionClosedException;
import com.bidding.server.exception.AuctionNotFoundException;
import com.bidding.server.exception.InvalidBidException;
import com.bidding.server.repository.AuctionRecordDAO;
import com.bidding.server.repository.AuctionStateDAO;
import com.bidding.server.repository.AutoBidDAO;
import com.bidding.server.repository.BidHistoryDAO;

public class AuctionService {

    private final Map<String, Auction> auctions = new ConcurrentHashMap<>();
    private final AtomicInteger nextAuctionId;
    private final BidHistoryDAO bidHistoryDAO;
    private final AuctionStateDAO auctionStateDAO;
    private final AuctionRecordDAO auctionRecordDAO;
    private final AutoBidDAO autoBidDAO;

    public AuctionService() {
        DatabaseInitializer.initialize();
        this.bidHistoryDAO = new BidHistoryDAO();
        this.auctionStateDAO = new AuctionStateDAO();
        this.auctionRecordDAO = new AuctionRecordDAO();
        this.autoBidDAO = new AutoBidDAO();
        seedData();
        loadPersistedRuntimeAuctions();
        this.nextAuctionId = new AtomicInteger((int) auctionRecordDAO.findMaxAuctionId() + 1);
    }

    private void seedData() {
        addInitialAuction("seller1", "iPhone 15", 15000000, AuctionStatus.OPEN);
        addInitialAuction("seller2", "MacBook Pro", 25000000, AuctionStatus.OPEN);
        addInitialAuction("seller3", "Oil Painting", 5000000, AuctionStatus.OPEN);
    }

    private void addInitialAuction(String sellerUsername, String itemName, double startPrice, AuctionStatus status) {
        String id = String.valueOf(auctions.size() + 1);
        Auction auction = new Auction(id, sellerUsername, itemName, startPrice, status);
        auctions.put(id, auction);
        if (!auctionRecordDAO.existsById(id)) {
            if (!auctionRecordDAO.existsById(id)) {
                auctionRecordDAO.save(id, sellerUsername, itemName, startPrice, auction.getStartTimeMillis(), auction.getDurationMinutes(), status);
            }

        }
        syncAuctionFromDatabase(auction);
        persistAuctionState(auction);
    }

    private void loadPersistedRuntimeAuctions() {
        for (AuctionStateDAO.AuctionStateSnapshot snapshot : auctionStateDAO.findAll()) {
            if (auctions.containsKey(snapshot.auctionId())) {
                continue;
            }

            Auction auction = new Auction(
                    snapshot.auctionId(),
                    snapshot.sellerUsername(),
                    snapshot.itemName(),
                    snapshot.startPrice(), (AuctionStatus) snapshot.status());
            auctions.put(snapshot.auctionId(), auction);
            syncAuctionFromDatabase(auction);
        }
    }

    public String closeAuction(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        synchronized (auction) {
            AuctionStateDAO.AuctionStateSnapshot state = syncAuctionFromDatabase(auction);

            if (!isActiveAuction(auction)) {
                return "ERROR|Auction is not open";
            }

            return finishAuction(auction, resolveBidCount(auctionId, state), "CLOSE_AUCTION_SUCCESS");
        }
    }

    public String getWinner(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        syncAuctionFromDatabase(auction);
        String winner = auction.getHighestBidder() == null ? "NONE" : auction.getHighestBidder();

        return "WINNER_INFO|auctionId=" + auctionId
                + "|winner=" + winner
                + "|finalPrice=" + (long) auction.getCurrentPrice()
                + "|status=" + auction.getStatus();
    }

    public String getAuctionList() {
        StringBuilder sb = new StringBuilder("AUCTION_LIST|");
        boolean first = true;

        for (Auction auction : auctions.values()) {
            syncAuctionFromDatabase(auction);

            if (!first) {
                sb.append(";");
            }

            sb.append(auction.getId())
                    .append(":")
                    .append(auction.getItemName())
                    .append(":")
                    .append((long) auction.getCurrentPrice())
                    .append(":")
                    .append(auction.getStatus());

            first = false;
        }

        return sb.toString();
    }

    public Auction findAuctionById(String auctionId) {
        return auctions.get(auctionId);
    }

    public String getAuctionDetail(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        AuctionStateDAO.AuctionStateSnapshot state = syncAuctionFromDatabase(auction);
        String bidder = auction.getHighestBidder() == null ? "NONE" : auction.getHighestBidder();

        return "AUCTION_DETAIL|id=" + auction.getId()
                + "|seller=" + auction.getSellerUsername()
                + "|itemName=" + auction.getItemName()
                + "|startPrice=" + (long) auction.getStartPrice()
                + "|currentPrice=" + (long) auction.getCurrentPrice()
                + "|highestBidder=" + bidder
                + "|status=" + auction.getStatus()
                + "|endTime=" + auction.getEndTime()
                + "|bidCount=" + resolveBidCount(auctionId, state);
    }

    public String getProductInfo(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        syncAuctionFromDatabase(auction);
        return "PRODUCT_INFO|id=" + auction.getId()
                + "|itemName=" + auction.getItemName()
                + "|seller=" + auction.getSellerUsername()
                + "|startPrice=" + (long) auction.getStartPrice()
                + "|currentPrice=" + (long) auction.getCurrentPrice()
                + "|status=" + auction.getStatus()
                + "|endTime=" + auction.getEndTime();
    }

    public String getBidHistory(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        StringBuilder sb = new StringBuilder("BID_HISTORY|auctionId=")
                .append(auctionId)
                .append("|entries=");
        boolean first = true;

        for (BidRecord bidRecord : bidHistoryDAO.findByAuctionId(auctionId)) {
            if (!first) {
                sb.append(";");
            }

            sb.append(bidRecord.getBidderUsername())
                    .append(",")
                    .append((long) bidRecord.getAmount())
                    .append(",")
                    .append(bidRecord.getTimestamp());
            first = false;
        }

        return sb.toString();
    }

    public String addAuction(String sellerUsername, String itemName, double startPrice) {
        if (sellerUsername == null || sellerUsername.trim().isEmpty()) {
            return "ERROR|Seller username is required";
        }

        if (itemName == null || itemName.trim().isEmpty()) {
            return "ERROR|Item name is required";
        }

        if (startPrice <= 0) {
            return "ERROR|Start price must be greater than 0";
        }

        String id = String.valueOf(nextAuctionId.getAndIncrement());
        Auction auction = new Auction(id, sellerUsername, itemName, startPrice, AuctionStatus.OPEN);
        auctionRecordDAO.save(id, sellerUsername, itemName, startPrice, auction.getEndTime(), AuctionStatus.OPEN);
        auctions.put(id, auction);
        persistAuctionState(auction, 0);

        return "ADD_AUCTION_SUCCESS|id=" + id
                + "|seller=" + sellerUsername
                + "|itemName=" + itemName
                + "|startPrice=" + (long) startPrice;
    }

    public String setAutoBid(String auctionId, String username, double maxBid, double increment) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        synchronized (auction) {
            syncAuctionFromDatabase(auction);

            if (auction.getStatus() == AuctionStatus.FINISHED
                    || auction.getStatus() == AuctionStatus.PAID
                    || auction.getStatus() == AuctionStatus.CANCELED) {
                return "ERROR|Auction is not available";
            }

            if (maxBid <= auction.getCurrentPrice()) {
                return "ERROR|Max bid must be greater than current price";
            }

            AutoBid autoBid = new AutoBid(Long.parseLong(auctionId), username, maxBid, increment);
            autoBidDAO.upsert(autoBid);
            if (auction.getHighestBidder() != null && !username.equals(auction.getHighestBidder())) {
                processAutoBidChain(auction, System.currentTimeMillis());
            }

            return "AUTO_BID_SET|auctionId=" + auctionId
                    + "|user=" + username
                    + "|maxBid=" + (long) maxBid
                    + "|increment=" + (long) increment;
        }
    }

    public List<String> closeExpiredAuctions() {
        List<String> notifications = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Auction auction : auctions.values()) {
            synchronized (auction) {
                if (isActiveAuction(auction) && now >= auction.getEndTime()) {
                    notifications.add(finishAuction(auction, bidHistoryDAO.countByAuctionId(auction.getId()), "AUCTION_CLOSED"));
                }
            }
        }

        return notifications;
    }

    public String placeBid(String auctionId, String username, double amount) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            throw new AuctionNotFoundException("Auction not found");
        }

        synchronized (auction) {
            // Sync từ DB trước khi validate — đảm bảo không dùng dữ liệu RAM stale
            // (quan trọng khi nhiều client bid đồng thời hoặc nhiều server instance)
            syncAuctionFromDatabase(auction);

            long now = System.currentTimeMillis();

            if (isActiveAuction(auction) && now >= auction.getEndTime()) {
                finishAuction(auction, bidHistoryDAO.countByAuctionId(auctionId), "AUCTION_CLOSED");
                throw new AuctionClosedException("Auction is not available");
            }

            if (auction.getStatus() == AuctionStatus.FINISHED
                    || auction.getStatus() == AuctionStatus.PAID
                    || auction.getStatus() == AuctionStatus.CANCELED) {
                throw new AuctionClosedException("Auction is not available");
            }

            if (auction.getStatus() == AuctionStatus.OPEN) {
                auction.setStatus(AuctionStatus.RUNNING);
            }

            if (amount <= auction.getCurrentPrice()) {
                throw new InvalidBidException(
                        "Bid amount must be greater than current price ("
                                + (long) auction.getCurrentPrice() + ")"
                );
            }

            // Lưu snapshot SAU KHI sync — không lưu trước vì sync có thể đổi status/price
            AuctionStatus previousStatus = (AuctionStatus) auction.getStatus();
            double previousPrice = auction.getCurrentPrice();
            String previousHighestBidder = auction.getHighestBidder();
            long previousEndTime = auction.getEndTime();

            auction.setCurrentPrice(amount);
            auction.setHighestBidder(username);

            long remaining = auction.getEndTime() - now;
            long oldEndTime = auction.getEndTime();

            if (remaining > 0 && remaining < 30000) {
                auction.extendEndTime(60000);
                System.out.println("[ANTI-SNIPING] Auction " + auctionId
                        + " extended from " + oldEndTime
                        + " to " + auction.getEndTime());
            }

            try {
                bidHistoryDAO.save(auctionId, username, amount, now);
                persistAuctionState(auction);
                processAutoBidChain(auction, now);
            } catch (RuntimeException e) {
                auction.setStatus(previousStatus);
                auction.setCurrentPrice(previousPrice);
                auction.setHighestBidder(previousHighestBidder);
                auction.setEndTime(previousEndTime);
                throw e;
            }

            auction.addBidRecord(new BidRecord(username, amount, now));

            return "BID_SUCCESS|auctionId=" + auctionId
                    + "|user=" + username
                    + "|amount=" + (long) amount;
        }
    }

    private boolean isActiveAuction(Auction auction) {
        return auction.getStatus() == AuctionStatus.OPEN
                || auction.getStatus() == AuctionStatus.RUNNING;
    }

    private String finishAuction(Auction auction, int bidCount, String messageType) {
        auction.setStatus(AuctionStatus.FINISHED);
        persistAuctionState(auction, bidCount);

        String winner = auction.getHighestBidder() == null ? "NONE" : auction.getHighestBidder();

        return messageType + "|auctionId=" + auction.getId()
                + "|winner=" + winner
                + "|finalPrice=" + (long) auction.getCurrentPrice();
    }

    private AuctionStateDAO.AuctionStateSnapshot syncAuctionFromDatabase(Auction auction) {
        AuctionStateDAO.AuctionStateSnapshot state = auctionStateDAO.findByAuctionId(auction.getId());
        if (state == null) {
            return null;
        }

        auction.setCurrentPrice(state.currentPrice());
        auction.setStatus((AuctionStatus) state.status());
        auction.setHighestBidder(state.highestBidder());
        auction.setEndTime(state.endTime());
        return state;
    }

    private int resolveBidCount(String auctionId, AuctionStateDAO.AuctionStateSnapshot state) {
        return state != null ? state.bidCount() : bidHistoryDAO.countByAuctionId(auctionId);
    }
    private void persistAuctionState(Auction auction) {
        persistAuctionState(auction, bidHistoryDAO.countByAuctionId(auction.getId()));
    }

    private void persistAuctionState(Auction auction, int bidCount) {
        auctionStateDAO.upsert(auction, bidCount);
        auctionRecordDAO.updateState(auction);
    }

    private void processAutoBidChain(Auction auction, long now) {
        while (true) {
            AutoBid nextAutoBid = findNextAutoBidder(auction);
            if (nextAutoBid == null) {
                return;
            }

            double nextAmount = Math.min(
                    nextAutoBid.getMaxBid(),
                    auction.getCurrentPrice() + nextAutoBid.getIncrement()
            );

            if (nextAmount <= auction.getCurrentPrice()) {
                return;
            }

            auction.setCurrentPrice(nextAmount);
            auction.setHighestBidder(nextAutoBid.getBidderUsername());
            applyAntiSniping(auction, now);
            bidHistoryDAO.save(auction.getId(), nextAutoBid.getBidderUsername(), nextAmount, now);
            auction.addBidRecord(new BidRecord(nextAutoBid.getBidderUsername(), nextAmount, now));
            persistAuctionState(auction);
        }
    }

    private AutoBid findNextAutoBidder(Auction auction) {
        AutoBid chosen = null;
        double chosenTarget = 0;

        for (AutoBid autoBid : autoBidDAO.findActiveByAuction(Long.parseLong(auction.getId()))) {
            if (!autoBid.isActive()) {
                continue;
            }
            if (autoBid.getBidderUsername().equals(auction.getHighestBidder())) {
                continue;
            }
            if (autoBid.getMaxBid() <= auction.getCurrentPrice()) {
                continue;
            }

            double targetBid = Math.min(autoBid.getMaxBid(), auction.getCurrentPrice() + autoBid.getIncrement());
            if (targetBid <= auction.getCurrentPrice()) {
                continue;
            }

            if (chosen == null
                    || targetBid > chosenTarget
                    || (targetBid == chosenTarget && autoBid.getMaxBid() > chosen.getMaxBid())) {
                chosen = autoBid;
                chosenTarget = targetBid;
            }
        }

        return chosen;
    }

    private void applyAntiSniping(Auction auction, long now) {
        long remaining = auction.getEndTime() - now;
        long oldEndTime = auction.getEndTime();

        if (remaining > 0 && remaining < 30000) {
            auction.extendEndTime(60000);
            System.out.println("[ANTI-SNIPING] Auction " + auction.getId()
                    + " extended from " + oldEndTime
                    + " to " + auction.getEndTime());
        }
    }
}