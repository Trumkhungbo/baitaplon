package com.bidding.server.core;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.common.model.item.Art;
import com.bidding.common.model.item.Item;
import com.bidding.server.database.DatabaseInitializer;
import com.bidding.server.repository.AuctionRecordDAO;
import com.bidding.server.repository.AuctionStateDAO;
import com.bidding.server.repository.AutoBidDAO;
import com.bidding.server.repository.BidHistoryDAO;
import com.bidding.server.repository.ItemDAO;
import com.bidding.server.repository.SellerAuctionDAO;
import com.bidding.server.repository.TransactionDAO;
import com.bidding.server.repository.UserDAO;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lớp trung gian chính cho các lệnh đấu giá mà máy chủ socket gọi vào.
 * Điều phối đặt giá, auto-bid, thanh toán, vòng đời, truy vấn và định dạng phản hồi qua các service nhỏ hơn.
 */
public class AuctionService {

    private static final long MILLIS_PER_MINUTE = 60_000L;
    private static final int DEFAULT_DURATION_MINUTES = 5;

    private final Map<String, Auction> auctions = new ConcurrentHashMap<>();
    private final AtomicInteger nextAuctionId;

    private final BidHistoryDAO bidHistoryDAO;
    private final AuctionStateDAO auctionStateDAO;
    private final AuctionRecordDAO auctionRecordDAO;
    private final AutoBidDAO autoBidDAO;
    private final ItemDAO itemDAO;
    private final SellerAuctionDAO sellerAuctionDAO;
    private final UserDAO userDAO;
    private final TransactionDAO transactionDAO;
    private final AuctionResponseFormatter responseFormatter = new AuctionResponseFormatter();
    private final AuctionValidationService validationService = new AuctionValidationService();

    /**
     * Khởi tạo DAO, nạp lại trạng thái chạy của auction từ cơ sở dữ liệu và tạo dữ liệu mẫu nếu chưa có dữ liệu.
     */
    public AuctionService() {
        DatabaseInitializer.initialize();

        this.bidHistoryDAO = new BidHistoryDAO();
        this.auctionStateDAO = new AuctionStateDAO();
        this.auctionRecordDAO = new AuctionRecordDAO();
        this.autoBidDAO = new AutoBidDAO();
        this.itemDAO = new ItemDAO();
        this.sellerAuctionDAO = new SellerAuctionDAO();
        this.userDAO = new UserDAO();
        this.transactionDAO = new TransactionDAO();

        loadPersistedRuntimeAuctions();
        if (auctions.isEmpty()) {
            seedData();
        }

        long maxId = auctionRecordDAO.findMaxAuctionId();
        this.nextAuctionId = new AtomicInteger(Math.toIntExact(maxId + 1));
    }

    private AuctionLifecycleService lifecycleService() {
        return new AuctionLifecycleService(auctions, bidHistoryDAO, auctionStateDAO, auctionRecordDAO);
    }

    private PaymentService paymentService() {
        return new PaymentService(auctions, bidHistoryDAO, auctionStateDAO, auctionRecordDAO, userDAO, transactionDAO);
    }

    private AutoBidService autoBidService() {
        return new AutoBidService(auctions, bidHistoryDAO, auctionStateDAO, auctionRecordDAO, autoBidDAO);
    }

    private BidService bidService() {
        return new BidService(auctions, bidHistoryDAO, auctionStateDAO, auctionRecordDAO, autoBidDAO);
    }

    private AuctionQueryService queryService() {
        return new AuctionQueryService(auctions, bidHistoryDAO, auctionStateDAO, auctionRecordDAO, itemDAO);
    }

    private AuctionValidationService validationService() {
        return validationService == null ? new AuctionValidationService() : validationService;
    }

    private AuctionResponseFormatter responseFormatter() {
        return responseFormatter == null ? new AuctionResponseFormatter() : responseFormatter;
    }

