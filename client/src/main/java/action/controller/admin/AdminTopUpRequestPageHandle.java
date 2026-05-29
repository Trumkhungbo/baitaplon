package action.controller.admin;

import action.network.SocketClient;
import action.network.SocketListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.ResourceBundle;

public class AdminTopUpRequestPageHandle implements Initializable, SocketListener {
    @FXML private Label feedbackLabel;
    @FXML private TableView<TopUpRow> requestsTable;
    @FXML private TableColumn<TopUpRow, Number> sttColumn;
    @FXML private TableColumn<TopUpRow, String> usernameColumn;
    @FXML private TableColumn<TopUpRow, String> balanceColumn;
    @FXML private TableColumn<TopUpRow, String> amountColumn;
    @FXML private TableColumn<TopUpRow, String> emailColumn;
    @FXML private TableColumn<TopUpRow, TopUpRow> actionColumn;

    private final ObservableList<TopUpRow> rows = FXCollections.observableArrayList();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SocketClient.getInstance().addListener(this);
        setupTable();
        loadRequests();
    }

    private void setupTable() {
        sttColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(requestsTable.getItems().indexOf(param.getValue()) + 1));
        usernameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().username()));
        balanceColumn.setCellValueFactory(param -> new SimpleStringProperty(formatMoney(param.getValue().currentBalance()) + " VND"));
        amountColumn.setCellValueFactory(param -> new SimpleStringProperty(formatMoney(param.getValue().amount()) + " VND"));
        emailColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().email()));
        actionColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button approveButton = new Button("Đồng ý");

            {
                approveButton.setStyle("-fx-background-color: #FACC15; -fx-text-fill: black; -fx-font-weight: bold;");
                approveButton.setOnAction(event -> {
                    TopUpRow row = getItem();
                    if (row != null) {
                        approveRequest(row.id());
                    }
                });
            }

            @Override
            protected void updateItem(TopUpRow item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : approveButton);
            }
        });
        requestsTable.setItems(rows);
    }

    private void loadRequests() {
        JsonObject request = new JsonObject();
        request.addProperty("command", "ADMIN_LIST_TOPUP_REQUESTS");
        SocketClient.getInstance().requestData(request.toString());
    }

    private void approveRequest(long id) {
        JsonObject request = new JsonObject();
        request.addProperty("command", "ADMIN_APPROVE_TOPUP_REQUEST");
        request.addProperty("requestId", id);
        SocketClient.getInstance().requestData(request.toString());
        setFeedback("Đang duyệt yêu cầu nạp tiền...");
    }

    @Override
    public void onDataReceived(String data) {
        Platform.runLater(() -> {
            try {
                JsonObject response = JsonParser.parseString(data).getAsJsonObject();
                String command = getAsString(response, "command");
                switch (command) {
                    case "ADMIN_TOPUP_REQUESTS" -> renderRequests(response.getAsJsonArray("requests"));
                    case "ADMIN_TOPUP_APPROVE_RESULT" -> {
                        setFeedback(getAsString(response, "message"));
                        loadRequests();
                    }
                    case "ERROR" -> setFeedback(getAsString(response, "message"));
                    default -> {
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void renderRequests(JsonArray requests) {
        rows.clear();
        if (requests != null) {
            for (JsonElement element : requests) {
                JsonObject item = element.getAsJsonObject();
                rows.add(new TopUpRow(
                        getAsLong(item, "id"),
                        getAsString(item, "username"),
                        getAsDouble(item, "currentBalance"),
                        getAsDouble(item, "amount"),
                        getAsString(item, "email")
                ));
            }
        }
        setFeedback("Yêu cầu đang chờ: " + rows.size());
    }

    private String getAsString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    private long getAsLong(JsonObject object, String key) {
        try {
            return object.has(key) ? object.get(key).getAsLong() : 0L;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private double getAsDouble(JsonObject object, String key) {
        try {
            return object.has(key) ? object.get(key).getAsDouble() : 0.0;
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private String formatMoney(double value) {
        return moneyFormat.format(value);
    }

    private void setFeedback(String message) {
        feedbackLabel.setText(message == null ? "" : message);
    }

    private record TopUpRow(long id, String username, double currentBalance, double amount, String email) {
    }
}
