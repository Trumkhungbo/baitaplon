package com.bidding.server.network.command;

import com.bidding.server.core.AuthService;
import com.bidding.server.network.ClientHandler;

public class ForgotPasswordCommand implements CommandHandler {

  private final AuthService authService;

  // Nhận AuthService từ CommandDispatcher truyền vào
  public ForgotPasswordCommand(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public void handle(String[] parts, ClientHandler client) {
    // Kiểm tra xem mảng có đủ 4 phần tử không (Lệnh, Username, Phone, PersonalID)
    if (parts.length >= 4) {
      String username = parts[1];
      String phone = parts[2];
      String personalID = parts[3];

      // Gọi AuthService để tra cứu Database
      String resultJson = authService.forgotPassword(username, phone, personalID);

      // Trả kết quả về cho Client
      client.sendMessage(resultJson);
    } else {
      // Trả lỗi nếu Client gửi thiếu dữ liệu
      client.sendMessage("{\"command\":\"ERROR\", \"message\":\"Thiếu tham số khôi phục mật khẩu\"}");
    }
  }
}