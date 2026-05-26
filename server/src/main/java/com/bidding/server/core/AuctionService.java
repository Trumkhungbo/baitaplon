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
import com.bidding.server.repository.SellerAuctionDAO;
import com.bidding.server.repository.UserDAO;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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
    private final SellerAuctionDAO sellerAuctionDAO;
    private final UserDAO userDAO;

    public AuctionService() {

        DatabaseInitializer.initialize();

        this.bidHistoryDAO = new BidHistoryDAO();
        this.auctionStateDAO = new AuctionStateDAO();
        this.auctionRecordDAO = new AuctionRecordDAO();
        this.autoBidDAO = new AutoBidDAO();
        this.itemDAO = new ItemDAO();
        this.sellerAuctionDAO = new SellerAuctionDAO();
        this.userDAO = new UserDAO();

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

    /**
     * Khôi phục trạng thái runtime của tất cả phiên đấu giá từ DB khi server khởi động lại.
     * Dữ liệu trong bảng auction_state chứa snapshot mới nhất (giá, bidder, status, thời gian).
     * Nếu bảng trống (lần đầu chạy), seedData() sẽ tạo dữ liệu mẫu.
     */
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

            auction.setItemId(
                    auctionRecordDAO.findItemIdByAuctionId(snapshot.auctionId())
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
        if (startTime < System.currentTimeMillis()) {
            return "ERROR|Start time cannot be in the past";
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

    public String getSellerAuctionList(String sellerUsername) {
        if (sellerUsername == null || sellerUsername.isBlank()) {
            return "ERROR|Seller username required";
        }

        StringBuilder sb = new StringBuilder("MY_AUCTIONS|");
        boolean first = true;

        for (Auction auction : auctions.values().stream()
                .filter(a -> sellerUsername.equals(a.getSellerUsername()))
                .sorted((left, right) -> Integer.compare(
                        Integer.parseInt(right.getId()),
                        Integer.parseInt(left.getId())))
                .toList()) {

            syncAuctionFromDatabase(auction);
            applyTimeBasedStatus(auction, System.currentTimeMillis());

            Item item = itemDAO.findById(auction.getItemId());
            if (item == null) {
                continue;
            }

            if (!first) {
                sb.append(";");
            }
            first = false;

            appendAccountAuctionRow(sb, auction, item, "Seller", auction.getStartDate(), auction.getStartClockTime(), "", false);
        }

        return sb.toString();
    }

    public String getAccountAuctionList(String username) {
        if (username == null || username.isBlank()) {
            return "ERROR|Username required";
        }

        StringBuilder sb = new StringBuilder("ACCOUNT_AUCTIONS|");
        boolean first = true;

        Map<String, Long> bidderAuctionTimes = bidHistoryDAO.findLatestBidTimesByBidder(username);
        for (Map.Entry<String, Long> entry : bidderAuctionTimes.entrySet()) {
            Auction auction = auctions.get(entry.getKey());
            if (auction == null || username.equals(auction.getSellerUsername())) {
                continue;
            }
            Item item = prepareAuctionForAccountList(auction);
            if (item == null) {
                continue;
            }
            if (!first) {
                sb.append(";");
            }
            first = false;
            String result = resolveBidderResultText(auction, username);
            boolean canPay = "FINISHED".equalsIgnoreCase(auction.getStatus().name())
                    && username.equalsIgnoreCase(auction.getHighestBidder());
            appendAccountAuctionRow(sb, auction, item, "Bidder", formatEpochDate(entry.getValue()), formatEpochTime(entry.getValue()), result, canPay);
        }

        return sb.toString();
    }

    private Item prepareAuctionForAccountList(Auction auction) {
        syncAuctionFromDatabase(auction);
        applyTimeBasedStatus(auction, System.currentTimeMillis());
        return itemDAO.findById(auction.getItemId());
    }

    private void appendAccountAuctionRow(StringBuilder sb, Auction auction, Item item, String role, String displayDate, String displayTime, String result, boolean canPay) {
        String itemType = item.getItemType() == null ? "" : item.getItemType().name();
        String imageUrl = sanitizeListValue(item.getImageUrl());
        String description = sanitizeListValue(item.getDescription());
        String information1 = sanitizeListValue(itemDAO.resolveInformation1(item));
        String information2 = sanitizeListValue(itemDAO.resolveInformation2(item));
        int bidCount = bidHistoryDAO.countByAuctionId(auction.getId());

        sb.append(auction.getId()).append(":")
                .append(auction.getItemId()).append(":")
                .append(sanitizeListValue(auction.getItemName())).append(":")
                .append(itemType).append(":")
                .append((long) auction.getStartPrice()).append(":")
                .append((long) auction.getCurrentPrice()).append(":")
                .append(auction.getStatus().name()).append(":")
                .append(sanitizeListValue(displayDate)).append(":")
                .append(sanitizeListValue(displayTime)).append(":")
                .append(auction.getDurationMinutes()).append(":")
                .append(bidCount).append(":")
                .append(imageUrl).append(":")
                .append(description).append(":")
                .append(information1).append(":")
                .append(information2).append(":")
                .append(auction.getStartTimeMillis()).append(":")
                .append(auction.getEndTime()).append(":")
                .append(role).append(":")
                .append(sanitizeListValue(result)).append(":")
                .append(canPay);
    }

    private String resolveBidderResultText(Auction auction, String username) {
        if (auction == null || username == null || username.isBlank()) {
            return "";
        }

        String status = auction.getStatus() == null ? "" : auction.getStatus().name();
        if ("RUNNING".equalsIgnoreCase(status)) {
            int rank = calculateBidderRank(auction.getId(), username);
            return rank > 0 ? "Top " + rank : "";
        }

        if ("FINISHED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status)) {
            return username.equalsIgnoreCase(auction.getHighestBidder()) ? "winner" : "lost";
        }

        if ("CANCELED".equalsIgnoreCase(status)) {
            return "canceled";
        }

        return "";
    }

    private int calculateBidderRank(String auctionId, String username) {
        Map<String, BidRecord> ranking = buildBidderRanking(auctionId);
        int rank = 1;
        for (BidRecord record : ranking.values().stream()
                .sorted((left, right) -> {
                    int amountCompare = Double.compare(right.getAmount(), left.getAmount());
                    if (amountCompare != 0) {
                        return amountCompare;
                    }
                    return Long.compare(left.getTimestamp(), right.getTimestamp());
                })
                .toList()) {
            if (username.equalsIgnoreCase(record.getBidderUsername())) {
                return rank;
            }
            rank++;
        }
        return 0;
    }

    private Map<String, BidRecord> buildBidderRanking(String auctionId) {
        Map<String, BidRecord> bestBids = new HashMap<>();
        for (BidRecord record : bidHistoryDAO.findByAuctionId(auctionId)) {
            BidRecord current = bestBids.get(record.getBidderUsername());
            if (current == null
                    || record.getAmount() > current.getAmount()
                    || (record.getAmount() == current.getAmount() && record.getTimestamp() < current.getTimestamp())) {
                bestBids.put(record.getBidderUsername(), record);
            }
        }
        return bestBids;
    }

    public String updateSellerAuction(String sellerUsername, String auctionId, Item item, long startTime, long durationMinutes) {
        Auction current = auctions.get(auctionId);
        if (current == null) {
            return "ERROR|Auction not found";
        }

        if (!sellerUsername.equals(current.getSellerUsername())) {
            return "ERROR|You can only edit your own auction";
        }

        int bidCount = bidHistoryDAO.countByAuctionId(auctionId);
        if (bidCount > 0 || current.getHighestBidder() != null) {
            return "ERROR|Auction already has bids and cannot be edited";
        }

        if (current.getStatus() == AuctionStatus.RUNNING || current.getStatus() == AuctionStatus.FINISHED) {
            return "ERROR|Auction cannot be edited after it has started";
        }

        itemDAO.update(current.getItemId(), item, sellerUsername);

        Auction updated = new Auction(
                current.getId(),
                sellerUsername,
                item.getName(),
                item.getStartingPrice(),
                current.getStatus()
        );
        updated.setItemId(current.getItemId());
        updated.setStartTimeMillis(startTime);
        updated.setDurationMinutes((int) durationMinutes);
        updated.setCurrentPrice(item.getStartingPrice());
        updated.setHighestBidder(null);

        auctions.put(auctionId, updated);
        auctionRecordDAO.updateListing(
                auctionId,
                updated.getItemId(),
                sellerUsername,
                item.getStartingPrice(),
                updated.getStartTimeMillis(),
                updated.getEndTime(),
                updated.getDurationMinutes(),
                updated.getStatus()
        );
        persistAuctionState(updated, 0);

        return "UPDATE_AUCTION_SUCCESS|auctionId=" + auctionId;
    }

    public String deleteSellerAuction(String sellerUsername, String auctionId) {
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            return "ERROR|Auction not found";
        }

        if (!sellerUsername.equals(auction.getSellerUsername())) {
            return "ERROR|You can only delete your own auction";
        }

        int bidCount = bidHistoryDAO.countByAuctionId(auctionId);
        if (bidCount > 0 || auction.getHighestBidder() != null) {
            return "ERROR|Auction already has bids and cannot be deleted";
        }

        if (auction.getStatus() == AuctionStatus.RUNNING || auction.getStatus() == AuctionStatus.FINISHED) {
            return "ERROR|Auction cannot be deleted after it has started";
        }

        sellerAuctionDAO.deleteAutoBidByAuctionId(auctionId);
        sellerAuctionDAO.deleteBidHistoryByAuctionId(auctionId);
        auctionStateDAO.deleteByAuctionId(auctionId);
        auctionRecordDAO.deleteByAuctionId(auctionId);
        itemDAO.deleteById(auction.getItemId());
        auctions.remove(auctionId);

        return "DELETE_AUCTION_SUCCESS|auctionId=" + auctionId;
    }

    public String addAuction(
            String sellerUsername,
            String itemName,
            double startPrice
    ) {

        Item item = new Art(
                itemName,
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

        Auction auction = auctions.get(auctionId);

        // Chuẩn hóa chuỗi lỗi theo cấu trúc: TÊN_LỆNH|status=FAILED|message=Nội_dung
        if (auction == null) {
            return "UPDATE_STATUS_RESULT|status=FAILED|message=Auction not found";
        }

        if (newStatus == null) {
            return "UPDATE_STATUS_RESULT|status=FAILED|message=Invalid status";
        }

        synchronized (auction) {
            syncAuctionFromDatabase(auction);
            auction.setStatus(newStatus);
            persistAuctionState(auction);
        }

        // Chuẩn hóa chuỗi thành công theo cấu trúc cặp key=value rõ ràng
        return "UPDATE_STATUS_RESULT"
                + "|status=SUCCESS"
                + "|auctionId=" + auctionId
                + "|newStatus=" + newStatus.name()
                + "|message=Auction " + auctionId + " is now " + newStatus.name();
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
        // Khởi tạo chuỗi kết quả ban đầu
        StringBuilder sb = new StringBuilder("AUCTION_LIST|");
        boolean first = true;

        for (Auction auction : auctions.values()) {
            // ĐÃ XÓA dòng syncAuctionFromDatabase(auction) tại đây để bảo vệ hệ thống khỏi lỗi N+1 Query

            // Bộ lọc trạng thái (Đã bao gồm FINISHED)
            if (!includePending
                    && auction.getStatus() != AuctionStatus.OPEN
                    && auction.getStatus() != AuctionStatus.RUNNING
                    && auction.getStatus() != AuctionStatus.FINISHED) {
                continue;
            }

            if (!first) {
                sb.append(";");
            }

            // Kiểm tra an toàn để tránh nối chữ "null" nếu dữ liệu trống
            String id = auction.getId() == null ? "" : auction.getId();
            String itemName = sanitizeListValue(auction.getItemName() == null ? "Unknown Item" : auction.getItemName());
            String status = auction.getStatus() == null ? "UNKNOWN" : auction.getStatus().name();
            String imageUrl = "";
            String description = "";
            String information1 = "";
            String information2 = "";
            String itemType = "";
            String startDate = sanitizeListValue(auction.getStartDate());
            String startTime = sanitizeListValue(auction.getStartClockTime());
            String duration = String.valueOf(auction.getDurationMinutes());
            try {
                com.bidding.common.model.item.Item item = itemDAO.findById(auction.getItemId());
                if (item != null) {
                    if (item.getItemType() != null) {
                        itemType = sanitizeListValue(item.getItemType().name());
                    }
                    if (item.getImageUrl() != null) {
                        imageUrl = sanitizeListValue(item.getImageUrl());
                    }
                    if (item.getDescription() != null) {
                        description = sanitizeListValue(item.getDescription());
                    }
                    String resolvedInformation1 = itemDAO.resolveInformation1(item);
                    String resolvedInformation2 = itemDAO.resolveInformation2(item);
                    if (resolvedInformation1 != null) {
                        information1 = sanitizeListValue(resolvedInformation1);
                    }
                    if (resolvedInformation2 != null) {
                        information2 = sanitizeListValue(resolvedInformation2);
                    }
                }
            } catch (RuntimeException e) {
                System.err.println("[AUCTION_LIST] Cannot load image for auction " + id + ": " + e.getMessage());
            }

            // Tiến hành nối chuỗi theo cấu trúc id:itemName:currentPrice:status
            sb.append(id)
                    .append(":")
                    .append(itemName)
                    .append(":")
                    .append((long) auction.getCurrentPrice())
                    .append(":")
                    .append(status)
                    .append(":")
                    .append(imageUrl)
                    .append(":")
                    .append(description)
                    .append(":")
                    .append(information1)
                    .append(":")
                    .append(information2)
                    .append(":")
                    .append(startDate)
                    .append(":")
                    .append(startTime)
                    .append(":")
                    .append(duration)
                    .append(":")
                    .append(itemType)
                    .append(":")
                    .append(auction.getEndTime())
                    .append(":")
                    .append(System.currentTimeMillis());

            first = false;
        }

        return sb.toString();
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
            persistAuctionState(auction, bidHistoryDAO.countByAuctionId(auctionId));
        }

        return "APPROVE_AUCTION_SUCCESS|auctionId=" + auctionId;
    }

    public Auction findAuctionById(String auctionId) {

        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return null;
        }

        syncAuctionFromDatabase(auction);
        applyTimeBasedStatus(auction, System.currentTimeMillis());

        return auction;
    }

    /**
     * Xử lý đặt giá cho một phiên đấu giá.
     *
     * <p>Flow:
     * 1. Validate input (username, amount, auction tồn tại, seller không tự bid)
     * 2. Kiểm tra trạng thái phiên (phải đang OPEN hoặc RUNNING, chưa hết giờ)
     * 3. Cập nhật giá và bidder trên in-memory object
     * 4. Áp dụng anti-sniping (nếu bid gần cuối phiên, gia hạn thêm 60s)
     * 5. Persist vào DB (bid_history + auction_state)
     * 6. Nếu fail DB → rollback in-memory state về giá trị cũ
     * 7. Trigger auto-bid chain cho các bidder khác
     *
     * @throws AuctionNotFoundException nếu phiên không tồn tại
     * @throws AuctionClosedException nếu phiên đã đóng hoặc hết giờ
     * @throws InvalidBidException nếu giá không hợp lệ
     */
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
            applyTimeBasedStatus(auction, now);

            if (auction.getStatus() == AuctionStatus.OPEN
                    && now < auction.getStartTimeMillis()) {
                throw new AuctionClosedException(
                        "Auction has not started yet"
                );
            }

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

            return "BID_RESULT"
                    + "|status=SUCCESS"
                    + "|auctionId=" + auctionId
                    + "|user=" + username
                    + "|amount=" + (long) amount
                    + "|message=Bid placed successfully";
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
            applyTimeBasedStatus(auction, System.currentTimeMillis());

            if (increment <= 0) {
                return "ERROR|Increment invalid";
            }

            if (auction.getStatus() != AuctionStatus.RUNNING) {
                return "ERROR|Auto-bid is only available when auction is running";
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
                    + "|user=" + username
                    + "|maxBid=" + (long) maxBid
                    + "|increment=" + (long) increment
                    + "|active=true";
        }
    }

    public String getAutoBid(String auctionId, String username) {
        if (auctionId == null || auctionId.isBlank()) {
            return "ERROR|Auction id required";
        }

        AutoBid autoBid = autoBidDAO.findOne(Long.parseLong(auctionId), username);
        if (autoBid == null) {
            return "AUTO_BID_STATUS"
                    + "|auctionId=" + auctionId
                    + "|user=" + sanitizeMessageValue(username)
                    + "|active=false";
        }

        return "AUTO_BID_STATUS"
                + "|auctionId=" + auctionId
                + "|user=" + sanitizeMessageValue(username)
                + "|maxBid=" + (long) autoBid.getMaxBid()
                + "|increment=" + (long) autoBid.getIncrement()
                + "|active=" + autoBid.isActive();
    }

    public String disableAutoBid(String auctionId, String username) {
        if (auctionId == null || auctionId.isBlank()) {
            return "ERROR|Auction id required";
        }

        autoBidDAO.disable(Long.parseLong(auctionId), username);
        return "AUTO_BID_DISABLED"
                + "|auctionId=" + auctionId
                + "|user=" + sanitizeMessageValue(username)
                + "|active=false";
    }

    public String payAuction(String auctionId, String username) {
        if (auctionId == null || auctionId.isBlank()) {
            return "PAY_AUCTION_RESULT|status=FAILED|message=Auction id required";
        }
        if (username == null || username.isBlank()) {
            return "PAY_AUCTION_RESULT|status=FAILED|message=Username required";
        }

        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            return "PAY_AUCTION_RESULT|status=FAILED|message=Auction not found";
        }

        synchronized (auction) {
            syncAuctionFromDatabase(auction);

            if (auction.getStatus() == AuctionStatus.PAID) {
                return "PAY_AUCTION_RESULT|status=FAILED|auctionId=" + auctionId + "|message=Auction already paid";
            }

            if (auction.getStatus() == AuctionStatus.CANCELED) {
                return "PAY_AUCTION_RESULT|status=FAILED|auctionId=" + auctionId + "|message=Auction was canceled";
            }

            if (auction.getStatus() != AuctionStatus.FINISHED) {
                return "PAY_AUCTION_RESULT|status=FAILED|auctionId=" + auctionId + "|message=Auction is not ready for payment";
            }

            String winner = auction.getHighestBidder();
            if (winner == null || winner.isBlank() || !winner.equalsIgnoreCase(username)) {
                return "PAY_AUCTION_RESULT|status=FAILED|auctionId=" + auctionId + "|message=Only the winner can pay";
            }

            double amount = auction.getCurrentPrice();
            double buyerBalance = userDAO.getBalanceByUsername(username);
            if (buyerBalance < amount) {
                auction.setStatus(AuctionStatus.CANCELED);
                persistAuctionState(auction, bidHistoryDAO.countByAuctionId(auctionId));
                return "PAY_AUCTION_RESULT|status=FAILED|auctionId=" + auctionId + "|newStatus=CANCELED|message=Insufficient balance";
            }

            String sellerUsername = auction.getSellerUsername();
            double sellerBalance = userDAO.getBalanceByUsername(sellerUsername);
            userDAO.updateBalance(username, buyerBalance - amount);
            userDAO.updateBalance(sellerUsername, sellerBalance + amount);

            auction.setStatus(AuctionStatus.PAID);
            persistAuctionState(auction, bidHistoryDAO.countByAuctionId(auctionId));

            return "PAY_AUCTION_RESULT|status=SUCCESS"
                    + "|auctionId=" + auctionId
                    + "|newStatus=PAID"
                    + "|paidAmount=" + (long) amount
                    + "|buyerBalance=" + (long) (buyerBalance - amount)
                    + "|seller=" + sanitizeMessageValue(sellerUsername)
                    + "|message=Payment completed";
        }
    }

    /**
     * Quét tất cả phiên đấu giá và đóng những phiên đã hết giờ.
     * Được gọi định kỳ bởi AuctionServer (mỗi 5 giây) để tự động kết thúc phiên.
     *
     * <p>Logic quan trọng: endTime runtime có thể khác DB (do anti-sniping gia hạn),
     * nên phải lưu lại localEndTime trước sync, rồi restore nếu bị DB ghi đè.
     *
     * @return danh sách notification cần broadcast cho client (AUCTION_STARTED, AUCTION_CLOSED)
     */
    public List<String> closeExpiredAuctions() {

        List<String> notifications =
                new ArrayList<>();

        long now = System.currentTimeMillis();

        for (Auction auction : auctions.values()) {
            applyTimeBasedStatus(auction, System.currentTimeMillis());

            synchronized (auction) {

                // Lưu lại endTime runtime trước khi sync, vì syncAuctionFromDatabase() gọi
                // setStartTimeMillis() → tính lại endTime từ duration, có thể ghi đè giá trị
                // đã được anti-sniping hoặc hệ thống điều chỉnh.
                long localEndTime = auction.getEndTime();
                syncAuctionFromDatabase(auction);
                if (localEndTime != auction.getEndTime()) {
                    auction.setEndTime(localEndTime);
                    persistAuctionState(auction, 0);
                }

                if (applyTimeBasedStatus(auction, now)) {
                    notifications.add("AUCTION_STARTED|auctionId=" + auction.getId());
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

    /**
     * Đánh dấu phiên đấu giá là FINISHED và persist vào DB.
     * Nếu phiên đã FINISHED rồi thì trả về message ngắn (idempotent).
     *
     * @param auction   phiên cần đóng
     * @param bidCount  số lượt bid hiện tại (để lưu vào snapshot)
     * @param messageType loại message trả về ("AUCTION_CLOSED" hoặc "CLOSE_AUCTION_SUCCESS")
     * @return chuỗi notification dạng "AUCTION_CLOSED|auctionId=X|winner=Y|finalPrice=Z"
     */
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

    /**
     * Đồng bộ trạng thái in-memory của auction với DB.
     * Cần thiết vì nhiều instance AuctionService có thể cùng chạy (hoặc restart),
     * nên phải đọc lại giá trị mới nhất từ bảng auction_state.
     *
     * <p>Lưu ý: setStartTimeMillis() sẽ tính lại endTime = start + duration,
     * có thể ghi đè endTime đã được anti-sniping gia hạn. Caller cần bảo vệ endTime nếu cần.
     */
    private AuctionStateDAO.AuctionStateSnapshot
    syncAuctionFromDatabase(Auction auction) {

        AuctionStateDAO.AuctionStateSnapshot state =
                auctionStateDAO.findByAuctionId(
                        auction.getId()
                );

        if (state == null) {
            return null;
        }

        auction.setCurrentPrice(state.currentPrice());
        auction.setStatus(state.status());
        auction.setHighestBidder(state.highestBidder());
        auction.setStartTimeMillis(state.startTimeMillis());
        auction.setDurationMinutes(state.durationMinutes());
        if (state.endTimeMillis() > auction.getEndTime()) {
            auction.setEndTime(state.endTimeMillis());
        }

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

    /**
     * Xử lý chuỗi auto-bid: khi có bid mới, tìm auto-bidder tiếp theo và đặt giá tự động.
     * Lặp cho đến khi không còn auto-bidder nào đủ điều kiện hoặc cùng người thắng.
     *
     * <p>Ví dụ: User A set auto-bid max=20tr, User B set max=18tr.
     * Khi C bid 16tr → A auto-bid 16.5tr → B auto-bid 17tr → A auto-bid 17.5tr → B dừng (vượt max).
     *
     * <p>Safety counter = 100 vòng lặp tối đa để tránh infinite loop.
     */
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

    /**
     * Tìm auto-bidder phù hợp nhất cho lượt đặt giá tiếp theo.
     *
     * <p>Tiêu chí chọn (ưu tiên theo thứ tự):
     * 1. Không phải người đang thắng (tránh tự bid)
     * 2. maxBid > giá hiện tại (còn khả năng bid)
     * 3. targetBid = min(maxBid, currentPrice + increment) > currentPrice
     * 4. Ai có targetBid cao hơn thì ưu tiên. Nếu bằng nhau → chọn ai có maxBid lớn hơn.
     *
     * @return auto-bidder được chọn, hoặc null nếu không ai đủ điều kiện
     */
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

    /**
     * Chống snipe: nếu bid được đặt trong 30 giây cuối phiên, gia hạn thêm 60 giây.
     * Đảm bảo các bidder khác có thời gian phản ứng, tránh chiến thuật "chờ giây cuối mới bid".
     *
     * <p>Chỉ kích hoạt khi: remaining > 0 VÀ remaining <= 30s (ANTI_SNIPE_THRESHOLD_MS).
     * endTime mới sẽ được persist ngay vào DB.
     */
    private void applyAntiSniping(
            Auction auction,
            long now
    ) {

        long remaining =
                auction.getEndTime() - now;

        long oldEndTime =
                auction.getEndTime();

        if (remaining > 0
                && remaining <= ANTI_SNIPE_THRESHOLD_MS) {

            auction.extendEndTime(ANTI_SNIPE_EXTENSION_MS);

            System.out.println(
                    "[ANTI-SNIPING] Auction "
                            + auction.getId()
                            + " extended from "
                            + oldEndTime
                            + " to "
                            + auction.getEndTime());
            persistAuctionState(auction, 0);
        }
    }

    private boolean applyTimeBasedStatus(Auction auction, long now) {
        if (auction == null) {
            return false;
        }

        if (auction.getStatus() == AuctionStatus.OPEN
                && now >= auction.getStartTimeMillis()
                && now < auction.getEndTime()) {
            auction.setStatus(AuctionStatus.RUNNING);
            persistAuctionState(auction);
            return true;
        }

        return false;
    }
    public String getAuctionDetail(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        syncAuctionFromDatabase(auction);
        applyTimeBasedStatus(auction, System.currentTimeMillis());

        String bidder = auction.getHighestBidder() == null ? "NONE" : auction.getHighestBidder();

        String imageUrl = "";
        String information1 = "";
        String information2 = "";
        String itemType = "";

        try {
            com.bidding.common.model.item.Item item = itemDAO.findById(auction.getItemId());
            if (item != null) {
                if (item.getImageUrl() != null) imageUrl = item.getImageUrl();
                if (item.getItemType() != null) itemType = item.getItemType().name();
                String description = item.getDescription();

                String i1 = itemDAO.resolveInformation1(item);
                String i2 = itemDAO.resolveInformation2(item);
                if (i1 != null) information1 = i1;
                if (i2 != null) information2 = i2;
                if (description != null) {
                    information2 = information2 == null ? "" : information2;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String statusStr = auction.getStatus() == null ? "UNKNOWN" : auction.getStatus().name();

        return "AUCTION_DETAIL"
                + "|id=" + auction.getId()
                + "|seller=" + auction.getSellerUsername()
                + "|itemName=" + auction.getItemName()
                + "|startPrice=" + (long) auction.getStartPrice()
                + "|currentPrice=" + (long) auction.getCurrentPrice()
                + "|highestBidder=" + bidder
                + "|status=" + statusStr
                + "|startDate=" + auction.getStartDate()
                + "|startTime=" + auction.getStartClockTime()
                + "|duration=" + auction.getDurationMinutes()
                + "|bidCount=" + bidHistoryDAO.countByAuctionId(auctionId)
                + "|imageUrl=" + imageUrl
                + "|information1=" + information1
                + "|information2=" + information2
                + "|description=" + sanitizeMessageValue(getItemDescription(auction.getItemId()))
                + "|itemType=" + itemType
                + "|endTime=" + auction.getEndTime()
                + "|serverTime=" + System.currentTimeMillis();
    }

    private String getItemDescription(long itemId) {
        try {
            com.bidding.common.model.item.Item item = itemDAO.findById(itemId);
            return item == null ? "" : item.getDescription();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private String sanitizeMessageValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", " ").replace("\r", " ").replace("\n", " ").trim();
    }

    private String sanitizeListValue(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace(":", " ")
                .replace(";", " ")
                .replace("|", " ")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
    }

    private String formatEpochDate(long epochMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(epochMillis);
        return String.format("%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    private String formatEpochTime(long epochMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(epochMillis);
        return String.format("%02d:%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));
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
