package com.bidding.server.core;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.common.model.item.Item;

/**
 * Gom các quy tắc kiểm tra dùng chung của auction vào một chỗ.
 */
class AuctionValidationService {

    private static final double MIN_BID_INCREMENT = 1000;
    private static final double MIN_BID_INCREMENT_RATE = 0.005;

    /**
     * Kiểm tra các trường bắt buộc trước khi tạo auction mới.
     */
    String validateAuctionInput(String sellerUsername, Item item) {
        if (item == null) {
            return "ERROR|Item is null";
        }

        if (sellerUsername == null || sellerUsername.isBlank()) {
            return "ERROR|Seller username required";
        }

        if (item.getStartingPrice() <= 0) {
            return "ERROR|Invalid start price";
        }

        return null;
    }

    /**
     * Kiểm tra user hiện tại có được thanh toán auction này không.
     */
    String validatePaymentState(String auctionId, String username, Auction auction) {
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

        return null;
    }

    /**
     * Trả true nếu auction vẫn còn nhận thay đổi vòng đời hoặc đặt giá.
     */
    boolean isActiveAuction(Auction auction) {
        return auction.getStatus() == AuctionStatus.OPEN
                || auction.getStatus() == AuctionStatus.RUNNING
                || auction.getStatus() == AuctionStatus.PENDING;
    }

    /**
     * Tính bước giá tối thiểu được phép từ giá hiện tại.
     */
    double calculateMinIncrement(double currentPrice) {
        return Math.max(
                MIN_BID_INCREMENT,
                Math.ceil(currentPrice * MIN_BID_INCREMENT_RATE)
        );
    }
}
