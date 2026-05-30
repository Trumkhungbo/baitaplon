package com.bidding.server.core;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.common.model.AutoBid;
import com.bidding.server.repository.AuctionRecordDAO;
import com.bidding.server.repository.AuctionStateDAO;
import com.bidding.server.repository.AutoBidDAO;
import com.bidding.server.repository.BidHistoryDAO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Xử lý cấu hình auto-bid và đặt giá thay giữa các auto-bid đang hoạt động.
 */
class AutoBidService {

    private static final long ANTI_SNIPE_THRESHOLD_MS = 30_000L;
    private static final long ANTI_SNIPE_EXTENSION_MS = 60_000L;

    private final Map<String, Auction> auctions;
    private final BidHistoryDAO bidHistoryDAO;
    private final AuctionStateDAO auctionStateDAO;
    private final AuctionRecordDAO auctionRecordDAO;
    private final AutoBidDAO autoBidDAO;
    private final AuctionValidationService validationService = new AuctionValidationService();
    private final AuctionResponseFormatter formatter = new AuctionResponseFormatter();

    AutoBidService(
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
        this.autoBidDAO = autoBidDAO;
    }

    /**
     * Bật hoặc cập nhật auto-bid cho người dùng trên auction đang chạy.
     */
    String setAutoBid(String auctionId, String username, double maxBid, double increment) {
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

            double minIncrement = validationService.calculateMinIncrement(auction.getCurrentPrice());

            if (increment < minIncrement) {
                return "ERROR|Increment too small";
            }

            double minimumValidBid = auction.getCurrentPrice() + minIncrement;

            if (maxBid < minimumValidBid) {
                return "ERROR|Max bid must be at least " + (long) minimumValidBid;
            }

            AutoBid autoBid = new AutoBid(
                    Long.parseLong(auctionId),
                    username,
                    maxBid,
                    increment
            );

            autoBidDAO.upsert(autoBid);

            repriceAuctionFromAutoBids(auction, System.currentTimeMillis());

            return "AUTO_BID_SET"
                    + "|auctionId=" + auctionId
                    + "|user=" + username
                    + "|maxBid=" + (long) maxBid
                    + "|increment=" + (long) increment
                    + "|active=true";
        }
    }

    /**
     * Trả cấu hình auto-bid hiện tại của một người dùng trong auction.
     */
    String getAutoBid(String auctionId, String username) {
        if (auctionId == null || auctionId.isBlank()) {
            return "ERROR|Auction id required";
        }

        AutoBid autoBid = autoBidDAO.findOne(Long.parseLong(auctionId), username);
        if (autoBid == null) {
            return "AUTO_BID_STATUS"
                    + "|auctionId=" + auctionId
                    + "|user=" + formatter.sanitizeMessageValue(username)
                    + "|active=false";
        }

        return "AUTO_BID_STATUS"
                + "|auctionId=" + auctionId
                + "|user=" + formatter.sanitizeMessageValue(username)
                + "|maxBid=" + (long) autoBid.getMaxBid()
                + "|increment=" + (long) autoBid.getIncrement()
                + "|active=" + autoBid.isActive();
    }

    /**
     * Tắt cấu hình auto-bid của một người dùng trong auction.
     */
    String disableAutoBid(String auctionId, String username) {
        if (auctionId == null || auctionId.isBlank()) {
            return "ERROR|Auction id required";
        }

        autoBidDAO.disable(Long.parseLong(auctionId), username);
        return "AUTO_BID_DISABLED"
                + "|auctionId=" + auctionId
                + "|user=" + formatter.sanitizeMessageValue(username)
                + "|active=false";
    }

    /**
     * Phản ứng với giá đặt thủ công bằng auto-bid cao nhất nếu nó thắng được mức giá vừa đặt.
     */
    void applyProxyAutoBidAfterManualBid(Auction auction, String manualBidder, double manualAmount, long now) {
        AutoBid winner = findHighestAutoBid(auction, manualBidder);

        if (winner == null || winner.getMaxBid() <= manualAmount) {
            return;
        }

        double minIncrement = validationService.calculateMinIncrement(manualAmount);
        double proxyPrice = Math.min(winner.getMaxBid(), manualAmount + minIncrement);

        applyProxyBidResult(auction, winner.getBidderUsername(), proxyPrice, now);
    }

    /**
     * Tính lại giá hiện tại sau khi auto-bid được thêm hoặc cập nhật.
     */
    private void repriceAuctionFromAutoBids(Auction auction, long now) {
        List<AutoBid> autoBids = findActiveAutoBidsSorted(auction);

        if (autoBids.isEmpty()) {
            return;
        }

        AutoBid winner = autoBids.get(0);

        double proxyPrice;
        if (autoBids.size() == 1) {
            double minIncrement = validationService.calculateMinIncrement(auction.getCurrentPrice());
            proxyPrice = Math.min(winner.getMaxBid(), auction.getCurrentPrice() + minIncrement);
        } else {
            AutoBid runnerUp = autoBids.get(1);
            double minIncrement = validationService.calculateMinIncrement(runnerUp.getMaxBid());
            proxyPrice = Math.min(winner.getMaxBid(), runnerUp.getMaxBid() + minIncrement);
            proxyPrice = Math.max(auction.getCurrentPrice(), proxyPrice);
        }

        applyProxyBidResult(auction, winner.getBidderUsername(), proxyPrice, now);
    }

    /**
     * Tìm người dùng auto-bid mạnh nhất, có thể loại trừ người vừa đặt giá thủ công.
     */
    private AutoBid findHighestAutoBid(Auction auction, String excludedBidder) {
        List<AutoBid> autoBids = findActiveAutoBidsSorted(auction);

        for (AutoBid autoBid : autoBids) {
            if (!autoBid.getBidderUsername().equals(excludedBidder)) {
                return autoBid;
            }
        }

        return null;
    }

    /**
     * Nạp các auto-bid còn hoạt động và vẫn có thể vượt giá hiện tại.
     */
    private List<AutoBid> findActiveAutoBidsSorted(Auction auction) {
        List<AutoBid> autoBids = new ArrayList<>(
                autoBidDAO.findActiveByAuction(Long.parseLong(auction.getId()))
        );

        autoBids.removeIf(autoBid ->
                !autoBid.isActive()
                        || autoBid.getBidderUsername() == null
                        || autoBid.getMaxBid() <= auction.getCurrentPrice()
        );

        autoBids.sort(Comparator.comparingDouble(AutoBid::getMaxBid).reversed());

        return autoBids;
    }

    /**
     * Áp dụng kết quả đặt giá thay và ghi lịch sử nếu trạng thái hiển thị thay đổi.
     */
    private void applyProxyBidResult(Auction auction, String bidder, double amount, long now) {
        if (amount < auction.getCurrentPrice()) {
            return;
        }

        boolean stateChanged = amount != auction.getCurrentPrice()
                || !bidder.equals(auction.getHighestBidder());

        auction.setCurrentPrice(amount);
        auction.setHighestBidder(bidder);

        // AutoBid dùng cùng luật chống bid phút chót như đặt giá thủ công.
        applyAntiSniping(auction, now);

        if (stateChanged) {
            bidHistoryDAO.save(auction.getId(), bidder, amount, now);
            auction.addBidRecord(new BidRecord(bidder, amount, now));
        }

        persistAuctionState(auction);
    }

    /**
     * Gia hạn thời gian kết thúc khi đặt giá thay xuất hiện sát cuối phiên.
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
     * Đồng bộ auction trong bộ nhớ từ bản chụp trạng thái chạy đã lưu.
     */
    private AuctionStateDAO.AuctionStateSnapshot syncAuctionFromDatabase(Auction auction) {
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
    private void persistAuctionState(Auction auction) {
        persistAuctionState(auction, bidHistoryDAO.countByAuctionId(auction.getId()));
    }

    private void persistAuctionState(Auction auction, int bidCount) {
        auctionStateDAO.upsert(auction, bidCount);
        auctionRecordDAO.updateState(auction);
    }
}
