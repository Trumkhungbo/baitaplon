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
        Productinfomation1.setCellValueFactory(new PropertyValueFactory<>("iteminformation1"));
        Productinfomation2.setCellValueFactory(new PropertyValueFactory<>("iteminformation2"));
        ProductPrice.setCellValueFactory(new PropertyValueFactory<>("itemprice"));
        ProductDate.setCellValueFactory(new PropertyValueFactory<>("itemdate"));
        ProductTime.setCellValueFactory(new PropertyValueFactory<>("itemtime"));
        SellingTime.setCellValueFactory(new PropertyValueFactory<>("itemduration"));
        ProductDescription.setCellValueFactory(new PropertyValueFactory<>("iteminfomation"));
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
                StartScence.client.sendMessage("UPDATE_STATUS|" + item.getItemId() + "|RUNNING" );
                StartScence.client.sendMessage("UPDATE_STATUS|" + item.getIteminfomation()+"|OPEN" );
            }
        }

        // Sau khi Confirm, có thể yêu cầu tải lại dữ liệu cho chắc chắn
        AuctionItems.requestData();
    }

    private void renderAuctions() {
        ItemsTable.getItems().clear(); // Xóa dữ liệu cũ

        for (List<Object> item : AuctionItems.list) {
            String status = (String) item.get(6);

            // Trang Admin chỉ hiển thị các sản phẩm chờ duyệt (PENDING)
            if ("PENDING".equals(status)) {

                String itemid = (String) item.get(1);
                String itemname = (String) item.get(0);
                String itemType = (String) item.get(2);
                String itemInformation1 = (String) item.get(3);
                String itemInformation2 = (String) item.get(4);
                Double currentPrice = (Double) item.get(5);

                ItemsHolder newProduct = new ItemsHolder(
                        itemid,
                        itemname,
                        itemType,              // ProductDescription (Lấy type làm info tạm)
                        itemInformation1,      // information 1
                        itemInformation2,      // information 2
                        currentPrice,
                        LocalDate.of(2026, 12, 12),     // Dummy date
                        LocalTime.of(10, 10, 10),       // Dummy time
                        Time.valueOf(LocalTime.of(10, 10, 10)) // Dummy duration
                );

                ItemsTable.getItems().add(newProduct);
            }
        }
    }
}