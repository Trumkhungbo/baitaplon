package com.bidding.server.exception;

public class AuctionNotFoundException extends RuntimeException {
    public AuctionNotFoundException(String message) {
        super(message);
    }
}
// Ngoại lệ này được ném ra khi không tìm thấy đấu giá với ID đã cho, ví dụ như khi client yêu cầu thông tin chi tiết của một đấu giá không tồn tại.