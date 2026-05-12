package com.bidding.server.core;

import com.bidding.common.enums.UserRole;
import com.bidding.common.model.user.User;
import com.bidding.common.model.user.Bidder;
import com.bidding.server.repository.UserDAO;
import com.google.gson.JsonObject;
import java.util.List;

public class AuthService {

  private final UserDAO userDAO = new UserDAO();

  public AuthService() {
  }

  // Hàm đăng nhập
  public String login(String username, String password) {
    if (username == null || password == null) {
      return "LOGIN_FAILED|Back to refill username or password";
    }

    // Dòng này gọi UserDAO tìm user trong Database theo tên
    User foundUser = userDAO.findByUsername(username);
    // Kiểm tra: Nếu thấy người dùng VÀ mật khẩu trong DB khớp với mật khẩu nhập vào
    if (foundUser != null && foundUser.getPasswordHash().equals(password)) {
      return "LOGIN_SUCCESS|USER|Welcome " + username;
    }
    return "LOGIN_FAILED|Invalid username or password";
  }

  // Hàm đăng kí
  public String register(String username, List<String> information) {
    if (username == null || username.trim().isEmpty()) return "REGISTER_FAILED|Empty username";
    if (userDAO.existsByUsername(username)) return "REGISTER_FAILED|Unable to add Username";

    // Tạo đối tượng Bidder
    Bidder newUser = new Bidder();
    newUser.setUsername(username);
    newUser.setPasswordHash(information.get(0)); // Password là phần tử 0
    newUser.setEmail(information.get(2));        // Email là phần tử 2
    newUser.setPhone(information.get(1));        // Phone là phần tử 1
    newUser.setPersonalId(information.get(3));   // ID là phần tử 3

    newUser.setRole(UserRole.BIDDER);
    newUser.setBalance(0.0); // Mặc định tài khoản mới có 0 đồng
    newUser.setCreatedAt(System.currentTimeMillis());

    // Ra lệnh cho DAO lưu xuống file SQLite
    userDAO.save(newUser);
    return "REGISTER_SUCCESS|Register Success";
  }

  public String accountInformation(String username) {
    User user = userDAO.findByUsername(username);
    if (user == null) return "ERROR|User not found";

    // Đóng gói quà tặng dưới dạng JSON để gửi về Client
    JsonObject resp = new JsonObject();
    resp.addProperty("command", "ACCOUNT_INFO");
    resp.addProperty("username", user.getUsername());
    resp.addProperty("password", user.getPasswordHash());
    resp.addProperty("email", user.getEmail());
    resp.addProperty("phone", user.getPhone() != null ? user.getPhone() : "");
    resp.addProperty("personalID", user.getPersonalId() != null ? user.getPersonalId() : "");

    // Lấy số dư
    double balanceValue = (user instanceof Bidder b) ? b.getBalance() : 0.0;
    resp.addProperty("balance", String.format("%.2f", balanceValue));

    return resp.toString(); // Biến Object thành chuỗi JSON
  }

  // Hàm Quên Mật Khẩu
  public String forgotPassword(String username, String phone, String personalID) {
    // Tìm user trong DB
    User user = userDAO.findByUsername(username);

    JsonObject res = new JsonObject();
    res.addProperty("command", "FORGOT_PASSWORD_RESULT");

    // Kiểm tra xem User có tồn tại không VÀ Số điện thoại + CCCD có khớp chính xác không
    if (user != null
            && user.getPhone() != null && user.getPhone().equals(phone)
            && user.getPersonalId() != null && user.getPersonalId().equals(personalID)) {

      res.addProperty("status", "SUCCESS");
      res.addProperty("password", user.getPasswordHash()); // Moi mật khẩu ra
    } else {
      res.addProperty("status", "FAILED");
    }

    return res.toString(); // Trả JSON về cho Client
  }

  //Hàm thêm tiền vào tài khoản
  public String addMoney(String username, String money) {
    try {
      String cleanUsername = username.trim();
      double amountToAdd = Double.parseDouble(money.trim());

      User user = userDAO.findByUsername(cleanUsername);
      if (user == null) {
        return "{\"command\":\"ERROR\", \"message\":\"User not found\"}";
      }

      if (user instanceof Bidder bidder) {
        // Lấy số dư cũ + tiền nạp thêm
        double newTotal = bidder.getBalance() + amountToAdd;
        userDAO.updateBalance(cleanUsername, newTotal);

        // 5. Đóng gói kết quả trả về dạng JSON
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("command", "MONEY_UPDATE");
        responseJson.addProperty("balance", String.format("%.2f", newTotal));

        return responseJson.toString();
      }
      else {
        return "{\"command\":\"ERROR\", \"message\":\"Only bidders can add money\"}";
      }
    }
    catch(NumberFormatException e){
      return "{\"command\":\"ERROR\", \"message\":\"Invalid money format\"}";
    }
  }
}
