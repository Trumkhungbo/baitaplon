package action.MainUI;

import java.net.URL;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ResourceBundle;

import action.SellingJobs.AcceptedSellingData;
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

public class AdminpageHandle implements Initializable {
    ItemsHolder Product = new ItemsHolder("a","b",9.0, LocalDate.of(2026,4,30), LocalTime.of(16,0), Time.valueOf(LocalTime.of(1,30)));
    @FXML
    private TableView<ItemsHolder> ItemsTable;
    @FXML
    private TableColumn<ItemsHolder,String> ProductName;
    @FXML
    private TableColumn<ItemsHolder,String> ProductPrice;
    @FXML
    private TableColumn<ItemsHolder,LocalDate> ProductDate;
    @FXML
    private TableColumn<ItemsHolder,LocalTime> ProductTime;
    @FXML
    private TableColumn<ItemsHolder,Time> SellingTime;
    @FXML
    private TableColumn<ItemsHolder,String> ProductDescription;
    @FXML
    private TableColumn<ItemsHolder, CheckBox> CheckBox;
    @FXML
    private TableColumn<ItemsHolder,String> STTColumn;
    @Override
    public void initialize(URL location, ResourceBundle resource){
        ProductName.setCellValueFactory(new PropertyValueFactory<>("itemname"));
        ProductPrice.setCellValueFactory(new PropertyValueFactory<>("itemprice"));
        ProductDate.setCellValueFactory(new PropertyValueFactory<>("itemdate"));
        ProductTime.setCellValueFactory(new PropertyValueFactory<>("itemtime"));
        SellingTime.setCellValueFactory(new PropertyValueFactory<>("itemduration"));
        ProductDescription.setCellValueFactory(new PropertyValueFactory<>("iteminfomation"));
        CheckBox.setCellValueFactory(new PropertyValueFactory<>("checkBox"));
        for (int i = 0; i < 11; i++) {
            ItemsHolder newProduct = new ItemsHolder("a", "b", 9.0,
                    LocalDate.of(2026, 4, 30),
                    LocalTime.of(16, 0),
                    Time.valueOf(LocalTime.of(1, 30))
            );
            ItemsTable.getItems().add(newProduct);
        }
        ItemsTable.setItems(ShopDataBase.danhSachSanPham);

        STTColumn.setCellFactory(column -> new TableCell<ItemsHolder, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                // Quan trọng: Kiểm tra nếu dòng trống hoặc không có dòng (TableRow)
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Lấy index trực tiếp từ TableRow để đảm bảo tính chính xác khi cuộn
                    int index = getTableRow().getIndex();
                    setText(String.valueOf(index + 1));
                }
            }
        });
        STTColumn.setStyle("-fx-alignment: CENTER;");

    }
    public void QueueNewStuff(String name,String in4,double price,LocalDate date,LocalTime Starttime,Time time){
        ItemsHolder item = new ItemsHolder(name,in4,price,date,Starttime,time);
        ItemsTable.getItems().add(item);

    }
    public void Confirm(ActionEvent event){
        AcceptedSellingData.transferredItems.clear();
        for (ItemsHolder item : ItemsTable.getItems()) {
            if (item.getCheckBox().isSelected()) {
                AcceptedSellingData.transferredItems.add(item);
            }
        }
        ItemsTable.getItems().clear();
    }
    

}

