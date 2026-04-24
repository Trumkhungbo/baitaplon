package com.bidding.server.model;
import java.time.LocalDateTime;
import java.util.UUID;

public abstract class Entity {
    // Thuộc tính private (Encapsulation)
    private String id;
    private LocalDateTime createdAt;

    public Entity() {
        // Tự động sinh ID duy nhất và lấy thời gian hiện tại khi khởi tạo
        this.id = UUID.randomUUID().toString(); 
        this.createdAt = LocalDateTime.now();
    }

    // Chỉ cung cấp Getter, không cung cấp Setter để bảo vệ dữ liệu gốc
    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}