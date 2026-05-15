package com.bidding.server.network.command;

import com.bidding.common.payload.CreateItemRequest;
import com.bidding.common.model.item.Art;
import com.bidding.common.model.item.Electronics;
import com.bidding.common.model.item.Item;
import com.bidding.common.model.item.Vehicle;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.repository.ItemDAO;
import com.google.gson.Gson;

public class CreateItemCommand implements CommandHandler {

  // Khởi tạo ItemDAO trực tiếp để ít phải sửa đổi các class khác nhất
  private final ItemDAO itemDAO = new ItemDAO();

  @Override
  public void handle(String[] parts, ClientHandler client) {
    // 1. Kiểm tra xem người dùng đã đăng nhập chưa
    if (!client.isLoggedIn()) {
      client.sendMessage("ERROR|Bạn cần đăng nhập để đăng bán sản phẩm!");
      return;
    }

    // 2. Kiểm tra dữ liệu gửi lên có bị thiếu không
    if (parts.length < 2) {
      client.sendMessage("ERROR|Dữ liệu sản phẩm bị trống!");
      return;
    }

    try {
      // 3. Lấy chuỗi JSON (nằm ở phần tử thứ 2 sau khi split)
      String jsonString = parts[1];

      // 4. Dịch JSON ngược lại thành đối tượng Request
      Gson gson = new Gson();
      CreateItemRequest request = gson.fromJson(jsonString, CreateItemRequest.class);

      // 5. Chuyển đổi Request thành Model chuẩn của nhóm em
      Item item = null;
      switch (request.getItemType()) {
        case ART -> {
          Art art = new Art();
          art.setArtist(request.getArtist());
          art.setCreationYear(request.getCreationYear());
          item = art;
        }
        case ELECTRONICS -> {
          Electronics e = new Electronics();
          e.setBrand(request.getBrand());
          e.setWarrantyMonths(request.getWarrantyMonths());
          item = e;
        }
        case VEHICLE -> {
          Vehicle v = new Vehicle();
          v.setEngineType(request.getEngineType());
          v.setMileage(request.getMileage());
          item = v;
        }
      }

      if (item != null) {
        // Set các trường thông tin chung
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setStartingPrice(request.getStartingPrice());
        item.setItemType(request.getItemType());

        // 6. Lưu vào Database thông qua ItemDAO
        // Sử dụng client.getCurrentUser() để lấy tên người đang đăng bán
        itemDAO.save(item, client.getCurrentUser());

        client.sendMessage("SUCCESS|Đã đăng bán sản phẩm thành công!");
        System.out.println("[SERVER] Đã lưu sản phẩm mới từ user: " + client.getCurrentUser());
      } else {
        client.sendMessage("ERROR|Loại sản phẩm không hợp lệ.");
      }

    } catch (Exception e) {
      System.out.println("[SERVER ERROR] Lỗi khi tạo sản phẩm: " + e.getMessage());
      e.printStackTrace();
      client.sendMessage("ERROR|Lỗi hệ thống khi lưu sản phẩm.");
    }
  }
}