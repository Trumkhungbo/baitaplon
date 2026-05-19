package action.SellingJobs;

import action.Authentication.StoreItemDataInit;
import action.MainUI.LobbyHandle;
import action.SocketClient;
import action.SocketListener;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Base64;

public class AuctionCardItem extends VBox implements SocketListener {

    private final String name;
    private final String id;
    private final Double currentPrice;
    private final String status;
    private final String imageUrl;
    private final ImageView productImage = new ImageView();
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public AuctionCardItem(String name, String id, Double currentPrice, String status) {
        this(name, id, currentPrice, status, "");
    }

    public AuctionCardItem(String name, String id, Double currentPrice, String status, String imageUrl) {
        this.name = name;
        this.id = id;
        this.currentPrice = currentPrice;
        this.status = status == null ? "UNKNOWN" : status;
        this.imageUrl = imageUrl == null ? "" : imageUrl;

        setSpacing(15);
        getStyleClass().add("auction-card");
        setPrefWidth(280);
        setPadding(new Insets(20));
        addEventHandler(MouseEvent.MOUSE_CLICKED, event -> openDetail());

        productImage.setFitWidth(240);
        productImage.setFitHeight(150);
        productImage.setPreserveRatio(true);
        productImage.setSmooth(true);

        VBox imageBox = new VBox(productImage);
        imageBox.setMinHeight(150);
        imageBox.setStyle("-fx-background-color: #262626; -fx-background-radius: 10; -fx-alignment: center;");

        Label titleLabel = new Label(name);
        titleLabel.getStyleClass().add("card-title");

        HBox infoBox = new HBox(10);
        Label idLabel = new Label("#" + id);
        idLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        Label statusLabel = new Label(this.status.toUpperCase());
        statusLabel.getStyleClass().add(this.status.equalsIgnoreCase("FINISHED") ? "status-finished" : "status-active");
        infoBox.getChildren().addAll(idLabel, statusLabel);

        Label priceTag = new Label("Giá hiện tại:");
        priceTag.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 12px;");

        Label priceLabel = new Label(formatter.format(currentPrice) + " VNĐ");
        priceLabel.getStyleClass().add("card-price");

        Button actionButton = new Button(this.status.equalsIgnoreCase("FINISHED") ? "Xem kết quả" : "Tham gia đấu giá");
        actionButton.setMaxWidth(Double.MAX_VALUE);
        if (this.status.equalsIgnoreCase("FINISHED")) {
            actionButton.setStyle("-fx-background-color: transparent; -fx-border-color: #FF4444; -fx-border-radius: 8; -fx-text-fill: #FF6666; -fx-font-weight: bold; -fx-padding: 10;");
        } else {
            actionButton.getStyleClass().add("btn-gold-sm");
        }
        actionButton.setOnAction(event -> {
            event.consume();
            openDetail();
        });

        VBox priceBox = new VBox(2, priceTag, priceLabel);
        getChildren().addAll(imageBox, titleLabel, infoBox, priceBox, actionButton);

        loadImage();
    }

    private void openDetail() {
        StoreItemDataInit.name = name;
        StoreItemDataInit.description = id;
        StoreItemDataInit.price = formatter.format(currentPrice);
        StoreItemDataInit.status = status;
        StoreItemDataInit.image = imageUrl;

        if (LobbyHandle.getInstance() == null) {
            System.err.println("[AuctionCardItem] LobbyHandle instance is null");
            return;
        }

        try {
            LobbyHandle.getInstance().ItemShowing();
        } catch (IOException ex) {
            System.err.println("[AuctionCardItem] Failed to open ItemShowing.fxml");
            ex.printStackTrace();
        }
    }

    private void loadImage() {
        if (imageUrl.isBlank()) {
            return;
        }
        SocketClient.getInstance().addListener(this);
        SocketClient.getInstance().requestData("GET_IMAGE|" + imageUrl);
    }

    @Override
    public void onDataReceived(String data) {
        if (data == null || !data.startsWith("IMAGE_DATA|")) {
            return;
        }

        String[] parts = data.split("\\|", 4);
        if (parts.length < 4 || !imageUrl.equals(parts[1])) {
            return;
        }

        Platform.runLater(() -> {
            try {
                byte[] bytes = Base64.getDecoder().decode(parts[3]);
                productImage.setImage(new Image(new ByteArrayInputStream(bytes)));
                SocketClient.getInstance().removeListener(this);
            } catch (IllegalArgumentException ignored) {
            }
        });
    }
}
