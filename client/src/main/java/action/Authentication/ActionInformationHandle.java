package action.Authentication;

import action.Core.SceneSwitch;
import action.MainUI.LobbyHandle;
import action.SocketClient;
import action.SocketListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.Locale;
import java.util.ResourceBundle;

public class ActionInformationHandle implements Initializable, SocketListener {

    @FXML private Label personalID;
    @FXML private Label name;
    @FXML private Label email;
    @FXML private Label phone;
    @FXML private Label password;
    @FXML private Label money;
    @FXML private TextField moneyIn;
    @FXML private Label feedbackLabel;
    @FXML private Label allStatusLabel;
    @FXML private Label pendingStatusLabel;
    @FXML private Label openStatusLabel;
    @FXML private Label runningStatusLabel;
    @FXML private Label finishedStatusLabel;

    @FXML private TableView<AccountAuctionRow> ItemsTable;
    @FXML private TableColumn<AccountAuctionRow, Number> sttColumn;
    @FXML private TableColumn<AccountAuctionRow, String> productNameColumn;
    @FXML private TableColumn<AccountAuctionRow, String> auctionCodeColumn;
    @FXML private TableColumn<AccountAuctionRow, String> roleColumn;
    @FXML private TableColumn<AccountAuctionRow, String> moneyColumn;
    @FXML private TableColumn<AccountAuctionRow, String> statusColumn;
    @FXML private TableColumn<AccountAuctionRow, String> resultColumn;
    @FXML private TableColumn<AccountAuctionRow, String> timeColumn;
    @FXML private TableColumn<AccountAuctionRow, AccountAuctionRow> actionColumn;

