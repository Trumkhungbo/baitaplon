package com.bidding.server.core;

import com.bidding.server.repository.TransactionDAO;
import com.bidding.server.repository.UserDAO;

/**
 * Xử lý cập nhật số dư và ghi lịch sử giao dịch khi thanh toán auction.
 */
class TransactionService {

    private final UserDAO userDAO;
    private final TransactionDAO transactionDAO;

    TransactionService(UserDAO userDAO, TransactionDAO transactionDAO) {
        this.userDAO = userDAO;
        this.transactionDAO = transactionDAO;
    }

    /**
     * Chuyển tiền từ người thắng sang người bán và ghi hai dòng giao dịch.
     */
    void transferAuctionPayment(
            String auctionId,
            String username,
            Auction auction,
            double amount,
            double buyerBalance,
            String sellerUsername,
            double sellerBalance
    ) {
        userDAO.updateBalance(username, buyerBalance - amount);
        userDAO.updateBalance(sellerUsername, sellerBalance + amount);
        long paidAt = System.currentTimeMillis();

        transactionDAO.save(
                username,
                "PAYMENT",
                amount,
                "Thanh toan cho phien dau gia " + auction.getItemName(),
                "thanh cong",
                auctionId,
                paidAt
        );
        transactionDAO.save(
                sellerUsername,
                "RECEIVE_PAYMENT",
                amount,
                "Nhan tien thanh toan tu phien dau gia " + auction.getItemName(),
                "thanh cong",
                auctionId,
                paidAt
        );
    }
}
