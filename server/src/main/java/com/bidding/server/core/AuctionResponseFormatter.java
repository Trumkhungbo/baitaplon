package com.bidding.server.core;

import com.bidding.common.model.item.Item;

import java.util.Calendar;

/**
 * Tạo chuỗi phản hồi socket và làm sạch dữ liệu trước khi gửi cho máy khách.
 */
class AuctionResponseFormatter {

    /**
     * Định dạng phản hồi trả về sau khi tạo auction thành công.
     */
    String createAuctionSuccess(String auctionId, Item savedItem) {
        return "CREATE_AUCTION_SUCCESS"
                + "|auctionId=" + auctionId
                + "|itemId=" + savedItem.getId()
                + "|itemType=" + savedItem.getItemType();
    }

    /**
     * Định dạng phản hồi trả về sau khi người thắng thanh toán auction.
     */
    String paymentSuccess(String auctionId, double amount, double buyerBalance, String sellerUsername) {
        return "PAY_AUCTION_RESULT|status=SUCCESS"
                + "|auctionId=" + auctionId
                + "|newStatus=PAID"
                + "|paidAmount=" + (long) amount
                + "|buyerBalance=" + (long) (buyerBalance - amount)
                + "|seller=" + sanitizeMessageValue(sellerUsername)
                + "|message=Payment completed";
    }

    /**
     * Loại bỏ ký tự phân tách giao thức khỏi một trường phản hồi.
     */
    String sanitizeMessageValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", " ").replace("\r", " ").replace("\n", " ").trim();
    }

    /**
     * Loại bỏ ký tự phân tách danh sách khỏi giá trị trong các dòng AUCTION_LIST.
     */
    String sanitizeListValue(String value) {
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

    /**
     * Đổi epoch milliseconds sang yyyy-MM-dd để máy khách hiển thị.
     */
    String formatEpochDate(long epochMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(epochMillis);
        return String.format("%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Đổi epoch milliseconds sang HH:mm:ss để máy khách hiển thị.
     */
    String formatEpochTime(long epochMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(epochMillis);
        return String.format("%02d:%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));
    }
}