    /**
     * Khôi phục trạng thái chạy của auction từ cơ sở dữ liệu khi máy chủ khởi động.
     */
    private void loadPersistedRuntimeAuctions() {
        for (AuctionStateDAO.AuctionStateSnapshot snapshot : auctionStateDAO.findAll()) {
            Auction auction = new Auction(
                    snapshot.auctionId(),
                    snapshot.sellerUsername(),
                    snapshot.itemName(),
                    snapshot.startPrice(),
                    snapshot.status()
            );
            auction.setCurrentPrice(snapshot.currentPrice());
            auction.setHighestBidder(snapshot.highestBidder());
            auction.setStartTimeMillis(snapshot.startTimeMillis());
            auction.setEndTime(snapshot.endTimeMillis());
            auction.setItemId(auctionRecordDAO.findItemIdByAuctionId(snapshot.auctionId()));
            auctions.put(snapshot.auctionId(), auction);
        }
    }

    private void seedData() {
        addInitialAuction("seller1", "iPhone 15", 15_000_000, AuctionStatus.OPEN);
        addInitialAuction("seller2", "MacBook Pro", 25_000_000, AuctionStatus.OPEN);
        addInitialAuction("seller3", "Oil Painting", 5_000_000, AuctionStatus.OPEN);
    }

