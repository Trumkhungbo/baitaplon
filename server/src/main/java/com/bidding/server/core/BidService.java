package com.bidding.server.core;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.server.exception.AuctionClosedException;
import com.bidding.server.exception.AuctionNotFoundException;
import com.bidding.server.exception.InvalidBidException;
import com.bidding.server.repository.AuctionRecordDAO;
import com.bidding.server.repository.AuctionStateDAO;
import com.bidding.server.repository.AutoBidDAO;
import com.bidding.server.repository.BidHistoryDAO;

import java.util.Map;

/**
 * Xử lý đặt giá thủ công, kiểm tra giá đặt, lưu cơ sở dữ liệu và kích hoạt auto-bid sau đó.
 */
class BidService {

    private static final long ANTI_SNIPE_THRESHOLD_MS = 30_000L;
    private static final long ANTI_SNIPE_EXTENSION_MS = 60_000L;

    private final Map<String, Auction> auctions;
    private final BidHistoryDAO bidHistoryDAO;
    private final AuctionStateDAO auctionStateDAO;
    private final AuctionRecordDAO auctionRecordDAO;
    private final AutoBidService autoBidService;
    private final AuctionValidationService validationService = new AuctionValidationService();

    BidService(
            Map<String, Auction> auctions,
            BidHistoryDAO bidHistoryDAO,
            AuctionStateDAO auctionStateDAO,
            AuctionRecordDAO auctionRecordDAO,
            AutoBidDAO autoBidDAO
    ) {
        this.auctions = auctions;
        this.bidHistoryDAO = bidHistoryDAO;
        this.auctionStateDAO = auctionStateDAO;
        this.auctionRecordDAO = auctionRecordDAO;
        this.autoBidService = new AutoBidService(auctions, bidHistoryDAO, auctionStateDAO, auctionRecordDAO, autoBidDAO);
    }

    /**
     * Đặt giá thủ công và trả về nguyên định dạng giao thức BID_RESULT.
     */
    String placeBid(String auctionId, String username, double amount) {
        if (username == null || username.isBlank()) {
            throw new InvalidBidException("Username is required");
        }

        if (amount <= 0) {
            throw new InvalidBidException("Invalid bid amount");
        }

        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            throw new AuctionNotFoundException("Auction not found");
        }

        if (auction.getSellerUsername().equals(username)) {
            throw new InvalidBidException("Seller cannot bid on their own auction");
        }

        synchronized (auction) {
            long now = System.currentTimeMillis();
            applyTimeBasedStatus(auction, now);

            if (auction.getStatus() == AuctionStatus.OPEN
                    && now < auction.getStartTimeMillis()) {
                throw new AuctionClosedException("Auction has not started yet");
            }

            if (validationService.isActiveAuction(auction)
                    && now >= auction.getEndTime()) {
                finishAuction(auction, bidHistoryDAO.countByAuctionId(auctionId), "AUCTION_CLOSED");
                throw new AuctionClosedException("Auction is not available");
            }

            if (auction.getStatus() == AuctionStatus.PENDING
                    || auction.getStatus() == AuctionStatus.FINISHED
                    || auction.getStatus() == AuctionStatus.PAID
                    || auction.getStatus() == AuctionStatus.CANCELED) {
                throw new AuctionClosedException("Auction is not available");
            }

            double minIncrement = validationService.calculateMinIncrement(auction.getCurrentPrice());
            double minimumValidBid = auction.getCurrentPrice() + minIncrement;

            if (amount < minimumValidBid) {
                throw new InvalidBidException("Minimum valid bid is " + (long) minimumValidBid);
            }

            AuctionStatus previousStatus = auction.getStatus();
            double previousPrice = auction.getCurrentPrice();
            String previousHighestBidder = auction.getHighestBidder();
            long previousEndTime = auction.getEndTime();

            auction.setCurrentPrice(amount);
            auction.setHighestBidder(username);

            // Chống bid phút chót bằng cách gia hạn ngắn khi có giá đặt sát thời điểm kết thúc.
            applyAntiSniping(auction, now);

            try {
                bidHistoryDAO.save(auctionId, username, amount, now);
                persistAuctionState(auction);
            } catch (RuntimeException e) {
                auction.setStatus(previousStatus);
                auction.setCurrentPrice(previousPrice);
                auction.setHighestBidder(previousHighestBidder);
                auction.setEndTime(previousEndTime);
                throw e;
            }

            auction.addBidRecord(new BidRecord(username, amount, now));

            // AutoBid có thể tự đặt giá cao hơn ngay nếu người khác đặt giá tối đa lớn hơn.
            autoBidService.applyProxyAutoBidAfterManualBid(auction, username, amount, now);

            return "BID_RESULT"
                    + "|status=SUCCESS"
                    + "|auctionId=" + auctionId
                    + "|user=" + username
                    + "|amount=" + (long) amount
                    + "|message=Bid placed successfully";
        }
    }

    /**
     * Đánh dấu auction đã kết thúc và tạo thông báo đóng phiên.
     */
    private String finishAuction(Auction auction, int bidCount, String messageType) {
        if (auction.getStatus() == AuctionStatus.FINISHED) {
            return messageType + "|auctionId=" + auction.getId();
        }

        auction.setStatus(AuctionStatus.FINISHED);
        persistAuctionState(auction, bidCount);

        String winner = auction.getHighestBidder() == null ? "NONE" : auction.getHighestBidder();

        return messageType
                + "|auctionId=" + auction.getId()
                + "|winner=" + winner
                + "|finalPrice=" + (long) auction.getCurrentPrice();
    }

    /**
     * Gia hạn thời gian kết thúc khi có giá đặt trong những giây cuối.
     */
    private void applyAntiSniping(Auction auction, long now) {
        long remaining = auction.getEndTime() - now;
        long oldEndTime = auction.getEndTime();

        if (remaining > 0 && remaining <= ANTI_SNIPE_THRESHOLD_MS) {
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

    /**
     * Chuyển auction từ OPEN sang RUNNING khi đã tới thời gian bắt đầu.
     */
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

    /**
     * Lưu trạng thái auction hiện tại xuống cả bảng trạng thái chạy và bảng danh sách.
     */
    private void persistAuctionState(Auction auction) {
        persistAuctionState(auction, bidHistoryDAO.countByAuctionId(auction.getId()));
    }

    private void persistAuctionState(Auction auction, int bidCount) {
        auctionStateDAO.upsert(auction, bidCount);
        auctionRecordDAO.updateState(auction);
    }
}
