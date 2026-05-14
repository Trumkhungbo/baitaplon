package action.SellingJobs;

import action.Authentication.StoreItemDataInit;
import action.MainUI.LobbyHandle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.text.DecimalFormat;

public class AuctionCardItem extends VBox {

    public AuctionCardItem(String name, String id, Double currentPrice, String status) {
        // Cấu trúc khung thẻ
        this.setSpacing(15);
        this.getStyleClass().add("auction-card"); // Dùng class trong CSS
        this.setPrefWidth(280); // Cố định chiều rộng để các card đều nhau khi dùng FlowPane
        this.setPadding(new Insets(20));

        // Tiêu đề sản phẩm
        Label lblTitle = new Label(name);
        lblTitle.getStyleClass().add("card-title");

        // Dòng chứa ID và Trạng thái
        HBox infoBox = new HBox(10);
        Label lblId = new Label("#" + id);
        lblId.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");

        Label lblStatus = new Label(status.toUpperCase());
        lblStatus.getStyleClass().add(status.equalsIgnoreCase("FINISHED") ? "status-finished" : "status-active");
        infoBox.getChildren().addAll(lblId, lblStatus);

        // Hiển thị giá tiền (Fix lỗi 1.5E7)
        DecimalFormat formatter = new DecimalFormat("#,###");
        Label lblPriceTag = new Label("Giá hiện tại:");
        lblPriceTag.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 12px;");

        Label lblPrice = new Label(formatter.format(currentPrice) + " VNĐ");
        lblPrice.getStyleClass().add("card-price");

        // Nút bấm tham gia
        Button actionBtn = new Button("Tham gia đấu giá");
        actionBtn.setMaxWidth(Double.MAX_VALUE); // Cho nút dài hết cỡ card
        actionBtn.getStyleClass().add("btn-gold-sm");

        actionBtn.setOnAction(e -> {
            StoreItemDataInit.name = name;
            StoreItemDataInit.description = id; // Bạn nên cân nhắc đổi description thành ID thực tế
            StoreItemDataInit.price = formatter.format(currentPrice);
            StoreItemDataInit.status = status;

            if (LobbyHandle.getInstance() != null) {
                try {
                    LobbyHandle.getInstance().ItemShowing();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        // Xếp các thành phần vào Card
        VBox priceContainer = new VBox(2, lblPriceTag, lblPrice);
        this.getChildren().addAll(lblTitle, infoBox, priceContainer, actionBtn);
    }
}