    private final ObservableList<AccountAuctionRow> rows = FXCollections.observableArrayList();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,###");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SocketClient.getInstance().addListener(this);
        setupTable();
        loadAccountInfo();
        loadMyAuctions();
    }

    private void setupTable() {
        sttColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(ItemsTable.getItems().indexOf(param.getValue()) + 1));
        productNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().itemName()));
        auctionCodeColumn.setCellValueFactory(param -> new SimpleStringProperty("#AU" + param.getValue().auctionId()));
        roleColumn.setCellValueFactory(param -> new SimpleStringProperty(normalizeRole(param.getValue().role())));
        moneyColumn.setCellValueFactory(param -> new SimpleStringProperty(formatMoney(param.getValue().currentPrice()) + " VND"));
        statusColumn.setCellValueFactory(param -> new SimpleStringProperty(normalizeStatus(param.getValue().status())));
        resultColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().result()));
        timeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().startDate() + " " + param.getValue().startClockTime()));

        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                String style = switch (item) {
                    case "Dang dau gia" -> "-fx-text-fill: #4ADE80; -fx-font-weight: bold;";
                    case "Sap dien ra" -> "-fx-text-fill: #60A5FA; -fx-font-weight: bold;";
                    case "Cho duyet" -> "-fx-text-fill: #FACC15; -fx-font-weight: bold;";
                    case "Da thanh toan" -> "-fx-text-fill: #22C55E; -fx-font-weight: bold;";
                    case "Da huy" -> "-fx-text-fill: #F87171; -fx-font-weight: bold;";
                    default -> "-fx-text-fill: #CBD5E1; -fx-font-weight: bold;";
                };
                setStyle(style);
            }
        });

        resultColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                String normalized = item.toLowerCase(Locale.ROOT);
                String style = switch (normalized) {
                    case "winner" -> "-fx-text-fill: #FACC15; -fx-font-weight: bold;";
                    case "lost", "canceled" -> "-fx-text-fill: #F87171; -fx-font-weight: bold;";
                    default -> "-fx-text-fill: #93C5FD; -fx-font-weight: bold;";
                };
                setStyle(style);
            }
        });

        actionColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button payButton = new Button("Thanh toan");

            {
                payButton.getStyleClass().add("btn-gold");
                payButton.setOnAction(event -> {
                    AccountAuctionRow row = getItem();
                    if (row == null) {
                        return;
                    }
                    event.consume();
                    openPayment(row);
                });
            }

            @Override
            protected void updateItem(AccountAuctionRow item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(!empty && item != null && item.canPay() ? payButton : null);
            }
        });

        ItemsTable.setItems(rows);
        ItemsTable.setRowFactory(table -> {
            TableRow<AccountAuctionRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (row.isEmpty()) {
                    return;
                }
                if ("CANCELED".equalsIgnoreCase(row.getItem().status())) {
                    setFeedback("Phien dau gia nay da bi huy.");
                    return;
                }
                openAuctionRoom(row.getItem());
            });
            return row;
        });
    }

    private void loadAccountInfo() {
        JsonObject req = new JsonObject();
        req.addProperty("command", "GET_ACCOUNTINFORMATION");
        req.addProperty("username", StoreDataInput.username);
        SocketClient.getInstance().requestData(req.toString());
    }

    private void loadMyAuctions() {
        JsonObject req = new JsonObject();
        req.addProperty("command", "LIST_ACCOUNT_AUCTIONS");
        req.addProperty("username", StoreDataInput.username);
        SocketClient.getInstance().requestData(req.toString());
    }

    public void addMoney(ActionEvent event) throws IOException {
        String input = moneyIn.getText() == null ? "" : moneyIn.getText().trim().replace(".", "").replace(",", "");
        if (input.isEmpty()) {
            new SceneSwitch().SwitchToLockPage(event, "/views/SomeThingUnFill.fxml");
            return;
        }
        if (!input.matches("\\d+")) {
            new SceneSwitch().SwitchToLockPage(event, "/views/WrongInputShow.fxml");
            return;
        }

        JsonObject addReq = new JsonObject();
        addReq.addProperty("command", "ADD_MONEY");
        addReq.addProperty("username", StoreDataInput.username);
        addReq.addProperty("money", input);
        SocketClient.getInstance().requestData(addReq.toString());
        setFeedback("Dang gui yeu cau nap tien...");
    }

    @FXML
    public void fillQuickAmount500K() {
        moneyIn.setText("500000");
    }

    @FXML
    public void fillQuickAmount1M() {
        moneyIn.setText("1000000");
    }

    @FXML
    public void fillQuickAmount5M() {
        moneyIn.setText("5000000");
    }

    @FXML
    public void fillQuickAmount10M() {
        moneyIn.setText("10000000");
    }

    public void ReturnToLogin(ActionEvent event) throws IOException {
        new SceneSwitch().SwitchToLogin(event);
    }

    @Override
    public void onDataReceived(String data) {
        Platform.runLater(() -> {
            try {
                JsonObject res = JsonParser.parseString(data).getAsJsonObject();
                if (!res.has("command")) {
                    return;
                }

                String command = res.get("command").getAsString();
                switch (command) {
                    case "ACCOUNT_INFO" -> {
                        name.setText(getAsString(res, "username"));
                        password.setText(maskPassword(getAsString(res, "password")));
                        phone.setText(getAsString(res, "phone"));
                        email.setText(getAsString(res, "email"));
                        personalID.setText(getAsString(res, "personalID"));
                        money.setText(formatMoneyText(getAsString(res, "balance")));
                    }
                    case "MONEY_UPDATE" -> {
                        money.setText(formatMoneyText(getAsString(res, "balance")));
                        moneyIn.clear();
                        setFeedback("Nap tien thanh cong.");
                        loadAccountInfo();
                    }
                    case "TOPUP_REQUEST_CREATED" -> {
                        moneyIn.clear();
                        setFeedback(getAsString(res, "message"));
                    }
                    case "ERROR" -> setFeedback(getAsString(res, "message"));
                    default -> {
                    }
                }
            } catch (Exception ignored) {
                if (data.startsWith("ACCOUNT_AUCTIONS|")) {
                    parseMyAuctions(data.substring("ACCOUNT_AUCTIONS|".length()));
                } else if (data.startsWith("MY_AUCTIONS|")) {
                    parseMyAuctions(data.substring("MY_AUCTIONS|".length()));
                } else if (data.startsWith("ERROR|")) {
                    setFeedback(data.substring("ERROR|".length()));
                }
            }
        });
    }

    private void parseMyAuctions(String payload) {
        rows.clear();
        if (!payload.isBlank()) {
            String[] recordRows = payload.split(";");
            for (String row : recordRows) {
                String[] attr = row.split(":", -1);
                if (attr.length < 18) {
                    continue;
                }
                rows.add(new AccountAuctionRow(
                        attr[0],
                        attr[2],
                        attr[3],
                        parseDouble(attr[5]),
                        attr[6],
                        attr[7],
                        attr[8],
                        attr[11],
                        attr[13],
                        attr[14],
                        attr.length > 17 ? attr[17] : "Bidder",
                        attr.length > 18 ? attr[18] : "",
                        attr.length > 19 && Boolean.parseBoolean(attr[19])
                ));
            }
        }

        rows.sort(Comparator.comparingInt((AccountAuctionRow item) -> parseInt(item.auctionId())).reversed());
        updateStatusSummary();
    }

    private void updateStatusSummary() {
        long pending = rows.stream().filter(row -> "PENDING".equalsIgnoreCase(row.status())).count();
        long open = rows.stream().filter(row -> "OPEN".equalsIgnoreCase(row.status())).count();
        long running = rows.stream().filter(row -> "RUNNING".equalsIgnoreCase(row.status())).count();
        long finished = rows.stream().filter(row ->
                "FINISHED".equalsIgnoreCase(row.status())
                        || "PAID".equalsIgnoreCase(row.status())
                        || "CANCELED".equalsIgnoreCase(row.status())).count();

        allStatusLabel.setText("Tat ca: " + rows.size());
        pendingStatusLabel.setText("Cho duyet: " + pending);
        openStatusLabel.setText("Sap dien ra: " + open);
        runningStatusLabel.setText("Dang dau gia: " + running);
        finishedStatusLabel.setText("Da ket thuc: " + finished);
    }

    private void openAuctionRoom(AccountAuctionRow row) {
        StoreItemDataInit.name = row.itemName();
        StoreItemDataInit.description = row.auctionId();
        StoreItemDataInit.price = formatMoney(row.currentPrice());
        StoreItemDataInit.status = row.status();
        StoreItemDataInit.image = row.imageUrl();
        StoreItemDataInit.itemInformation1 = row.information1();
        StoreItemDataInit.itemInformation2 = row.information2();
        StoreItemDataInit.itemType = row.itemType();

        try {
            if (LobbyHandle.getInstance() != null) {
                LobbyHandle.getInstance().ItemShowing();
            }
        } catch (IOException e) {
            setFeedback("Khong the mo phong dau gia cua san pham.");
        }
    }

    private void openPayment(AccountAuctionRow row) {
        StoreItemDataInit.name = row.itemName();
        StoreItemDataInit.description = row.auctionId();
        StoreItemDataInit.price = formatMoney(row.currentPrice());
        StoreItemDataInit.status = row.status();
        StoreItemDataInit.image = row.imageUrl();
        StoreItemDataInit.itemInformation1 = row.information1();
        StoreItemDataInit.itemInformation2 = row.information2();
        StoreItemDataInit.itemType = row.itemType();

        try {
            if (LobbyHandle.getInstance() != null) {
                LobbyHandle.getInstance().MovingCenter("/views/Payment_BuyingStuff.fxml");
            }
        } catch (IOException e) {
            setFeedback("Khong the mo trang thanh toan.");
        }
    }

    private String normalizeStatus(String status) {
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "PENDING" -> "Cho duyet";
            case "OPEN" -> "Sap dien ra";
            case "RUNNING" -> "Dang dau gia";
            case "PAID" -> "Da thanh toan";
            case "CANCELED" -> "Da huy";
            default -> "Da ket thuc";
        };
    }

    private String normalizeRole(String role) {
        return "Bidder".equalsIgnoreCase(role) ? "Nguoi dat" : "Nguoi ban";
    }

    private String getAsString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private String maskPassword(String raw) {
        return "********";
    }

    private String formatMoney(double value) {
        return moneyFormat.format(value);
    }

    private String formatMoneyText(String raw) {
        String normalized = normalizeNumber(raw);
        if (normalized.isEmpty()) {
            return "0";
        }
        try {
            DecimalFormat formatter = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));
            return formatter.format(new BigDecimal(normalized));
        } catch (NumberFormatException ignored) {
            return raw == null || raw.isBlank() ? "0" : raw.trim();
        }
    }

    private String normalizeNumber(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim().replace(" ", "");
        if (value.contains(",") && value.contains(".")) {
            return value.replace(".", "").replace(",", ".");
        }
        if (value.contains(",")) {
            return value.replace(",", ".");
        }
        return value;
    }

    private double parseDouble(String raw) {
        try {
            return Double.parseDouble(normalizeNumber(raw));
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private int parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void setFeedback(String message) {
        feedbackLabel.setText(message == null ? "" : message);
    }

    private record AccountAuctionRow(
            String auctionId,
            String itemName,
            String itemType,
            double currentPrice,
            String status,
            String startDate,
            String startClockTime,
            String imageUrl,
            String information1,
            String information2,
            String role,
            String result,
            boolean canPay
    ) {
    }
}
