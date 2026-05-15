package action.SellingJobs;

import action.Core.StartScence;
import javafx.application.Platform;
import java.util.ArrayList;
import java.util.List;

public class AuctionItems {
    // Biến lưu trữ dùng chung cho tất cả các trang
    public static ArrayList<List<Object>> list = new ArrayList<>();

    // Sự kiện để báo cho UI biết là dữ liệu đã tải xong
    public interface DataChangeListener {
        void onDataChanged();
    }
    public static DataChangeListener currentListener;

    // Yêu cầu Server gửi dữ liệu mới
    public static void requestData() {
        StartScence.client.sendMessage("LIST_AUCTIONS");
        StartScence.client.setServerListener(message -> {
            if (message != null && message.startsWith("AUCTION_LIST|")) {
                updateListFromServer(message);
            }
        });
    }

    // Hàm này được SocketClient gọi khi nhận được chuỗi
    public static void updateListFromServer(String message) {
        String dataPart = message.substring("AUCTION_LIST|".length());

        Platform.runLater(() -> {
            list.clear(); // Xóa cũ đi
            if (!dataPart.isEmpty()) {
                String[] items = dataPart.split(";");
                for (String itemData : items) {
                    String[] attributes = itemData.split(":");
                    if (attributes.length ==4) {
                        String id = attributes[0];
                        String itemName = attributes[1];
                        Double currentPrice = Double.parseDouble(attributes[2]);
                        String status = attributes[3];
                        // Lưu theo đúng thứ tự
                        list.add(List.of(itemName, id, currentPrice, status));
                    }
                }
            }
            // Báo cho trang FXML (InvesmentSite, Wait, Admin) vẽ lại UI
            if (currentListener != null) {
                currentListener.onDataChanged();
            }
        });
    }
}