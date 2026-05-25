package action.SellingJobs;

import action.Authentication.StoreDataInput;
import action.Authentication.StoreItemDataInit;
import action.MainUI.LobbyHandle;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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
    @FXML private Label heroStatusChip;
    @FXML private Label modeLabel;
    @FXML private Label statusSubLabel;
    @FXML private Label auctionCodeLabel;
    @FXML private Label scheduleLabel;
    @FXML private Label historyTitleLabel;
    @FXML private Label historyModeLabel;
    @FXML private Label sellerLabel;
    @FXML private Label winnerAvatarLabel;
    @FXML private Label winnerNameLabel;
    @FXML private Label winnerPriceLabel;
    @FXML private Label startPriceLabel;
    @FXML private Label finalPriceStatLabel;
    @FXML private Label bidCountLabel;
    @FXML private Label finishedAtLabel;
    @FXML private Label resultHintLabel;
    @FXML private Label leaderValueLabel;
    @FXML private TextField money;
    @FXML private TextField autoBidMax;
    @FXML private TextField autoBidIncrement;
    @FXML private ProgressBar timeProgress;
    @FXML private TableView<BidHistoryRow> bidHistoryTable;
    @FXML private TableColumn<BidHistoryRow, String> bidUserColumn;
    @FXML private TableColumn<BidHistoryRow, String> bidAmountColumn;
    @FXML private TableColumn<BidHistoryRow, String> bidTimeColumn;
    @FXML private LineChart<String, Number> bidHistoryChart;
    @FXML private CategoryAxis bidHistoryTimeAxis;
    @FXML private NumberAxis bidHistoryPriceAxis;
    @FXML private VBox activeBidCard;
    @FXML private VBox finishedResultCard;
    @FXML private VBox autoBidCard;
    @FXML private VBox topWinnersBox;
    @FXML private Button raiseBidButton;
    @FXML private ToggleButton autoBidToggle;

    private final ObservableList<BidHistoryRow> bidHistoryRows = FXCollections.observableArrayList();
    private final XYChart.Series<String, Number> bidHistorySeries = new XYChart.Series<>();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")
            .withZone(ZoneId.systemDefault());
    private final DateTimeFormatter chartTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    private final DateTimeFormatter endTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private Timeline countdownTimeline;
    private long endTimeMillis;
    private long serverClockOffsetMillis;
    private long durationMinutes;
    private String currentAuctionStatus = "UNKNOWN";
    private boolean autoBidActive;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupBidHistoryTable();
        setupBidHistoryChart();
        syncAutoBidToggle();
        SocketClient.getInstance().addListener(this);
        watchAuction();
        updateAvatar();
        getItem();
        getBidHistory();
        getAccountBalance();
        getAutoBidStatus();
        applyStatusState("UNKNOWN");
    }

    private void setupBidHistoryTable() {
        bidUserColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().username()));
        bidAmountColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().amount()));
        bidTimeColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().time()));
        styleHistoryColumn(bidUserColumn);
        styleHistoryColumn(bidAmountColumn);
        styleHistoryColumn(bidTimeColumn);
        bidHistoryTable.setRowFactory(table -> {
            TableRow<BidHistoryRow> row = new TableRow<>();
            row.setStyle("-fx-background-color: #000000; -fx-table-cell-border-color: rgba(255,255,255,0.05);");
            return row;
        });
        bidHistoryTable.setPlaceholder(new Label("Chưa có lượt đặt giá nào."));
        bidHistoryTable.setItems(bidHistoryRows);
    }

    private void styleHistoryColumn(TableColumn<BidHistoryRow, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-background-color: #000000; -fx-text-fill: #f3f4f6; -fx-border-color: transparent; -fx-alignment: CENTER_LEFT;");
            }
        });
    }

    private void setupBidHistoryChart() {
        if (bidHistoryChart == null) {
            return;
        }

        bidHistorySeries.setName("Giá đặt");
        bidHistoryChart.getData().setAll(bidHistorySeries);
        bidHistoryChart.setLegendVisible(false);
        bidHistoryChart.setAnimated(false);

        if (bidHistoryTimeAxis != null) {
            bidHistoryTimeAxis.setTickLabelRotation(0);
        }
        if (bidHistoryPriceAxis != null) {
            bidHistoryPriceAxis.setForceZeroInRange(false);
            bidHistoryPriceAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(bidHistoryPriceAxis) {
                @Override
                public String toString(Number object) {
                    return object == null ? "" : formatMoney(String.valueOf(object.doubleValue()));
                }
            });
        }
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

    private void getWinnerInfo() {
        String auctionId = StoreItemDataInit.description;
        if (auctionId != null && !auctionId.isBlank()) {
            SocketClient.getInstance().requestData("GET_WINNER|" + auctionId);
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

    private void getAutoBidStatus() {
        String auctionId = StoreItemDataInit.description;
        if (auctionId != null && !auctionId.isBlank()) {
            SocketClient.getInstance().requestData("GET_AUTO_BID|" + auctionId);
        }
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
            setLabelText(statusSubLabel, "Thiếu mã phiên đấu giá");
            return;
        }

        if (amountText == null || !amountText.matches("\\d+(\\.\\d+)?")) {
            setLabelText(statusSubLabel, "Số tiền đặt giá không hợp lệ");
            return;
        }

        SocketClient.getInstance().requestData("BID|" + auctionId + "|" + amountText);
    }

    @FXML
    public void EnableAutoBid(ActionEvent actionEvent) {
        String auctionId = StoreItemDataInit.description;
        if (auctionId == null || auctionId.isBlank()) {
            syncAutoBidToggle();
            setAutoBidStatus("Thiếu mã phiên đấu giá.");
            return;
        }

        if (!"RUNNING".equalsIgnoreCase(currentAuctionStatus)) {
            syncAutoBidToggle();
            setAutoBidStatus("Auto-bid chỉ bật khi phiên đang RUNNING.");
            return;
        }

        if (autoBidActive) {
            if (autoBidToggle != null) {
                autoBidToggle.setText("OFF");
            }
            setAutoBidStatus("Đang tắt auto-bid...");
            SocketClient.getInstance().requestData("DISABLE_AUTO_BID|" + auctionId);
            return;
        }

        String maxBid = cleanNumber(autoBidMax.getText());
        String increment = cleanNumber(autoBidIncrement.getText());

        if (maxBid == null || !maxBid.matches("\\d+(\\.\\d+)?")
                || increment == null || !increment.matches("\\d+(\\.\\d+)?")) {
            syncAutoBidToggle();
            setAutoBidStatus("Thông tin auto-bid không hợp lệ.");
            return;
        }

        setAutoBidStatus("Đang bật auto-bid...");
        if (autoBidToggle != null) {
            autoBidToggle.setText("ON");
        }
        SocketClient.getInstance().requestData("SET_AUTO_BID|" + auctionId + "|" + maxBid + "|" + increment);
    }

    @FXML
    public void ReturnToInvesment(ActionEvent actionEvent) throws IOException {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        SocketClient.getInstance().requestData("WATCH|");
        SocketClient.getInstance().removeListener(this);
        if (LobbyHandle.getInstance() != null) {
            LobbyHandle.getInstance().MovingCenter("/views/InvesmentSite.fxml");
        }
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
                    renderAutoBidStatus(params);
                    setAutoBidStatus("Đã bật auto-bid cho phiên này");
                    getBidHistory();
                    getAccountBalance();
                }
                case "AUTO_BID_STATUS" -> renderAutoBidStatus(params);
                case "AUTO_BID_DISABLED" -> renderAutoBidDisabled();
                case "AUCTION_STARTED" -> {
                    applyStatusState("RUNNING");
                    getItem();
                }
                case "AUCTION_CLOSED" -> {
                    applyStatusState("FINISHED");
                    if (params.containsKey("winner")) {
                        updateWinnerCard(params.get("winner"));
                    }
                    if (params.containsKey("finalPrice")) {
                        String finalPriceText = formatMoney(params.get("finalPrice")) + " VNĐ";
                        setLabelText(price, finalPriceText);
                        setLabelText(winnerPriceLabel, finalPriceText);
                        setLabelText(finalPriceStatLabel, finalPriceText);
                    }
                    setLabelText(starTime, "00:00:00");
                    if (timeProgress != null) {
                        timeProgress.setProgress(0);
                    }
                    getBidHistory();
                    getWinnerInfo();
                    getItem();
                }
                case "WINNER_INFO" -> renderWinnerInfo(params);
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
        setLabelText(name, params.getOrDefault("itemName", "Đang cập nhật"));
        setLabelText(price, formatMoney(params.getOrDefault("currentPrice", "0")) + " VNĐ");
        applyStatusState(params.getOrDefault("status", "UNKNOWN"));
        setLabelText(date, params.getOrDefault("startDate", "--/--/----"));
        setLabelText(Type, params.getOrDefault("itemType", "Chưa có dữ liệu"));
        setLabelText(sellerLabel, params.getOrDefault("seller", "Chưa có dữ liệu"));
        setLabelText(information1, params.getOrDefault("information1", "Chưa có dữ liệu"));
        setLabelText(information2, params.getOrDefault("information2", "Chưa có dữ liệu"));
        setLabelText(description, params.getOrDefault("description", "Chưa có mô tả chi tiết."));
        setLabelText(auctionCodeLabel, "#" + params.getOrDefault("auctionId", StoreItemDataInit.description));
        setLabelText(scheduleLabel, "Theo giờ bắt đầu");

        String durationValue = params.containsKey("duration")
                ? params.get("duration")
                : params.getOrDefault("durationMinutes", "0");
        setLabelText(duration, durationValue + " phút");
        updateCountdownConfig(params, durationValue);

        setLabelText(startPriceLabel, formatMoney(params.getOrDefault("startPrice", "0")) + " VNĐ");
        setLabelText(finalPriceStatLabel, formatMoney(params.getOrDefault("currentPrice", "0")) + " VNĐ");
        setLabelText(winnerPriceLabel, formatMoney(params.getOrDefault("currentPrice", "0")) + " VNĐ");
        setLabelText(bidCountLabel, parseLong(params.getOrDefault("bidCount", "0")) + " lượt");
        setLabelText(leaderValueLabel, params.getOrDefault("highestBidder", "Chưa có"));
        if (params.containsKey("endTime")) {
            setLabelText(finishedAtLabel, endTimeFormatter.format(Instant.ofEpochMilli(parseLong(params.get("endTime")))));
        }

        if ("FINISHED".equalsIgnoreCase(currentAuctionStatus)) {
            getWinnerInfo();
        }

        String imageUrl = params.getOrDefault("imageUrl", "");
        StoreItemDataInit.image = imageUrl;
        if (!imageUrl.isBlank()) {
            SocketClient.getInstance().requestData("GET_IMAGE|" + imageUrl);
        }
    }

    private void renderBidUpdate(Map<String, String> params) {
        String resultStatus = params.getOrDefault("status", "SUCCESS");
        if ("SUCCESS".equalsIgnoreCase(resultStatus)) {
            applyStatusState("RUNNING");
            String amount = params.containsKey("highestBid")
                    ? params.get("highestBid")
                    : params.getOrDefault("amount", "0");
            String amountText = formatMoney(amount) + " VNĐ";
            setLabelText(price, amountText);
            setLabelText(finalPriceStatLabel, amountText);
            setLabelText(winnerPriceLabel, amountText);
            setLabelText(leaderValueLabel, params.getOrDefault("highestBidder", params.getOrDefault("user", "Chưa có")));
            updateCountdownConfig(params, params.getOrDefault("duration", String.valueOf(durationMinutes)));
            if (money != null) {
                money.clear();
            }
            getBidHistory();
            getAccountBalance();
            return;
        }

        setLabelText(statusSubLabel, params.getOrDefault("message", "Lỗi đặt giá"));
    }

    private void renderBidHistory(String entries) {
        bidHistoryRows.clear();

        if (entries == null || entries.isBlank()) {
            setLabelText(bidHistoryHint, "Chưa có lượt đặt giá nào.");
            updateBidHistoryChart();
            renderTopWinners();
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
                    formatTimestamp(fields[2]),
                    parseDouble(fields[1]),
                    parseLong(fields[2])
            ));
        }

        setLabelText(bidHistoryHint, bidHistoryRows.isEmpty()
                ? "Chưa có lượt đặt giá nào."
                : "Đã tải " + bidHistoryRows.size() + " lượt đặt giá.");
        setLabelText(bidCountLabel, bidHistoryRows.size() + " lượt");
        updateBidHistoryChart();
        renderTopWinners();
    }

    private void updateBidHistoryChart() {
        if (bidHistoryChart == null) {
            return;
        }

        bidHistorySeries.getData().clear();
        bidHistoryRows.stream()
                .sorted((left, right) -> Long.compare(left.epochMillis(), right.epochMillis()))
                .forEach(row -> {
                    XYChart.Data<String, Number> point =
                            new XYChart.Data<>(formatChartTimestamp(row.epochMillis()), row.amountValue());
                    bidHistorySeries.getData().add(point);
                    installChartPointTooltip(point, row);
                });

        bidHistoryChart.setTitle(bidHistoryRows.isEmpty() ? "Chưa có dữ liệu đặt giá" : null);
    }

    private void installChartPointTooltip(XYChart.Data<String, Number> point, BidHistoryRow row) {
        Platform.runLater(() -> {
            if (point.getNode() == null) {
                return;
            }
            Tooltip tooltip = new Tooltip(
                    "Người đấu giá: " + row.username()
                            + "\nGiá: " + row.amount()
                            + "\nThời điểm: " + row.time()
            );
            Tooltip.install(point.getNode(), tooltip);
            point.getNode().setStyle("-fx-cursor: hand;");
        });
    }

    private void renderWinnerInfo(Map<String, String> params) {
        updateWinnerCard(params.getOrDefault("winner", "NONE"));
        String finalPriceText = formatMoney(params.getOrDefault("finalPrice", "0")) + " VNĐ";
        setLabelText(winnerPriceLabel, finalPriceText);
        setLabelText(finalPriceStatLabel, finalPriceText);
        setLabelText(resultHintLabel, "Kết quả cuối cùng đã được xác nhận từ server.");
    }

    private void renderAutoBidStatus(Map<String, String> params) {
        autoBidActive = Boolean.parseBoolean(params.getOrDefault("active", "false"));

        syncAutoBidToggle();

        if (autoBidActive) {
            String maxBid = params.getOrDefault("maxBid", "");
            String increment = params.getOrDefault("increment", "");
            if (autoBidMax != null && !maxBid.isBlank()) {
                autoBidMax.setText(maxBid);
            }
            if (autoBidIncrement != null && !increment.isBlank()) {
                autoBidIncrement.setText(increment);
            }
            setAutoBidStatus("Đã lưu auto-bid cho phiên này.");
            return;
        }

        setAutoBidStatus("Chưa bật auto-bid.");
    }

    private void renderAutoBidDisabled() {
        autoBidActive = false;
        syncAutoBidToggle();
        setAutoBidStatus("Đã tắt auto-bid.");
    }

    private void renderError(String[] parts, Map<String, String> params) {
        String message = params.get("message");
        if ((message == null || message.isBlank()) && parts.length > 1) {
            message = parts[1];
        }

        String safeMessage = message == null || message.isBlank() ? "Đã xảy ra lỗi" : message;
        if ("Auction is not available".equalsIgnoreCase(safeMessage)) {
            applyStatusState("FINISHED");
            setLabelText(starTime, "00:00:00");
            if (timeProgress != null) {
                timeProgress.setProgress(0);
            }
            getBidHistory();
            getWinnerInfo();
            getItem();
            return;
        }

        setLabelText(statusSubLabel, safeMessage);
        setAutoBidStatus(safeMessage);
        syncAutoBidToggle();
    }

    private void renderImage(String[] parts) {
        if (parts.length < 4 || StoreItemDataInit.image == null || !StoreItemDataInit.image.equals(parts[1])) {
            return;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(parts[3]);
            image.setImage(new Image(new ByteArrayInputStream(bytes)));
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

    private void updateCountdownConfig(Map<String, String> params, String durationValue) {
        try {
            if (params.containsKey("endTime")) {
                endTimeMillis = Long.parseLong(params.get("endTime"));
                setLabelText(finishedAtLabel, endTimeFormatter.format(Instant.ofEpochMilli(endTimeMillis)));
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
            applyStatusState("FINISHED");
            if (countdownTimeline != null) {
                countdownTimeline.stop();
            }
        }
    }

    private void applyStatusState(String statusValue) {
        currentAuctionStatus = statusValue == null || statusValue.isBlank() ? "UNKNOWN" : statusValue.toUpperCase();
        setLabelText(status, currentAuctionStatus);
        configureStatusChip(status, currentAuctionStatus);
        configureStatusChip(heroStatusChip, currentAuctionStatus);

        boolean isFinished = "FINISHED".equals(currentAuctionStatus)
                || "PAID".equals(currentAuctionStatus)
                || "CANCELED".equals(currentAuctionStatus);
        boolean isRunning = "RUNNING".equals(currentAuctionStatus);
        boolean isOpen = "OPEN".equals(currentAuctionStatus);
        boolean isPending = "PENDING".equals(currentAuctionStatus);

        modeLabel.setText(isFinished ? "Kết quả phiên đấu giá" : "Phiên đấu giá trực tuyến");
        statusSubLabel.setText(
                isFinished ? "Phiên đấu giá đã kết thúc"
                        : isRunning ? "Cập nhật theo thời gian thực"
                        : isOpen ? "Đang chờ đến giờ bắt đầu"
                        : "Đang đồng bộ trạng thái");

        heroStatusChip.setText(
                isFinished ? "ĐÃ KẾT THÚC"
                        : isRunning ? "SẢN PHẨM ĐANG ĐẤU GIÁ"
                        : isOpen ? "SẮP MỞ PHIÊN"
                        : "ĐANG CẬP NHẬT");

        setVisibleManaged(activeBidCard, !isFinished);
        setVisibleManaged(autoBidCard, !isFinished);
        setVisibleManaged(finishedResultCard, isFinished);
        setVisibleManaged(topWinnersBox, isFinished && !topWinnersBox.getChildren().isEmpty());

        if (raiseBidButton != null) {
            raiseBidButton.setDisable(!isRunning);
        }
        if (money != null) {
            money.setDisable(!isRunning);
        }
        if (autoBidToggle != null) {
            autoBidToggle.setDisable(!isRunning);
        }
        if (autoBidMax != null) {
            autoBidMax.setDisable(!isRunning);
        }
        if (autoBidIncrement != null) {
            autoBidIncrement.setDisable(!isRunning);
        }

        historyTitleLabel.setText(isFinished ? "Danh sách giá thắng" : "Lịch sử đặt giá");
        historyModeLabel.setText(isFinished ? "xếp hạng cuối phiên" : "theo server");
    }

    private void configureStatusChip(Label label, String statusValue) {
        if (label == null) {
            return;
        }
        label.getStyleClass().removeAll("status-running-chip", "status-open-chip", "status-finished-chip");
        if ("FINISHED".equalsIgnoreCase(statusValue)) {
            label.getStyleClass().add("status-finished-chip");
        } else if ("OPEN".equalsIgnoreCase(statusValue)) {
            label.getStyleClass().add("status-open-chip");
        } else {
            label.getStyleClass().add("status-running-chip");
        }
    }

    private void updateWinnerCard(String winner) {
        boolean noWinner = winner == null || winner.isBlank() || "NONE".equalsIgnoreCase(winner);
        String displayWinner = noWinner ? "Chưa có người thắng" : winner;
        setLabelText(winnerNameLabel, displayWinner);
        setLabelText(winnerAvatarLabel, noWinner ? "?" : displayWinner.substring(0, 1).toUpperCase());
        setLabelText(resultHintLabel, noWinner
                ? "Phiên đã kết thúc nhưng chưa có người đặt giá hợp lệ."
                : "Người dẫn đầu cuối cùng đã trở thành người chiến thắng.");
    }

    private void renderTopWinners() {
        topWinnersBox.getChildren().clear();

        if (!"FINISHED".equalsIgnoreCase(currentAuctionStatus) || bidHistoryRows.isEmpty()) {
            setVisibleManaged(topWinnersBox, false);
            return;
        }

        bidHistoryRows.stream()
                .sorted((left, right) -> {
                    int amountCompare = Double.compare(right.amountValue(), left.amountValue());
                    if (amountCompare != 0) {
                        return amountCompare;
                    }
                    return Long.compare(right.epochMillis(), left.epochMillis());
                })
                .limit(3)
                .forEachOrdered(row -> topWinnersBox.getChildren().add(createWinnerRow(topWinnersBox.getChildren().size() + 1, row)));

        setVisibleManaged(topWinnersBox, !topWinnersBox.getChildren().isEmpty());
    }

    private HBox createWinnerRow(int rank, BidHistoryRow row) {
        Label rankBadge = new Label(String.valueOf(rank));
        rankBadge.getStyleClass().add("history-rank-badge");
        rankBadge.getStyleClass().add(switch (rank) {
            case 1 -> "rank-gold";
            case 2 -> "rank-silver";
            default -> "rank-bronze";
        });

        Label avatar = new Label(row.username().substring(0, 1).toUpperCase());
        avatar.getStyleClass().add("small-avatar");
        if (rank == 1) {
            avatar.getStyleClass().add("gold-avatar");
        }

        Label user = new Label(row.username());
        user.getStyleClass().add("history-user");

        Label time = new Label(row.time());
        time.getStyleClass().add("muted-label");

        VBox userBox = new VBox(2.0, user, time);
        Label amount = new Label(row.amount());
        amount.getStyleClass().add("history-price");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox rowBox = new HBox(10.0, rankBadge, avatar, userBox, spacer, amount);
        rowBox.setAlignment(Pos.CENTER_LEFT);
        rowBox.setPadding(new Insets(10.0, 12.0, 10.0, 12.0));
        rowBox.getStyleClass().add("history-rank-row");
        return rowBox;
    }

    private void setVisibleManaged(VBox node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private String formatRemainingTime(long remainingMillis) {
        long totalSeconds = remainingMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String formatTimestamp(String raw) {
        try {
            return timeFormatter.format(Instant.ofEpochMilli(Long.parseLong(raw)));
        } catch (Exception e) {
            return raw;
        }
    }

    private String formatChartTimestamp(long epochMillis) {
        if (epochMillis <= 0) {
            return "--:--";
        }
        return chartTimeFormatter.format(Instant.ofEpochMilli(epochMillis));
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

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private void setLabelText(Label label, String value) {
        if (label != null) {
            label.setText(value == null || value.isBlank() ? "Chưa có dữ liệu" : value);
        }
    }

    private void setAutoBidStatus(String value) {
        setLabelText(autoBidStatus, value);
    }

    private void syncAutoBidToggle() {
        if (autoBidToggle == null) {
            return;
        }
        autoBidToggle.setSelected(autoBidActive);
        autoBidToggle.setText(autoBidActive ? "ON" : "OFF");
    }

    public record BidHistoryRow(String username, String amount, String time, double amountValue, long epochMillis) {
    }
}
