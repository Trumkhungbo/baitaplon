package action.Authentication;

import action.MainUI.LobbyHandle;
import action.SocketClient;
import action.SocketListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class PaymentBuyingStuffHandle implements Initializable, SocketListener {

    @FXML private Label paymentStatusLabel;
    @FXML private Label feedbackLabel;
    @FXML private Label itemNameLabel;
    @FXML private Label auctionCodeLabel;
    @FXML private Label sellerLabel;
    @FXML private Label auctionStatusLabel;
    @FXML private Label winnerLabel;
    @FXML private Label priceLabel;
    @FXML private Label balanceLabel;
    @FXML private Label balanceAfterLabel;
    @FXML private Button payButton;

    private final DecimalFormat moneyFormat = new DecimalFormat("#,###");
    private double currentPrice;
    private double currentBalance;
    private String currentStatus = "UNKNOWN";
    private String currentWinner = "NONE";
    private String currentSeller = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SocketClient.getInstance().addListener(this);
        itemNameLabel.setText(StoreItemDataInit.name == null ? "Sản phẩm" : StoreItemDataInit.name);
        auctionCodeLabel.setText("#AU" + StoreItemDataInit.description);
        requestAuctionDetail();
        requestAccountInfo();
        requestWinnerInfo();
        refreshPayState();
    }

    private void requestAuctionDetail() {
        SocketClient.getInstance().requestData("GET_AUCTION_DETAIL|" + StoreItemDataInit.description);
    }

    private void requestWinnerInfo() {
        SocketClient.getInstance().requestData("GET_WINNER|" + StoreItemDataInit.description);
    }

    private void requestAccountInfo() {
        JsonObject req = new JsonObject();
        req.addProperty("command", "GET_ACCOUNTINFORMATION");
        req.addProperty("username", StoreDataInput.getUsername());
        SocketClient.getInstance().requestData(req.toString());
    }

    @FXML
    public void payNow(ActionEvent event) {
        SocketClient.getInstance().requestData("PAY_AUCTION|" + StoreItemDataInit.description);
    }

    @FXML
    public void goBack(ActionEvent event) throws IOException {
        SocketClient.getInstance().removeListener(this);
        if (LobbyHandle.getInstance() != null) {
            LobbyHandle.getInstance().MovingCenter("/views/AccountInformation.fxml");
        }
    }

    @Override
    public void onDataReceived(String data) {
        Platform.runLater(() -> {
            if (data == null || data.isBlank()) {
                return;
            }

            if (data.startsWith("AUCTION_DETAIL|")) {
                renderAuctionDetail(parseParams(data.split("\\|")));
                return;
            }

            if (data.startsWith("WINNER_INFO|")) {
                Map<String, String> params = parseParams(data.split("\\|"));
                currentWinner = params.getOrDefault("winner", "NONE");
                winnerLabel.setText("Người thắng: " + currentWinner);
                refreshPayState();
                return;
            }

            if (data.startsWith("PAY_AUCTION_RESULT|")) {
                renderPaymentResult(parseParams(data.split("\\|")));
                return;
            }

            try {
                JsonObject json = JsonParser.parseString(data).getAsJsonObject();
                if (json.has("command") && "ACCOUNT_INFO".equals(json.get("command").getAsString())) {
                    currentBalance = parseDouble(json.get("balance").getAsString());
                    balanceLabel.setText(formatMoney(currentBalance) + " VND");
                    balanceAfterLabel.setText(formatMoney(currentBalance - currentPrice) + " VND");
                    refreshPayState();
                }
            } catch (Exception ignored) {
                if (data.startsWith("ERROR|")) {
                    setFeedback(data.substring("ERROR|".length()));
                }
            }
        });
    }

    private void renderAuctionDetail(Map<String, String> params) {
        currentPrice = parseDouble(params.getOrDefault("currentPrice", "0"));
        currentStatus = params.getOrDefault("status", "UNKNOWN");
        currentSeller = params.getOrDefault("seller", "");

        itemNameLabel.setText(params.getOrDefault("itemName", StoreItemDataInit.name));
        auctionCodeLabel.setText("#AU" + params.getOrDefault("id", StoreItemDataInit.description));
        sellerLabel.setText("Người bán: " + currentSeller);
        auctionStatusLabel.setText("Trạng thái phiên: " + currentStatus);
        priceLabel.setText(formatMoney(currentPrice) + " VND");
        balanceAfterLabel.setText(formatMoney(currentBalance - currentPrice) + " VND");
        refreshPayState();
    }

    private void renderPaymentResult(Map<String, String> params) {
        String status = params.getOrDefault("status", "FAILED");
        String message = params.getOrDefault("message", "");
        if ("SUCCESS".equalsIgnoreCase(status)) {
            currentStatus = params.getOrDefault("newStatus", "PAID");
            paymentStatusLabel.setText("ĐÃ THANH TOÁN");
            paymentStatusLabel.setStyle("-fx-background-color: rgba(34,197,94,0.18); -fx-text-fill: #86EFAC; -fx-background-radius: 10; -fx-padding: 8 12; -fx-font-weight: bold;");
            currentBalance = parseDouble(params.getOrDefault("buyerBalance", "0"));
            balanceLabel.setText(formatMoney(currentBalance) + " VND");
            balanceAfterLabel.setText(formatMoney(currentBalance) + " VND");
            setFeedback(message);
        } else {
            if ("CANCELED".equalsIgnoreCase(params.getOrDefault("newStatus", ""))) {
                currentStatus = "CANCELED";
                paymentStatusLabel.setText("ĐÃ HỦY");
            }
            setFeedback(message);
        }
        refreshPayState();
    }

    private void refreshPayState() {
        boolean isWinner = StoreDataInput.getUsername() != null && StoreDataInput.getUsername().equalsIgnoreCase(currentWinner);
        boolean canPay = isWinner && "FINISHED".equalsIgnoreCase(currentStatus) && currentBalance >= currentPrice;

        if ("PAID".equalsIgnoreCase(currentStatus)) {
            paymentStatusLabel.setText("ĐÃ THANH TOÁN");
            paymentStatusLabel.setStyle("-fx-background-color: rgba(34,197,94,0.18); -fx-text-fill: #86EFAC; -fx-background-radius: 10; -fx-padding: 8 12; -fx-font-weight: bold;");
        } else if ("CANCELED".equalsIgnoreCase(currentStatus)) {
            paymentStatusLabel.setText("ĐÃ HỦY");
            paymentStatusLabel.setStyle("-fx-background-color: rgba(239,68,68,0.18); -fx-text-fill: #FCA5A5; -fx-background-radius: 10; -fx-padding: 8 12; -fx-font-weight: bold;");
        } else {
            paymentStatusLabel.setText("CHỜ THANH TOÁN");
            paymentStatusLabel.setStyle("-fx-background-color: rgba(239,68,68,0.18); -fx-text-fill: #FCA5A5; -fx-background-radius: 10; -fx-padding: 8 12; -fx-font-weight: bold;");
        }

        if (payButton != null) {
            payButton.setDisable(!canPay);
        }

        if (!isWinner && !"NONE".equalsIgnoreCase(currentWinner)) {
            setFeedback("Chỉ bidder thắng mới được thanh toán.");
        } else if ("FINISHED".equalsIgnoreCase(currentStatus) && currentBalance < currentPrice) {
            setFeedback("Số dư hiện tại không đủ để thanh toán.");
        }
    }

    private Map<String, String> parseParams(String[] parts) {
        Map<String, String> params = new HashMap<>();
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].contains("=")) {
                continue;
            }
            String[] keyValue = parts[i].split("=", 2);
            params.put(keyValue[0], keyValue[1]);
        }
        return params;
    }

    private double parseDouble(String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private String formatMoney(double value) {
        return moneyFormat.format(value);
    }

    private void setFeedback(String value) {
        feedbackLabel.setText(value == null ? "" : value);
    }
}
