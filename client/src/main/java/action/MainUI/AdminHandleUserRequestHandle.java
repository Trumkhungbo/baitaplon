package action.MainUI;

import action.SocketClient;
import action.SocketListener;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class AdminHandleUserRequestHandle implements Initializable, SocketListener {
    @FXML
    private TableView<UserRequestHolder> table;
    @FXML
    private TableColumn<UserRequestHolder,String> UserName;
    @FXML
    private TableColumn<UserRequestHolder,String> UserBalance;
    @FXML
    private TableColumn<UserRequestHolder,String> UserAddingMoney;
    @FXML
    private TableColumn<UserRequestHolder, Button> Button;
    @FXML
    private TableColumn<UserRequestHolder,String> STTColumn;
    @Override
    public void initialize(URL location, ResourceBundle resource) {
        UserName.setCellValueFactory(new PropertyValueFactory<>("name"));
        UserBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
        UserAddingMoney.setCellValueFactory(new PropertyValueFactory<>("addingMoneyAmount"));
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
        UserRequestHolder dummy1 = new UserRequestHolder("Nguyễn Văn A", "500.000", "a@gmail.com");
        UserRequestHolder dummy2 = new UserRequestHolder( "Trần Thị B", "1.200.000", "b@gmail.com");

        // Đưa dữ liệu vào danh sách
        javafx.collections.ObservableList<UserRequestHolder> listData = javafx.collections.FXCollections.observableArrayList(dummy1, dummy2);

        // Nạp danh sách vào bảng
        table.setItems(listData);
    }

    @Override
    public void onDataReceived(String data) {

    }
}
