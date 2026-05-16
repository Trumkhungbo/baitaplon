package action.MainUI;

import java.io.IOException;
import java.net.URL;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import action.Core.SceneSwitch;
import action.SocketClient;
import action.SocketListener;
import action.SellingJobs.ItemsHolder;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.application.Platform;

public class AdminpageHandle implements Initializable, SocketListener {

    @FXML
    private TableView<ItemsHolder> ItemsTable;
    @FXML
    private TableColumn<ItemsHolder, String> ProductName;
    @FXML
    private TableColumn<ItemsHolder, String> Productinfomation1;
    @FXML
    private TableColumn<ItemsHolder, String> Productinfomation2;
    @FXML
    private TableColumn<ItemsHolder, String> ProductPrice;
    @FXML
    private TableColumn<ItemsHolder, LocalDate> ProductDate;
    @FXML
    private TableColumn<ItemsHolder, LocalTime> ProductTime;
    @FXML
    private TableColumn<ItemsHolder, Time> SellingTime;
    @FXML
    private TableColumn<ItemsHolder, String> ProductDescription;
    @FXML
    private TableColumn<ItemsHolder, CheckBox> CheckBox;
    @FXML
    private TableColumn<ItemsHolder, String> STTColumn;

    private List<List<Object>> list = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resource) {
        // Ánh xạ các cột trong bảng với thuộc tính của ItemsHolder
        ProductName.setCellValueFactory(new PropertyValueFactory<>("itemname"));
        ProductPrice.setCellValueFactory(new PropertyValueFactory<>("itemprice"));
        ProductDate.setCellValueFactory(new PropertyValueFactory<>("itemdate"));
        ProductTime.setCellValueFactory(new PropertyValueFactory<>("itemtime"));
        SellingTime.setCellValueFactory(new PropertyValueFactory<>("itemduration"));
        CheckBox.setCellValueFactory(new PropertyValueFactory<>("checkBox"));

        STTColumn.setCellFactory(column -> new TableCell<ItemsHolder, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    int index = getTableRow().getIndex();
                    setText(String.valueOf(index + 1));
                }
            }
        });
        STTColumn.setStyle("-fx-alignment: CENTER;");

        SocketClient.getInstance().addListener(this);
        SocketClient.getInstance().requestData("LIST_AUCTIONS");
    }

    public void Confirm(ActionEvent event) {
        for (ItemsHolder item : ItemsTable.getItems()) {
            if (item.getCheckBox().isSelected()){
                SocketClient.getInstance().requestData("UPDATE_STATUS|" + item.getItemId()+"|OPEN" );
            } else {
                SocketClient.getInstance().requestData("UPDATE_STATUS|" + item.getItemId()+"|CANCELED" );
            }
        }
        SocketClient.getInstance().requestData("LIST_AUCTIONS");
    }

    private void renderAuctions() {
        ItemsTable.getItems().clear(); // Xóa dữ liệu cũ

        for (List<Object> item : list) {
            String status = (String) item.get(3);

            // Trang Admin chỉ hiển thị các sản phẩm chờ duyệt (PENDING)
            if ("PENDING".equals(status)) {

                String itemid = (String) item.get(1);
                String itemname = (String) item.get(0);

                Double currentPrice = (Double) item.get(2);

                ItemsHolder newProduct = new ItemsHolder(
                        itemid,
                        itemname,
                        currentPrice,
                        LocalTime.of(10, 10, 10),       // Dummy time
                        Time.valueOf(LocalTime.of(10, 10, 10)) // Dummy duration
                );

                ItemsTable.getItems().add(newProduct);
            }
        }
    }

    @Override
    public void onDataReceived(String data) {
        System.out.println("AdminpageHandle received: " + data); // DEBUG
        if (data != null && data.startsWith("AUCTION_LIST|")) {
            String dataPart = data.substring("AUCTION_LIST|".length());
            System.out.println("AdminpageHandle dataPart: " + dataPart); // DEBUG

            Platform.runLater(() -> {
                list.clear(); // Xóa cũ đi
                if (!dataPart.isEmpty()) {
                    String[] items = dataPart.split(";");
                    for (String itemData : items) {
                        System.out.println("AdminpageHandle itemData: " + itemData); // DEBUG
                        String[] attributes = itemData.split(":");
                        if (attributes.length ==4) {
                            String id = attributes[0];
                            String itemName = attributes[1];
                            Double currentPrice = Double.parseDouble(attributes[2]);
                            String status = attributes[3];
                            // Lưu theo đúng thứ tự
                            list.add(List.of(itemName, id, currentPrice, status));
                        }
                    }
                }
                renderAuctions();
            });
        }
    }
    public void LogOut(ActionEvent event) throws IOException {
        SceneSwitch sceneSwitch = new SceneSwitch();
        sceneSwitch.SwitchToLogin(event);
    }
}