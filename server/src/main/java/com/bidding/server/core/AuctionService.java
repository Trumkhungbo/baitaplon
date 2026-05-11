package com.bidding.server.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.bidding.server.exception.AuctionClosedException;
import com.bidding.server.exception.AuctionNotFoundException;
import com.bidding.server.exception.InvalidBidException;
// Dịch vụ quản lý các hoạt động liên quan đến đấu giá
public class AuctionService {

    private final Map<String, Auction> auctions = new ConcurrentHashMap<>();
    private final AtomicInteger nextAuctionId;
    // Khởi tạo dịch vụ đấu giá với một số dữ liệu mẫu
    public AuctionService() {
        this.nextAuctionId = new AtomicInteger(1);
        seedData();
    }
    // Hàm khởi tạo một số đấu giá mẫu để có dữ liệu ban đầu khi server chạy
    private void seedData() {
        addInitialAuction("seller1", "iPhone 15", 15000000, AuctionStatus.OPEN);
        addInitialAuction("seller2", "MacBook Pro", 25000000, AuctionStatus.OPEN);
        addInitialAuction("seller3", "Oil Painting", 5000000, AuctionStatus.OPEN);
    }
    // Hàm hỗ trợ thêm đấu giá ban đầu vào hệ thống, chỉ dùng trong quá trình khởi tạo dữ liệu mẫu
    private void addInitialAuction(String sellerUsername, String itemName, double startPrice, AuctionStatus status) {
        String id = String.valueOf(nextAuctionId.getAndIncrement());
        auctions.put(id, new Auction(id, sellerUsername, itemName, startPrice, status));
    }

    // Đóng đấu giá, trả về thông báo để gửi cho client
    public String closeAuction(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        synchronized (auction) {
            if (!isActiveAuction(auction)) {
                return "ERROR|Auction is not open";
            }

            return finishAuction(auction, "CLOSE_AUCTION_SUCCESS");
        }
    }
    // Lấy thông tin người thắng cuộc của một đấu giá, trả về thông báo để gửi cho client
    public String getWinner(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        String winner = auction.getHighestBidder() == null ? "NONE" : auction.getHighestBidder();

        return "WINNER_INFO|auctionId=" + auctionId
                + "|winner=" + winner
                + "|finalPrice=" + (long) auction.getCurrentPrice()
                + "|status=" + auction.getStatus();
    }
    // Lấy danh sách đấu giá, trả về thông báo để gửi cho client
    public String getAuctionList() {
        StringBuilder sb = new StringBuilder("AUCTION_LIST|");
        boolean first = true;

        for (Auction auction : auctions.values()) {
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
    // Tìm đấu giá theo ID, trả về đối tượng Auction hoặc null nếu không tìm thấy
    public Auction findAuctionById(String auctionId) {
        return auctions.get(auctionId);
    }
    // Lấy thông tin chi tiết của một đấu giá, trả về thông báo để gửi cho client
    public String getAuctionDetail(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        String bidder = auction.getHighestBidder() == null ? "NONE" : auction.getHighestBidder();

        return "AUCTION_DETAIL|id=" + auction.getId()
                + "|seller=" + auction.getSellerUsername()
                + "|itemName=" + auction.getItemName()
                + "|startPrice=" + (long) auction.getStartPrice()
                + "|currentPrice=" + (long) auction.getCurrentPrice()
                + "|highestBidder=" + bidder
                + "|status=" + auction.getStatus()
                + "|endTime=" + auction.getEndTime();
    }
    //thêm auction mới, trả về thông báo để gửi cho client
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
        auctions.put(id, auction);

        return "ADD_AUCTION_SUCCESS|id=" + id
                + "|seller=" + sellerUsername
                + "|itemName=" + itemName
                + "|startPrice=" + (long) startPrice;
    }
    //đóng auction khi hết thời gian, trả về list thông báo để gửi cho client
    public List<String> closeExpiredAuctions() {
        List<String> notifications = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Auction auction : auctions.values()) {
            synchronized (auction) {
                if (isActiveAuction(auction)
                        && now >= auction.getEndTime()) {
                    notifications.add(finishAuction(auction, "AUCTION_CLOSED"));
                }
            }
        }

        return notifications;
    }
    // Đặt giá thầu cho một đấu giá, trả về thông báo để gửi cho client hoặc ném ngoại lệ nếu có lỗi
    public String placeBid(String auctionId, String username, double amount) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            throw new AuctionNotFoundException("Auction not found");
        }

        synchronized (auction) {
            long now = System.currentTimeMillis();

            if (isActiveAuction(auction) && now >= auction.getEndTime()) {
                finishAuction(auction, "AUCTION_CLOSED");
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

            return "BID_SUCCESS|auctionId=" + auctionId
                    + "|user=" + username
                    + "|amount=" + (long) amount;
        }
    }
    // Kiểm tra xem đấu giá có đang ở trạng thái mở hoặc đang chạy hay không
    private boolean isActiveAuction(Auction auction) {
        return auction.getStatus() == AuctionStatus.OPEN
                || auction.getStatus() == AuctionStatus.RUNNING;
    }
    // Hoàn tất đấu giá, cập nhật trạng thái và trả về thông báo để gửi cho client
    private String finishAuction(Auction auction, String messageType) {
        auction.setStatus(AuctionStatus.FINISHED);

        String winner = auction.getHighestBidder() == null ? "NONE" : auction.getHighestBidder();

        return messageType + "|auctionId=" + auction.getId()
                + "|winner=" + winner
                + "|finalPrice=" + (long) auction.getCurrentPrice();
    }
}
