package com.bidding.common.payload;

public class ResponseMsg<T> {
    private int status;       // Ví dụ: 200 (Thành công), 400 (Lỗi)
    private String message;   // Thông báo: "Thành công", "Sai mật khẩu"...
    private T data;           // Dữ liệu thực tế trả về (có thể là LoginResponse, hoặc danh sách Item...)

    public ResponseMsg() {
    }

    public ResponseMsg(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}

// Đây là lớp ResponseMsg dùng để chuẩn hóa phản hồi từ server về client.
