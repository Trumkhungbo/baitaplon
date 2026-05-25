package action.MainUI;

import action.SocketClient;
import action.SocketListener;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminHandleUserRequestHandle implements Initializable, SocketListener {
    @FXML private TableView<TopUpRequestRow> table;
    @FXML private TableColumn<TopUpRequestRow, String> STTColumn;
    @FXML private TableColumn<TopUpRequestRow, String> UserName;
    @FXML private TableColumn<TopUpRequestRow, String> UserBalance;
    @FXML private TableColumn<TopUpRequestRow, String> UserAddingMoney;
    @FXML private TableColumn<TopUpRequestRow, TopUpRequestRow> Button;

    private final ObservableList<TopUpRequestRow> rows = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resource) {
        SocketClient.getInstance().addListener(this);
        setupTable();
        loadRequests();
    }

    private void setupTable() {
        STTColumn.setCellValueFactory(param -> new SimpleStringProperty(String.valueOf(table.getItems().indexOf(param.getValue()) + 1)));
        STTColumn.setStyle("-fx-alignment: CENTER;");

        UserName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().username()));
        UserBalance.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().currentBalance() + " VND"));
        UserAddingMoney.setCellValueFactory(param -> new SimpleStringProperty("+" + param.getValue().amount() + " VND"));

        // Tạo nút bấm Duyệt nạp tiền trực tiếp trong bảng
        Button.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        Button.setCellFactory(column -> new TableCell<>() {
            private final javafx.scene.control.Button approveBtn = new javafx.scene.control.Button("Duyệt ngay");
            {
                approveBtn.setStyle("-fx-background-color: #22C55E; -fx-text-fill: white; -fx-font-weight: bold;");
                approveBtn.setOnAction(event -> {
                    TopUpRequestRow row = getItem();
                    if (row != null) {
                        approveRequest(row.id());
                    }
                });
            }
            @Override
            protected void updateItem(TopUpRequestRow item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : approveBtn);
            }
        });

        table.setItems(rows);
    }

    private void loadRequests() {
        // Gửi JSON yêu cầu lấy danh sách chờ nạp tiền
        JsonObject req = new JsonObject();
        req.addProperty("command", "ADMIN_LIST_TOPUP_REQUESTS");
        SocketClient.getInstance().requestData(req.toString());
    }

    private void approveRequest(long id) {
        // Gửi JSON lệnh duyệt tiền cho 1 user
        JsonObject req = new JsonObject();
        req.addProperty("command", "ADMIN_APPROVE_TOPUP_REQUEST");
        req.addProperty("requestId", id);
        SocketClient.getInstance().requestData(req.toString());
    }

    @Override
    public void onDataReceived(String data) {
        Platform.runLater(() -> {
            try {
                JsonObject res = JsonParser.parseString(data).getAsJsonObject();
                if (!res.has("command")) return;

                String command = res.get("command").getAsString();

                // Đón JSON chứa danh sách mảng các yêu cầu nạp tiền
                if ("ADMIN_TOPUP_REQUESTS".equals(command)) {
                    rows.clear();
                    JsonArray requests = res.getAsJsonArray("requests");
                    if (requests != null) {
                        for (JsonElement elem : requests) {
                            JsonObject obj = elem.getAsJsonObject();
                            rows.add(new TopUpRequestRow(
                                    obj.get("id").getAsLong(),
                                    obj.get("username").getAsString(),
                                    obj.get("currentBalance").getAsString(),
                                    obj.get("amount").getAsString()
                            ));
                        }
                    }
                }
                else if ("ADMIN_TOPUP_APPROVE_RESULT".equals(command)) {
                    loadRequests();
                }
            } catch (Exception e) {
                System.err.println("[ADMIN_TOPUP] Lỗi đọc JSON: " + data);
            }
        });
    }

    // Record nội bộ tối ưu bộ nhớ
    private record TopUpRequestRow(long id, String username, String currentBalance, String amount) {}
}