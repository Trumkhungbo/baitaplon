package com.bidding.server.network.command;

import com.bidding.server.core.AuthService;
import com.bidding.server.network.ClientHandler;

public class ResetPasswordCommand implements CommandHandler {

  private final AuthService authService;
  public ResetPasswordCommand(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public void handle(String[] parts, ClientHandler client) {
    // parts[0] = "RESET_PASSWORD", parts[1] = username, parts[2] = newPassword
    // Kiểm tra xem mảng có đủ 3 phần tử không
    if (parts.length >= 3) {
      String username = parts[1];
      String newPassword = parts[2];
      // Gọi AuthService để xử lý băm mật khẩu và cập nhật Database
      // Lưu ý: AuthService phải trả về chuỗi JSON kết quả (VD: {"status":"SUCCESS", ...})
      String resultJson = authService.resetPassword(username, newPassword);
      client.sendMessage(resultJson);
    } else {
      String errorJson = "{\"command\":\"RESET_PASSWORD_RESULT\", \"status\":\"FAILED\", \"message\":\"Thiếu tham số đổi mật khẩu!\"}";
      client.sendMessage(errorJson);
    }
  }
}