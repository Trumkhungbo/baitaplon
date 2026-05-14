package action.SellingJobs;

import action.Core.StartScence;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.FlowPane;

import java.net.URL;
import java.util.ResourceBundle;

public class InvesmentWaitHandle implements Initializable {

    // Khai báo FXML ở cấp độ class
    @FXML
    private FlowPane flowPane;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Tự động chạy khi FXML load xong
        initData();
    }

    public void initData() {
        if (flowPane != null) {
            flowPane.getChildren().clear();
        }
        fetchAuctionsFromServer();
    }

    public void fetchAuctionsFromServer() {
        // 1. Lắng nghe phản hồi từ Server
        StartScence.client.setServerListener(message -> {
            System.out.println("Nhận được từ Server: " + message);

            if (message.startsWith("AUCTION_LIST|")) {
                String dataPart = message.substring("AUCTION_LIST|".length());

                if (dataPart.isEmpty()) {
                    System.out.println("Không có sản phẩm nào.");
                    return;
                }

                // Chạy trên Thread của JavaFX
                Platform.runLater(() -> {
                    // Xóa dữ liệu cũ trên giao diện
                    if (flowPane != null) {
                        flowPane.getChildren().clear();
                    }

                    String[] items = dataPart.split(";");
                    for (String itemData : items) {
                        String[] attributes = itemData.split(":");
                        if (attributes.length >= 4) {
                            String id = attributes[0];
                            String itemName = attributes[1];
                            Double currentPrice = Double.parseDouble(attributes[2]);
                            String status = attributes[3];

                            // KHẮC PHỤC LỖI: Tạo mới 1 instance của Card
                            AuctionCardItem cardItem = new AuctionCardItem(itemName, id, currentPrice, status);

                            // Ném Card vừa tạo vào FlowPane
                            if (flowPane != null) {
                                flowPane.getChildren().add(cardItem);
                            }
                        }
                    }
                });
            }
        });
        StartScence.client.sendMessage("LIST_AUCTIONS");
    }
}