    private void addInitialAuction(String sellerUsername, String itemName, double startPrice, AuctionStatus status) {
        String id = String.valueOf(auctionRecordDAO.findMaxAuctionId() + 1);
        if (auctionRecordDAO.existsById(id)) {
            return;
        }

        Auction auction = new Auction(id, sellerUsername, itemName, startPrice, status);
        auction.setDurationMinutes(DEFAULT_DURATION_MINUTES);

        Item item = new Art(itemName, startPrice, "", "Unknown", Calendar.getInstance().get(Calendar.YEAR));
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
     * Tạo auction mở ngay và trả về đúng định dạng phản hồi client-server hiện có.
     */
    public String createAuction(String sellerUsername, Item item) {
        String validationError = validationService().validateAuctionInput(sellerUsername, item);
        if (validationError != null) {
            return validationError;
        }

        Item savedItem = itemDAO.save(item, sellerUsername);
        String auctionId = String.valueOf(nextAuctionId.getAndIncrement());

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

        saveAuctionRecord(auction, savedItem, sellerUsername, AuctionStatus.OPEN);
        persistAuctionState(auction, 0);

        return responseFormatter().createAuctionSuccess(auctionId, savedItem);
    }

    /**
     * Tạo auction đang chờ với thời gian bắt đầu đã hẹn.
     */
    public String createPendingAuction(String sellerUsername, Item item, long startTime, long durationMinutes) {
        String validationError = validationService().validateAuctionInput(sellerUsername, item);
        if (validationError != null) {
            return validationError;
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
        long endTime = startTime + durationMinutes * MILLIS_PER_MINUTE;
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

    /**
     * Lấy danh sách auction của người bán theo định dạng giao thức MY_AUCTIONS.
     */
    public String getSellerAuctionList(String sellerUsername) {
        return queryService().getSellerAuctionList(sellerUsername);
    }

    /**
     * Lấy danh sách auction liên quan đến người đặt giá theo định dạng giao thức ACCOUNT_AUCTIONS.
     */
    public String getAccountAuctionList(String username) {
        return queryService().getAccountAuctionList(username);
    }

    /**
     * Cập nhật auction của người bán khi auction chưa bắt đầu và chưa có lượt đặt giá.
     */
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

    /**
     * Xóa auction của người bán khi auction chưa bắt đầu và chưa có lượt đặt giá.
     */
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

    /**
     * Tạo auction dạng Art đơn giản cho lệnh cũ ADD_AUCTION.
     */
    public String addAuction(String sellerUsername, String itemName, double startPrice) {
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

    /**
     * Đóng thủ công một auction đang hoạt động.
     */
    public String closeAuction(String auctionId) {
        return lifecycleService().closeAuction(auctionId);
    }

    /**
     * Cập nhật trạng thái chạy đã lưu của auction.
     */
    public String updateStatus(String auctionId, AuctionStatus newStatus) {
        return lifecycleService().updateStatus(auctionId, newStatus);
    }

    /**
     * Trả thông tin người thắng sau khi auction không còn hoạt động.
     */
    public String getWinner(String auctionId) {
        return lifecycleService().getWinner(auctionId);
    }

    /**
     * Trả danh sách auction hiển thị ở màn lobby.
     */
    public String getAuctionList() {
        return getAuctionList(false);
    }

    /**
     * Trả danh sách auction cho lobby, có thể kèm auction PENDING cho màn quản trị/người bán.
     */
    public String getAuctionList(boolean includePending) {
        return queryService().getAuctionList(includePending);
    }

    /**
     * Duyệt auction PENDING và chuyển sang OPEN.
     */
    public String approveAuction(String auctionId) {
        return lifecycleService().approveAuction(auctionId);
    }

    /**
     * Tìm auction và đồng bộ trạng thái chạy trước khi trả về.
     */
    public Auction findAuctionById(String auctionId) {
        return queryService().findAuctionById(auctionId);
    }

    /**
     * Đặt giá thông qua BidService và giữ nguyên định dạng phản hồi socket.
     */
    public String placeBid(String auctionId, String username, double amount) {
        return bidService().placeBid(auctionId, username, amount);
    }

    /**
     * Bật hoặc cập nhật auto-bid cho người dùng trên auction đang chạy.
     */
    public String setAutoBid(String auctionId, String username, double maxBid, double increment) {
        return autoBidService().setAutoBid(auctionId, username, maxBid, increment);
    }

    /**
     * Trả cấu hình auto-bid hiện tại của người dùng trong auction.
     */
    public String getAutoBid(String auctionId, String username) {
        return autoBidService().getAutoBid(auctionId, username);
    }

    /**
     * Tắt cấu hình auto-bid của người dùng trong auction.
     */
    public String disableAutoBid(String auctionId, String username) {
        return autoBidService().disableAutoBid(auctionId, username);
    }

    /**
     * Xử lý thanh toán của người thắng và chuyển tiền từ người mua sang người bán.
     */
    public String payAuction(String auctionId, String username) {
        return paymentService().payAuction(auctionId, username);
    }

    /**
     * Đóng các auction hết hạn và trả thông báo thời gian thực để phát tới máy khách.
     */
    public List<String> closeExpiredAuctions() {
        return lifecycleService().closeExpiredAuctions();
    }

    /**
     * Tính bước giá tối thiểu hợp lệ từ giá hiện tại.
     */
    public double calculateMinIncrement(double currentPrice) {
        return validationService().calculateMinIncrement(currentPrice);
    }

    /**
     * Trả thông tin chi tiết auction cho màn chi tiết sản phẩm.
     */
    public String getAuctionDetail(String auctionId) {
        return queryService().getAuctionDetail(auctionId);
    }

    /**
     * Trả thông tin sản phẩm rút gọn cho yêu cầu chi tiết sản phẩm cũ.
     */
    public String getProductInfo(String auctionId) {
        return queryService().getProductInfo(auctionId);
    }

    /**
     * Trả lịch sử đặt giá theo định dạng giao thức BID_HISTORY.
     */
    public String getBidHistory(String auctionId) {
        return queryService().getBidHistory(auctionId);
    }

    private void saveAuctionRecord(Auction auction, Item savedItem, String sellerUsername, AuctionStatus status) {
        auctionRecordDAO.save(
                auction.getId(),
                savedItem.getId(),
                sellerUsername,
                savedItem.getStartingPrice(),
                auction.getStartTimeMillis(),
                auction.getEndTime(),
                auction.getDurationMinutes(),
                status
        );
    }

    /**
     * Lưu bản chụp auction trong bộ nhớ xuống bảng trạng thái chạy và bảng danh sách.
     */
    private void persistAuctionState(Auction auction) {
        persistAuctionState(auction, bidHistoryDAO.countByAuctionId(auction.getId()));
    }

    private void persistAuctionState(Auction auction, int bidCount) {
        auctionStateDAO.upsert(auction, bidCount);
        auctionRecordDAO.updateState(auction);
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
