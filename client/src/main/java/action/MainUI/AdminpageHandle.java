package action.MainUI;

import java.net.URL;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.ResourceBundle;

import action.Core.StartScence;
import action.SellingJobs.AuctionItems;
import action.SellingJobs.ItemsHolder;
import action.SellingJobs.ShopDataBase;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.application.Platform;

public class AdminpageHandle implements Initializable {

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

        // Đăng ký nghe sự kiện cập nhật dữ liệu (để vẽ bảng)
        AuctionItems.currentListener = () -> {
            renderAuctions();
        };

        // Yêu cầu tải dữ liệu từ Server
        AuctionItems.requestData();
    }

    public void Confirm(ActionEvent event) {
        for (ItemsHolder item : ItemsTable.getItems()) {
            if (item.getCheckBox().isSelected()){
                // Gửi lệnh lên Server để duyệt sản phẩm thành OPEN
                StartScence.client.sendMessage("UPDATE_STATUS|" + item.getItemId()+"|OPEN" );
            }
        }

        // Sau khi Confirm, có thể yêu cầu tải lại dữ liệu cho chắc chắn
        AuctionItems.requestData();
    }

    private void renderAuctions() {
        ItemsTable.getItems().clear(); // Xóa dữ liệu cũ

        for (List<Object> item : AuctionItems.list) {
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
}