package com.bidding.server.core;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.server.repository.AuctionRecordDAO;
import com.bidding.server.repository.AuctionStateDAO;
import com.bidding.server.repository.BidHistoryDAO;
import com.bidding.server.repository.TransactionDAO;
import com.bidding.server.repository.UserDAO;

import java.util.Map;

/**
 * Xử lý thanh toán của người thắng, chuyển số dư, trạng thái PAID/CANCELED và phản hồi thanh toán.
 */
class PaymentService {

    private final Map<String, Auction> auctions;
    private final BidHistoryDAO bidHistoryDAO;
    private final AuctionStateDAO auctionStateDAO;
    private final AuctionRecordDAO auctionRecordDAO;
    private final UserDAO userDAO;
    private final AuctionResponseFormatter formatter;
    private final AuctionValidationService validationService;
    private final TransactionService transactionService;

    PaymentService(
            Map<String, Auction> auctions,
            BidHistoryDAO bidHistoryDAO,
            AuctionStateDAO auctionStateDAO,
            AuctionRecordDAO auctionRecordDAO,
            UserDAO userDAO,
            TransactionDAO transactionDAO
    ) {
        this.auctions = auctions;
        this.bidHistoryDAO = bidHistoryDAO;
        this.auctionStateDAO = auctionStateDAO;
        this.auctionRecordDAO = auctionRecordDAO;
        this.userDAO = userDAO;
        this.formatter = new AuctionResponseFormatter();
        this.validationService = new AuctionValidationService();
        this.transactionService = new TransactionService(userDAO, transactionDAO);
    }

    /**
     * Xử lý thanh toán cho người đặt giá thắng một auction đã kết thúc.
     */
    String payAuction(String auctionId, String username) {
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

            String validationError = validationService.validatePaymentState(auctionId, username, auction);
            if (validationError != null) {
                return validationError;
            }

            double amount = auction.getCurrentPrice();
            double buyerBalance = userDAO.getBalanceByUsername(username);
            if (buyerBalance < amount) {
                // Thanh toán thất bại sẽ hủy auction theo hành vi hiện tại.
                auction.setStatus(AuctionStatus.CANCELED);
                persistAuctionState(auction, bidHistoryDAO.countByAuctionId(auctionId));
                return "PAY_AUCTION_RESULT|status=FAILED|auctionId=" + auctionId + "|newStatus=CANCELED|message=Insufficient balance";
            }

            String sellerUsername = auction.getSellerUsername();
            double sellerBalance = userDAO.getBalanceByUsername(sellerUsername);
            // Thanh toán chuyển tiền từ người mua sang người bán và ghi lịch sử giao dịch.
            transactionService.transferAuctionPayment(auctionId, username, auction, amount, buyerBalance, sellerUsername, sellerBalance);

            auction.setStatus(AuctionStatus.PAID);
            persistAuctionState(auction, bidHistoryDAO.countByAuctionId(auctionId));

            return formatter.paymentSuccess(auctionId, amount, buyerBalance, sellerUsername);
        }
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
     * Lưu các thay đổi trạng thái auction liên quan đến thanh toán.
     */
    private void persistAuctionState(Auction auction, int bidCount) {
        auctionStateDAO.upsert(auction, bidCount);
        auctionRecordDAO.updateState(auction);
    }
}
