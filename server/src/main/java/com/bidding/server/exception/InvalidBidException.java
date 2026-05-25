package com.bidding.server.exception;

public class InvalidBidException extends RuntimeException {
    public InvalidBidException(String message) {
        super(message);
    }
}
// Ngoại lệ này được ném ra khi có lỗi liên quan đến việc đặt giá thầu, ví dụ như giá thầu thấp hơn giá hiện tại hoặc đấu giá đã đóng.