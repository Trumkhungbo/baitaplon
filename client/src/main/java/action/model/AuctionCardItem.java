package action.model;

import action.controller.main.LobbyHandle;
import action.network.SocketClient;
import action.network.SocketListener;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Base64;
import java.util.Locale;

public class AuctionCardItem extends VBox implements SocketListener {

    private final String name;
    private final String id;
    private final Double currentPrice;
    private final String status;
    private final String imageUrl;
    private final String itemType;
    private final String startTime;
    private final long endTimeMillis;
    private final long serverClockOffsetMillis;

    private final ImageView productImage = new ImageView();
    private final Label timerLabel = new Label();
    private final DecimalFormat formatter;
    {
        DecimalFormatSymbols s = DecimalFormatSymbols.getInstance(Locale.US);
        s.setGroupingSeparator('.');
        formatter = new DecimalFormat("#,###", s);
    }
    private Timeline timerTimeline;

    public AuctionCardItem(
            String name,
            String id,
            Double currentPrice,
            String status,
            String imageUrl,
            String itemType,
            String startTime,
            long endTimeMillis,
            long serverTimeMillis
    ) {
        this.name = name;
        this.id = id;
        this.currentPrice = currentPrice;
        this.status = status == null ? "UNKNOWN" : status;
        this.imageUrl = imageUrl == null ? "" : imageUrl;
        this.itemType = itemType == null || itemType.isBlank() ? "OTHER" : itemType;
        this.startTime = startTime == null ? "--:--" : startTime;
        this.endTimeMillis = endTimeMillis;
        this.serverClockOffsetMillis = serverTimeMillis - System.currentTimeMillis();

        setSpacing(12);
        getStyleClass().add("auction-card");
        setPrefWidth(370);
        setPadding(new Insets(16));
        addEventHandler(MouseEvent.MOUSE_CLICKED, event -> openDetail());

        productImage.setFitWidth(330);
        productImage.setFitHeight(170);
        productImage.setPreserveRatio(true);
        productImage.setSmooth(true);

        VBox imageBox = new VBox(productImage);
        imageBox.setMinHeight(170);
        imageBox.getStyleClass().add("auction-card-image-box");

        Label titleLabel = new Label(name);
        titleLabel.getStyleClass().add("card-title");
        titleLabel.setWrapText(true);

        HBox topMeta = new HBox(10);
        Label idLabel = new Label("#" + id);
        idLabel.getStyleClass().add("auction-card-id");
        Label statusLabel = new Label(this.status.toUpperCase());
        statusLabel.getStyleClass().add(this.status.equalsIgnoreCase("FINISHED") ? "status-finished" : "status-active");
        topMeta.getChildren().addAll(idLabel, statusLabel);

        Label priceTag = new Label("Giá hiện tại");
        priceTag.getStyleClass().add("auction-card-muted");

        Label priceLabel = new Label(formatter.format(currentPrice) + " VNĐ");
        priceLabel.getStyleClass().add("card-price");

        HBox bottomMeta = new HBox(10);
        Label typeBadge = new Label(normalizeTypeLabel(this.itemType));
        typeBadge.getStyleClass().add("auction-type-badge");
        HBox.setHgrow(typeBadge, Priority.NEVER);

        timerLabel.getStyleClass().add("auction-timer-label");
        updateTimerText();
        startTimer();

        bottomMeta.getChildren().addAll(timerLabel, typeBadge);

        Button actionButton = new Button(this.status.equalsIgnoreCase("FINISHED") ? "Xem kết quả" : "Tham gia đấu giá");
        actionButton.setMaxWidth(Double.MAX_VALUE);
        actionButton.getStyleClass().add(this.status.equalsIgnoreCase("FINISHED") ? "auction-finished-button" : "btn-gold-sm");
        actionButton.setOnAction(event -> {
            event.consume();
            openDetail();
        });

        getChildren().addAll(imageBox, titleLabel, topMeta, priceTag, priceLabel, bottomMeta, actionButton);

        loadImage();
    }

    private void startTimer() {
        if ("FINISHED".equalsIgnoreCase(status)) {
            return;
        }
        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateTimerText()));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
        timerTimeline.play();
    }

    private void updateTimerText() {
        if ("OPEN".equalsIgnoreCase(status)) {
            timerLabel.setText("Bắt đầu: " + startTime);
            return;
        }
        if ("FINISHED".equalsIgnoreCase(status)) {
            timerLabel.setText("Đã kết thúc");
            return;
        }

        long now = System.currentTimeMillis() + serverClockOffsetMillis;
        long remainingMillis = Math.max(0, endTimeMillis - now);
        long totalSeconds = remainingMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        timerLabel.setText(String.format("Còn lại: %02d:%02d:%02d", hours, minutes, seconds));

        if (remainingMillis == 0 && timerTimeline != null) {
            timerTimeline.stop();
        }
    }

    private String normalizeTypeLabel(String rawType) {
        return switch (rawType.toUpperCase()) {
            case "ELECTRONICS" -> "Electronics";
            case "ART" -> "Art";
            case "VEHICLE", "VEHICLES" -> "Vehicles";
            default -> rawType;
        };
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
            if (timerTimeline != null) {
                timerTimeline.stop();
            }
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
