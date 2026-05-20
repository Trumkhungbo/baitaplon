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
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.ResourceBundle;

public class AdminAccountPageHandle implements Initializable, SocketListener {
    @FXML private Label feedbackLabel;
    @FXML private TableView<UserRow> usersTable;
    @FXML private TableColumn<UserRow, Number> sttColumn;
    @FXML private TableColumn<UserRow, String> usernameColumn;
    @FXML private TableColumn<UserRow, String> balanceColumn;
    @FXML private TableColumn<UserRow, String> emailColumn;
    @FXML private TableColumn<UserRow, String> idColumn;
    @FXML private TableColumn<UserRow, String> phoneColumn;
    @FXML private TableColumn<UserRow, String> personalIdColumn;
    @FXML private TableColumn<UserRow, String> roleColumn;
    @FXML private TableColumn<UserRow, UserRow> actionColumn;

    private final ObservableList<UserRow> rows = FXCollections.observableArrayList();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SocketClient.getInstance().addListener(this);
        setupTable();
        loadUsers();
    }

    private void setupTable() {
        sttColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(usersTable.getItems().indexOf(param.getValue()) + 1));
        usernameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().username()));
        balanceColumn.setCellValueFactory(param -> new SimpleStringProperty(formatMoney(param.getValue().balance()) + " VND"));
        emailColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().email()));
        idColumn.setCellValueFactory(param -> new SimpleStringProperty(String.valueOf(param.getValue().id())));
        phoneColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().phone()));
        personalIdColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().personalId()));
        roleColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().role()));
        actionColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button deleteButton = new Button("Xóa tài khoản");

            {
                deleteButton.setStyle("-fx-background-color: #7F1D1D; -fx-text-fill: white; -fx-font-weight: bold;");
                deleteButton.setOnAction(event -> {
                    UserRow row = getItem();
                    if (row != null) {
                        deleteUser(row.id());
                    }
                });
            }

            @Override
            protected void updateItem(UserRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || "ADMIN".equalsIgnoreCase(item.role())) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteButton);
                }
            }
        });
        usersTable.setItems(rows);
    }

    private void loadUsers() {
        JsonObject request = new JsonObject();
        request.addProperty("command", "ADMIN_LIST_USERS");
        SocketClient.getInstance().requestData(request.toString());
    }

    private void deleteUser(long id) {
        JsonObject request = new JsonObject();
        request.addProperty("command", "ADMIN_DELETE_USER");
        request.addProperty("userId", id);
        SocketClient.getInstance().requestData(request.toString());
        setFeedback("Đang xóa tài khoản...");
    }

    @Override
    public void onDataReceived(String data) {
        Platform.runLater(() -> {
            try {
                JsonObject response = JsonParser.parseString(data).getAsJsonObject();
                String command = getAsString(response, "command");
                switch (command) {
                    case "ADMIN_USERS" -> renderUsers(response.getAsJsonArray("users"));
                    case "ADMIN_USER_DELETE_RESULT" -> {
                        setFeedback(getAsString(response, "message"));
                        loadUsers();
                    }
                    case "ERROR" -> setFeedback(getAsString(response, "message"));
                    default -> {
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void renderUsers(JsonArray users) {
        rows.clear();
        if (users == null) {
            return;
        }
        for (JsonElement element : users) {
            JsonObject item = element.getAsJsonObject();
            rows.add(new UserRow(
                    getAsLong(item, "id"),
                    getAsString(item, "username"),
                    getAsDouble(item, "balance"),
                    getAsString(item, "email"),
                    getAsString(item, "phone"),
                    getAsString(item, "personalID"),
                    getAsString(item, "role")
            ));
        }
        setFeedback("Tổng tài khoản: " + rows.size());
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

    private record UserRow(long id, String username, double balance, String email, String phone, String personalId, String role) {
    }
}
