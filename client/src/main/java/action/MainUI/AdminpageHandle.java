package action.MainUI;

import java.net.URL;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.ResourceBundle;

import action.Core.StartScence;
import action.SellingJobs.AuctionCardItem;
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
    ItemsHolder Product = new ItemsHolder("a", "b", "","",9.0, LocalDate.of(2026, 4, 30), LocalTime.of(16, 0), Time.valueOf(LocalTime.of(1, 30)));
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
        fetchAuctionsFromServer();

        ProductName.setCellValueFactory(new PropertyValueFactory<>("itemname"));
        Productinfomation1.setCellValueFactory(new PropertyValueFactory<>("iteminfomation1"));
        Productinfomation2.setCellValueFactory(new PropertyValueFactory<>("iteminformation2"));
        ProductPrice.setCellValueFactory(new PropertyValueFactory<>("itemprice"));
        ProductDate.setCellValueFactory(new PropertyValueFactory<>("itemdate"));
        ProductTime.setCellValueFactory(new PropertyValueFactory<>("itemtime"));
        SellingTime.setCellValueFactory(new PropertyValueFactory<>("itemduration"));
        ProductDescription.setCellValueFactory(new PropertyValueFactory<>("iteminfomation"));
        CheckBox.setCellValueFactory(new PropertyValueFactory<>("checkBox"));
        for (int i = 0; i < 11; i++) {
            ItemsHolder newProduct = new ItemsHolder("a", "ELECTRONICS","","", 9.0,
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
    public void Confirm(ActionEvent event) {
        for (ItemsHolder item : ItemsTable.getItems()) {
            if (item.getCheckBox().isSelected()){
                StartScence.client.sendMessage("UPDATE_STATUS|" + item.getItemname()+"|OPEN" );
            }
        }
        ItemsTable.getItems().clear();
    }

    public void fetchAuctionsFromServer(){
        for(List item: AuctionItems.list) {
            if(!item.isEmpty()) {
                if(((String) item.get(6)).equals("PENDING")){
                    ItemsTable.getItems().add( new ItemsHolder((String) item.get(0),
                            (String) item.get(1),
                            (String) item.get(2),
                            (String) item.get(3),
                            (Double) item.get(4),
                            LocalDate.of(2026,12,12),
                            LocalTime.of(10,10,10),
                            Time.valueOf(LocalTime.of(10,10,10))));

                }
            }
        }

    }
}

