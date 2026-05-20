package action.MainUI;

import action.SocketClient;
import action.SocketListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminAccountPageHandle implements Initializable, SocketListener {
    @FXML
    private TableView<UserHolder> table;
    @FXML
    private TableColumn<UserHolder,String> UserName;
    @FXML
    private TableColumn<UserHolder,String> UserBalance;
    @FXML
    private TableColumn<UserHolder,String> UserGmail;
    @FXML
    private TableColumn<UserHolder, Button> Button;
    @FXML
    private TableColumn<UserHolder,String> STTColumn;
    @FXML
    private TableColumn<UserHolder,String> UserID;
    @FXML
    private TableColumn<UserHolder,String> UserSDT;
    @Override
    public void initialize(URL location, ResourceBundle resource) {
        UserName.setCellValueFactory(new PropertyValueFactory<>("name"));
        UserBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
        UserGmail.setCellValueFactory(new PropertyValueFactory<>("gmail"));
        UserID.setCellValueFactory(new PropertyValueFactory<>("ID"));
        UserSDT.setCellValueFactory(new PropertyValueFactory<>("SDT"));
        Button.setCellValueFactory(new PropertyValueFactory<>("button"));

        STTColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.valueOf(getTableRow().getIndex() + 1));
                }
            }
        });
        STTColumn.setStyle("-fx-alignment: CENTER;");

        SocketClient.getInstance().addListener(this);
        UserHolder dummy1 = new UserHolder("Nguyễn Văn A", "500.000", "a@gmail.com","123123123123", "0123456789");
        UserHolder dummy2 = new UserHolder("Trần Thị B", "1.200.000", "b@gmail.com","363636363636", "0987654321");

        // Đưa dữ liệu vào danh sách
        javafx.collections.ObservableList<UserHolder> listData = javafx.collections.FXCollections.observableArrayList(dummy1, dummy2);

        // Nạp danh sách vào bảng
        table.setItems(listData);
    }
    @Override
    public void onDataReceived(String data) {

    }
}
