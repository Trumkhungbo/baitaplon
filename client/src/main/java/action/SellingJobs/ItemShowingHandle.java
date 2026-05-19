package action.SellingJobs;

import action.Authentication.StoreDataInput;
import action.Authentication.StoreItemDataInit;
import action.Core.SceneSwitch;
import action.SocketClient;
import action.SocketListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class ItemShowingHandle implements Initializable, SocketListener {

    @FXML private Button buttonLeft;
    @FXML private Button buttonRight;
    @FXML private ImageView imageView;
    @FXML private ImageView image;

    @FXML private Label name;
    @FXML private Label price;
    @FXML private Label status;
    @FXML private Label description;
    @FXML private Label date;
    @FXML private Label starTime;
    @FXML private Label duration;
    @FXML private Label Type;
    @FXML private Label information1;
    @FXML private Label information2;
    @FXML private Label balanceValue;
    @FXML private Label avatarLabel;
    @FXML private Label bidHistoryHint;
    @FXML private Label autoBidStatus;
    @FXML private TextField money;
    @FXML private TextField autoBidMax;
    @FXML private TextField autoBidIncrement;
    @FXML private ProgressBar timeProgress;
    @FXML private TableView<BidHistoryRow> bidHistoryTable;
    @FXML private TableColumn<BidHistoryRow, String> bidUserColumn;
    @FXML private TableColumn<BidHistoryRow, String> bidAmountColumn;
    @FXML private TableColumn<BidHistoryRow, String> bidTimeColumn;

    private final ObservableList<BidHistoryRow> bidHistoryRows = FXCollections.observableArrayList();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")
            .withZone(ZoneId.systemDefault());

    private Timeline countdownTimeline;
    private long endTimeMillis;
    private long serverClockOffsetMillis;
    private long durationMinutes;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupBidHistoryTable();
        SocketClient.getInstance().addListener(this);
        watchAuction();
        updateAvatar();
        getItem();
        getBidHistory();
        getAccountBalance();
    }

    private void watchAuction() {
        String auctionId = StoreItemDataInit.description;
        if (auctionId != null && !auctionId.isBlank()) {
            SocketClient.getInstance().requestData("WATCH|" + auctionId);
        }
    }

    public void getItem() {
        SocketClient.getInstance().requestData("GET_AUCTION_DETAIL|" + StoreItemDataInit.description);
    }

    private void getBidHistory() {
        String auctionId = StoreItemDataInit.description;
        if (auctionId != null && !auctionId.isBlank()) {
            SocketClient.getInstance().requestData("GET_BID_HISTORY|" + auctionId);
        }
    }

    private void getAccountBalance() {
        String username = StoreDataInput.getUsername();
        if (username == null || username.isBlank()) {
            setLabelText(balanceValue, "0 VNĐ");
            return;
        }

        JsonObject req = new JsonObject();
        req.addProperty("command", "GET_ACCOUNTINFORMATION");
        req.addProperty("username", username);
        SocketClient.getInstance().requestData(req.toString());
    }

    private void setupBidHistoryTable() {
        if (bidHistoryTable == null) {
            return;
        }

        bidUserColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().username()));
        bidAmountColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().amount()));
        bidTimeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().time()));
        bidHistoryTable.setItems(bidHistoryRows);
    }

    private void updateAvatar() {
        String username = StoreDataInput.getUsername();
        if (username == null || username.isBlank()) {
            setLabelText(avatarLabel, "U");
            return;
        }
        setLabelText(avatarLabel, username.substring(0, 1).toUpperCase());
    }

    @FXML
    public void RaiseBind(ActionEvent actionEvent) {
        String auctionId = StoreItemDataInit.description;
        String amountText = cleanNumber(money.getText());

        if (auctionId == null || auctionId.isBlank()) {
            setLabelText(status, "Thiếu mã phiên đấu giá");
            return;
        }

        if (amountText == null || !amountText.matches("\\d+(\\.\\d+)?")) {
            setLabelText(status, "Số tiền đặt giá không hợp lệ");
            return;
        }

        SocketClient.getInstance().requestData("BID|" + auctionId + "|" + amountText);
    }

    @FXML
    public void EnableAutoBid(ActionEvent actionEvent) {
        String auctionId = StoreItemDataInit.description;
        String maxBid = cleanNumber(autoBidMax.getText());
        String increment = cleanNumber(autoBidIncrement.getText());

        if (auctionId == null || auctionId.isBlank()) {
            setAutoBidStatus("Thiếu mã phiên đấu giá");
            return;
        }

        if (maxBid == null || !maxBid.matches("\\d+(\\.\\d+)?")
                || increment == null || !increment.matches("\\d+(\\.\\d+)?")) {
            setAutoBidStatus("Thông tin auto-bid không hợp lệ");
            return;
        }

        setAutoBidStatus("Đang bật auto-bid...");
        SocketClient.getInstance().requestData("SET_AUTO_BID|" + auctionId + "|" + maxBid + "|" + increment);
    }

    @FXML
    public void ReturnToInvesment(ActionEvent actionEvent) throws IOException {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        SocketClient.getInstance().requestData("WATCH|");
        SocketClient.getInstance().removeListener(this);
        SceneSwitch sceneSwitch = new SceneSwitch();
        sceneSwitch.SwitchToAnyWhere(actionEvent, "/views/Lobby.fxml");
    }

    @Override
    public void onDataReceived(String data) {
        Platform.runLater(() -> {
            if (data == null || data.isBlank()) {
                return;
            }

            String[] parts = data.split("\\|");
            String command = parts[0];
            Map<String, String> params = parseParams(parts);

            switch (command) {
                case "AUCTION_DETAIL" -> renderAuctionDetail(params);
                case "BID_UPDATE", "BID_RESULT", "BID_SUCCESS" -> renderBidUpdate(params);
                case "BID_HISTORY" -> renderBidHistory(params.getOrDefault("entries", ""));
                case "AUTO_BID_SET" -> {
                    setAutoBidStatus("Đã bật auto-bid cho phiên này");
                    getBidHistory();
                    getAccountBalance();
                }
                case "AUCTION_STARTED" -> {
                    setLabelText(status, "RUNNING");
                    getItem();
                }
                case "AUCTION_CLOSED" -> {
                    setLabelText(status, "FINISHED");
                    setLabelText(starTime, "00:00:00");
                    if (timeProgress != null) {
                        timeProgress.setProgress(0);
                    }
                    if (params.containsKey("finalPrice")) {
                        setLabelText(price, formatMoney(params.get("finalPrice")) + " VNĐ");
                    }
                    getBidHistory();
                    getItem();
                }
                case "ERROR" -> renderError(parts, params);
                case "IMAGE_DATA" -> renderImage(parts);
                default -> renderAccountInfo(data);
            }
        });
    }

    private Map<String, String> parseParams(String[] parts) {
        Map<String, String> params = new HashMap<>();
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.contains("=")) {
                String[] keyValue = part.split("=", 2);
                params.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return params;
    }

    private void renderAuctionDetail(Map<String, String> params) {
        setLabelText(name, params.getOrDefault("itemName", "Dang cap nhat"));
        setLabelText(price, formatMoney(params.getOrDefault("currentPrice", "0")) + " VNĐ");
        setLabelText(status, params.getOrDefault("status", "UNKNOWN"));
        setLabelText(date, params.getOrDefault("startDate", "--/--/----"));
        setLabelText(Type, params.getOrDefault("itemType", "Chua co du lieu"));
        setLabelText(information1, params.getOrDefault("information1", "Chua co du lieu"));
        setLabelText(information2, params.getOrDefault("information2", "Chua co du lieu"));
        setLabelText(description, params.getOrDefault("description", "Chua co mo ta chi tiet."));

        String durationVal = params.containsKey("duration")
                ? params.get("duration")
                : params.getOrDefault("durationMinutes", "0");
        setLabelText(duration, durationVal + " phut");
        updateCountdownConfig(params, durationVal);

        String imageUrl = params.getOrDefault("imageUrl", "");
        StoreItemDataInit.image = imageUrl;
        if (!imageUrl.isBlank()) {
            SocketClient.getInstance().requestData("GET_IMAGE|" + imageUrl);
        }
    }

    private void renderBidUpdate(Map<String, String> params) {
        String resStatus = params.getOrDefault("status", "SUCCESS");
        if ("SUCCESS".equals(resStatus)) {
            setLabelText(status, "RUNNING");
            if (params.containsKey("highestBid")) {
                setLabelText(price, formatMoney(params.get("highestBid")) + " VNĐ");
            } else if (params.containsKey("amount")) {
                setLabelText(price, formatMoney(params.get("amount")) + " VNĐ");
            }
            updateCountdownConfig(params, params.getOrDefault("duration", String.valueOf(durationMinutes)));
            if (money != null) {
                money.clear();
            }
            getBidHistory();
            getAccountBalance();
            return;
        }

        setLabelText(status, params.getOrDefault("message", "Loi dat gia"));
    }

    private void renderBidHistory(String entries) {
        bidHistoryRows.clear();

        if (entries == null || entries.isBlank()) {
            setLabelText(bidHistoryHint, "Chua co luot dat gia nao.");
            return;
        }

        for (String entry : entries.split(";")) {
            String[] fields = entry.split(",", 3);
            if (fields.length < 3) {
                continue;
            }

            bidHistoryRows.add(new BidHistoryRow(
                    fields[0],
                    formatMoney(fields[1]) + " VNĐ",
                    formatTimestamp(fields[2])
            ));
        }

        setLabelText(bidHistoryHint, bidHistoryRows.isEmpty()
                ? "Chua co luot dat gia nao."
                : "Da tai " + bidHistoryRows.size() + " luot dat gia.");
    }

    private void updateCountdownConfig(Map<String, String> params, String durationValue) {
        try {
            if (params.containsKey("endTime")) {
                endTimeMillis = Long.parseLong(params.get("endTime"));
            }
            if (params.containsKey("serverTime")) {
                serverClockOffsetMillis = Long.parseLong(params.get("serverTime")) - System.currentTimeMillis();
            }
            durationMinutes = Long.parseLong(durationValue);
            startCountdown();
        } catch (NumberFormatException ignored) {
        }
    }

    private void startCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateCountdown()));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        updateCountdown();
        countdownTimeline.play();
    }

    private void updateCountdown() {
        if (endTimeMillis <= 0) {
            return;
        }

        long now = System.currentTimeMillis() + serverClockOffsetMillis;
        long remainingMillis = Math.max(0, endTimeMillis - now);
        long totalMillis = Math.max(1, durationMinutes * 60_000L);
        double progress = Math.max(0.0, Math.min(1.0, (double) remainingMillis / totalMillis));

        if (timeProgress != null) {
            timeProgress.setProgress(progress);
        }
        setLabelText(starTime, formatRemainingTime(remainingMillis));

        if (remainingMillis == 0) {
            setLabelText(status, "FINISHED");
            if (countdownTimeline != null) {
                countdownTimeline.stop();
            }
        }
    }

    private String formatRemainingTime(long remainingMillis) {
        long totalSeconds = remainingMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void renderError(String[] parts, Map<String, String> params) {
        String message = params.get("message");
        if ((message == null || message.isBlank()) && parts.length > 1) {
            message = parts[1];
        }

        String safeMessage = message == null || message.isBlank() ? "Da xay ra loi!" : message;
        if ("Auction is not available".equalsIgnoreCase(safeMessage)) {
            setLabelText(status, "FINISHED");
            setLabelText(starTime, "00:00:00");
            if (timeProgress != null) {
                timeProgress.setProgress(0);
            }
            getItem();
            getBidHistory();
            return;
        }

        setLabelText(status, safeMessage);
        setAutoBidStatus(safeMessage);
    }

    private void renderImage(String[] parts) {
        if (parts.length < 4 || StoreItemDataInit.image == null || !StoreItemDataInit.image.equals(parts[1])) {
            return;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(parts[3]);
            getProductImageView().setImage(new Image(new ByteArrayInputStream(bytes)));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void renderAccountInfo(String data) {
        try {
            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            if (!json.has("command") || !"ACCOUNT_INFO".equals(json.get("command").getAsString())) {
                return;
            }

            String balance = json.has("balance") ? json.get("balance").getAsString() : "0";
            setLabelText(balanceValue, formatMoney(balance) + " VNĐ");
        } catch (Exception ignored) {
        }
    }

    private String formatTimestamp(String raw) {
        try {
            return timeFormatter.format(Instant.ofEpochMilli(Long.parseLong(raw)));
        } catch (Exception e) {
            return raw;
        }
    }

    private String formatMoney(String raw) {
        try {
            double value = Double.parseDouble(raw);
            return String.format("%,.0f", value);
        } catch (NumberFormatException e) {
            return raw == null || raw.isBlank() ? "0" : raw;
        }
    }

    private String cleanNumber(String value) {
        return value == null ? null : value.trim().replace(",", "").replace(" ", "");
    }

    private void setLabelText(Label label, String value) {
        if (label != null) {
            label.setText(value == null || value.isBlank() ? "Chua co du lieu" : value);
        }
    }

    private void setAutoBidStatus(String value) {
        setLabelText(autoBidStatus, value);
    }

    private ImageView getProductImageView() {
        return imageView != null ? imageView : image;
    }

    public record BidHistoryRow(String username, String amount, String time) {
    }
}
