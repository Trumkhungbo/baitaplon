package com.bidding.server.core;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.server.repository.AuctionRecordDAO;
import com.bidding.server.repository.AuctionStateDAO;
import com.bidding.server.repository.BidHistoryDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Xử lý duyệt, đóng phiên, cập nhật trạng thái, tìm người thắng và tự đóng auction hết hạn.
 */
class AuctionLifecycleService {

    private final Map<String, Auction> auctions;
    private final BidHistoryDAO bidHistoryDAO;
    private final AuctionStateDAO auctionStateDAO;
    private final AuctionRecordDAO auctionRecordDAO;
    private final AuctionValidationService validationService = new AuctionValidationService();

    AuctionLifecycleService(
            Map<String, Auction> auctions,
            BidHistoryDAO bidHistoryDAO,
            AuctionStateDAO auctionStateDAO,
            AuctionRecordDAO auctionRecordDAO
    ) {
        this.auctions = auctions;
        this.bidHistoryDAO = bidHistoryDAO;
        this.auctionStateDAO = auctionStateDAO;
        this.auctionRecordDAO = auctionRecordDAO;
    }

    /**
     * Đóng thủ công một auction đang hoạt động.
     */
    String closeAuction(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        synchronized (auction) {
            AuctionStateDAO.AuctionStateSnapshot state = syncAuctionFromDatabase(auction);

            if (!validationService.isActiveAuction(auction)) {
                return "ERROR|Auction is not open";
            }

            return finishAuction(
                    auction,
                    resolveBidCount(auctionId, state),
                    "CLOSE_AUCTION_SUCCESS"
            );
        }
    }

    /**
     * Cập nhật trạng thái auction và lưu xuống DB.
     */
    String updateStatus(String auctionId, AuctionStatus newStatus) {
        Auction auction = auctions.get(auctionId);

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

        return "UPDATE_STATUS_RESULT"
                + "|status=SUCCESS"
                + "|auctionId=" + auctionId
                + "|newStatus=" + newStatus.name()
                + "|message=Auction " + auctionId + " is now " + newStatus.name();
    }

    /**
     * Trả thông tin người thắng sau khi auction không còn hoạt động.
     */
    String getWinner(String auctionId) {
        Auction auction = auctions.get(auctionId);

        if (auction == null) {
            return "ERROR|Auction not found";
        }

        syncAuctionFromDatabase(auction);

        if (validationService.isActiveAuction(auction)) {
            return "ERROR|Auction is still running";
        }

        String winner = auction.getHighestBidder() == null ? "NONE" : auction.getHighestBidder();

        return "WINNER_INFO|auctionId="
                + auctionId
                + "|winner=" + winner
                + "|finalPrice="
                + (long) auction.getCurrentPrice()
                + "|status=" + auction.getStatus();
    }

    /**
     * Duyệt auction PENDING để hiển thị dưới trạng thái OPEN.
     */
    String approveAuction(String auctionId) {
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

    /**
     * Khởi chạy auction tới giờ, đóng auction hết hạn và trả thông báo thời gian thực để phát tới máy khách.
     */
    List<String> closeExpiredAuctions() {
        List<String> notifications = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Auction auction : auctions.values()) {
            applyTimeBasedStatus(auction, System.currentTimeMillis());

            synchronized (auction) {
                syncAuctionPreservingRuntimeEndTime(auction);

                if (applyTimeBasedStatus(auction, now)) {
                    notifications.add("AUCTION_STARTED|auctionId=" + auction.getId());
                }

                if (validationService.isActiveAuction(auction)
                        && now >= auction.getEndTime()) {
                    notifications.add(
                            finishAuction(
                                    auction,
                                    bidHistoryDAO.countByAuctionId(auction.getId()),
                                    "AUCTION_CLOSED"
                            )
                    );
                }
            }
        }

        return notifications;
    }

    /**
     * Chuyển auction từ OPEN sang RUNNING khi đã tới thời gian bắt đầu.
     */
    boolean applyTimeBasedStatus(Auction auction, long now) {
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
     * Đồng bộ auction trong bộ nhớ từ bản chụp trạng thái chạy đã lưu.
     */
    AuctionStateDAO.AuctionStateSnapshot syncAuctionFromDatabase(Auction auction) {
        AuctionStateDAO.AuctionStateSnapshot state = auctionStateDAO.findByAuctionId(auction.getId());
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

    /**
     * Lưu trạng thái auction hiện tại xuống cả bảng trạng thái chạy và bảng danh sách.
     */
    void persistAuctionState(Auction auction) {
        persistAuctionState(auction, bidHistoryDAO.countByAuctionId(auction.getId()));
    }

    void persistAuctionState(Auction auction, int bidCount) {
        auctionStateDAO.upsert(auction, bidCount);
        auctionRecordDAO.updateState(auction);
    }

    /**
     * Đánh dấu auction là FINISHED và tạo thông báo đóng phiên.
     */
    String finishAuction(Auction auction, int bidCount, String messageType) {
        if (auction.getStatus() == AuctionStatus.FINISHED) {
            return messageType
                    + "|auctionId="
                    + auction.getId();
        }

        auction.setStatus(AuctionStatus.FINISHED);
        persistAuctionState(auction, bidCount);

        String winner = auction.getHighestBidder() == null ? "NONE" : auction.getHighestBidder();

        return messageType
                + "|auctionId=" + auction.getId()
                + "|winner=" + winner
                + "|finalPrice="
                + (long) auction.getCurrentPrice();
    }

    /**
     * Dùng số lượt đặt giá từ cơ sở dữ liệu nếu có, nếu không thì đếm lại lịch sử đặt giá.
     */
    private int resolveBidCount(String auctionId, AuctionStateDAO.AuctionStateSnapshot state) {
        return state != null
                ? state.bidCount()
                : bidHistoryDAO.countByAuctionId(auctionId);
    }

    /**
     * Giữ thay đổi thời gian kết thúc do chống bid phút chót khi đồng bộ từ cơ sở dữ liệu.
     */
    private void syncAuctionPreservingRuntimeEndTime(Auction auction) {
        long localEndTime = auction.getEndTime();
        syncAuctionFromDatabase(auction);
        if (localEndTime != auction.getEndTime()) {
            auction.setEndTime(localEndTime);
            persistAuctionState(auction, 0);
        }
    }
}
