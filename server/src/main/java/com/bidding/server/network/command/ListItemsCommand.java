package com.bidding.server.network.command;

import com.bidding.common.model.item.Art;
import com.bidding.common.model.item.Electronics;
import com.bidding.common.model.item.Item;
import com.bidding.common.model.item.Vehicle;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.repository.ItemDAO;
import java.util.List;

public class ListItemsCommand implements CommandHandler {
  private final ItemDAO itemDAO = new ItemDAO();

  @Override
  public void handle(String[] parts, ClientHandler client) {
    try {
      List<Item> items = itemDAO.findAll();
      StringBuilder sb = new StringBuilder("ITEM_LIST|"); // Tiền tố gửi về

      for (Item item : items) {
        String type = item.getItemType().name();
        String info1 = "", info2 = "";

        // Lấy đúng thông tin tùy theo loại sản phẩm
        if (item instanceof Art a) {
          info1 = a.getArtist() != null ? a.getArtist() : "";
          info2 = String.valueOf(a.getCreationYear());
        } else if (item instanceof Electronics e) {
          info1 = e.getBrand() != null ? e.getBrand() : "";
          info2 = String.valueOf(e.getWarrantyMonths());
        } else if (item instanceof Vehicle v) {
          info1 = v.getEngineType() != null ? v.getEngineType() : "";
          info2 = String.valueOf(v.getMileage());
        }

        // Gộp lại thành chuỗi: id:name:type:info1:info2:price;
        sb.append(item.getId()).append(":")
                .append(item.getName()).append(":")
                .append(type).append(":")
                .append(info1).append(":")
                .append(info2).append(":")
                .append(item.getStartingPrice()).append(";");
      }

      client.sendMessage(sb.toString());

    } catch (Exception e) {
      client.sendMessage("ERROR|Không thể tải danh sách sản phẩm.");
      e.printStackTrace();
    }
  }
}