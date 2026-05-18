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
import javafx.scene.layout.Region;
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
        this.status = status;
        this.imageUrl = imageUrl == null ? "" : imageUrl;

        this.setSpacing(15);
        this.getStyleClass().add("auction-card");
        this.setPrefWidth(280);
        this.setPadding(new Insets(20));
        this.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> openDetail());

        productImage.setFitWidth(240);
        productImage.setFitHeight(150);
        productImage.setPreserveRatio(true);
        productImage.setSmooth(true);

        Region imagePlaceholder = new Region();
        imagePlaceholder.setPrefSize(240, 150);
        imagePlaceholder.setStyle("-fx-background-color: #262626; -fx-background-radius: 10;");
        VBox imageBox = new VBox(productImage);
        imageBox.setMinHeight(150);
        imageBox.setStyle("-fx-background-color: #262626; -fx-background-radius: 10; -fx-alignment: center;");

        Label lblTitle = new Label(name);
        lblTitle.getStyleClass().add("card-title");

        HBox infoBox = new HBox(10);
        Label lblId = new Label("#" + id);
        lblId.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
        Label lblStatus = new Label(status.toUpperCase());
        lblStatus.getStyleClass().add(status.equalsIgnoreCase("FINISHED") ? "status-finished" : "status-active");
        infoBox.getChildren().addAll(lblId, lblStatus);

        Label lblPriceTag = new Label("Giá hiện tại:");
        lblPriceTag.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 12px;");

        Label lblPrice = new Label(formatter.format(currentPrice) + " VNĐ");
        lblPrice.getStyleClass().add("card-price");

        Button actionBtn = new Button("Tham gia đấu giá");
        actionBtn.setMaxWidth(Double.MAX_VALUE);
        actionBtn.getStyleClass().add("btn-gold-sm");
        actionBtn.setOnAction(e -> {
            e.consume();
            openDetail();
        });

        VBox priceContainer = new VBox(2, lblPriceTag, lblPrice);
        this.getChildren().addAll(imageBox, lblTitle, infoBox, priceContainer, actionBtn);

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
