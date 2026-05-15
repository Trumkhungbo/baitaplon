package action.MainUI;

import java.net.URL;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ResourceBundle;

import action.Core.StartScence;
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
    ItemsHolder Product = new ItemsHolder("a", "b", 9.0, LocalDate.of(2026, 4, 30), LocalTime.of(16, 0), Time.valueOf(LocalTime.of(1, 30)));
    @FXML
    private TableView<ItemsHolder> ItemsTable;
    @FXML
    private TableColumn<ItemsHolder, String> ProductName;
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
    public void Confirm(ActionEvent event) {
        for (ItemsHolder item : ItemsTable.getItems()) {
            if (item.getCheckBox().isSelected()){
                StartScence.client.sendMessage("UPDATE_STATUS|" + item.getIteminfomation()+"|OPEN" );
            }
        }
        ItemsTable.getItems().clear();
    }

    public void fetchAuctionsFromServer() {
        // 1. Lắng nghe phản hồi từ Server
        StartScence.client.setServerListener(message -> {
            System.out.println("Nhận được từ Server: " + message);

            if (message.startsWith("AUCTION_LIST|")) {
                // Cắt bỏ phần header
                String dataPart = message.substring("AUCTION_LIST|".length());

                if (dataPart.isEmpty()) {
                    System.out.println("Không có sản phẩm nào.");
                    return;
                }

                // Dữ liệu trả về sẽ chạy trên Thread của Socket, cần đưa vào JavaFX Thread để cập nhật UI
                Platform.runLater(() -> {
                    // Xóa dữ liệu cũ trên giao diện nếu có (ví dụ VBox hoặc FlowPane)
                    // vBoxLobby.getChildren().clear();

                    // Phân tách từng Item bằng dấu chấm phẩy ";"
                    String[] items = dataPart.split(";");
                    for (String itemData : items) {
                        // Phân tách các thuộc tính của 1 Item bằng dấu hai chấm ":"
                        String[] attributes = itemData.split(":");
                        if (attributes.length >= 4) {
                            String id = attributes[0];
                            String itemName = attributes[1];
                            Double currentPrice = Double.parseDouble(attributes[2]);
                            String status = attributes[3];
                            if(status.equals("PENDING")){
                            ItemsHolder itemsHolder = new ItemsHolder(itemName,id, (double) currentPrice, LocalDate.of(2026, 6, 30),LocalTime.of(15,20,30),Time.valueOf(LocalTime.of(10,30)));
                            ItemsTable.getItems().add(itemsHolder);}
                        }
                    }
                });
            }
        });

        // 2. Gửi yêu cầu lấy danh sách đến Server
        StartScence.client.sendMessage("LIST_AUCTIONS");

    }
}

