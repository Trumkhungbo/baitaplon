package action.SellingJobs;

import action.SocketClient;
import action.SocketListener;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.FlowPane;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class InvesmentSiteHandle implements Initializable, SocketListener {
    @FXML
    private FlowPane flowPane;

    private List<List<Object>> list = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (flowPane != null) {
            flowPane.getChildren().clear();
        }
        SocketClient.getInstance().addListener(this);
        SocketClient.getInstance().requestData("LIST_AUCTIONS");
    }

    public void fetchAuctionsFromServer() {
        if (flowPane != null) {
            flowPane.getChildren().clear(); // Xóa sạch danh sách cũ trước khi vẽ danh sách mới
        }

        for (List item : list) {
            if (!item.isEmpty()) {
                String status = (String) item.get(3);
                // Hiển thị cả phiên đấu giá đang chạy (RUNNING) và mới mở (OPEN)
                if ("RUNNING".equals(status) ) {
                    AuctionCardItem cardItem = new AuctionCardItem(
                            (String) item.get(0),
                            (String) item.get(1),
                            (Double) item.get(2),
                            status
                    );
                    if (flowPane != null) {
                        flowPane.getChildren().add(cardItem);
                    }
                }
            }
        }
    }

    @Override
    public void onDataReceived(String data) {
        if (data != null && data.startsWith("AUCTION_LIST|")) {
            String dataPart = data.substring("AUCTION_LIST|".length());

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
                fetchAuctionsFromServer();
            });
        }
    }
}