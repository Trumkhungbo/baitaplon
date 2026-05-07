package com.bidding.server.enums;

public enum AuctionStatus {
    OPEN,       // Vừa tạo, chưa đến giờ bắt đầu
    RUNNING,    // Đang diễn ra, cho phép đặt giá
    FINISHED,   // Đã kết thúc
    PAID,       // Người thắng đã thanh toán
    CANCELED    // Bị hủy
}