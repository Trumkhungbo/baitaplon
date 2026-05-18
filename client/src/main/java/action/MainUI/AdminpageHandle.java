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
import com.google.gson.JsonObject;
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

    @FXML private TableView<ItemsHolder> ItemsTable;
    @FXML private TableColumn<ItemsHolder, String> ProductName;
    @FXML private TableColumn<ItemsHolder, String> ProductPrice;
    @FXML private TableColumn<ItemsHolder, LocalDate> ProductDate;
    @FXML private TableColumn<ItemsHolder, LocalTime> ProductTime;
    @FXML private TableColumn<ItemsHolder, Time> SellingTime;
    @FXML private TableColumn<ItemsHolder, CheckBox> CheckBox;
    @FXML private TableColumn<ItemsHolder, String> STTColumn;

    private List<List<Object>> list = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resource) {
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
        JsonObject req = new JsonObject();
        req.addProperty("command", "LIST_AUCTIONS");
        SocketClient.getInstance().requestData(req.toString());
    }

    public void Confirm(ActionEvent event) {
        for (ItemsHolder item : ItemsTable.getItems()) {
            JsonObject req = new JsonObject();
            req.addProperty("auctionId", item.getItemId());

            if (item.getCheckBox().isSelected()){
                req.addProperty("command", "APPROVE_AUCTION");
            } else {
                req.addProperty("command", "UPDATE_STATUS");
                req.addProperty("status", "CANCELED");
            }
            SocketClient.getInstance().requestData(req.toString());
        }

        JsonObject refreshReq = new JsonObject();
        refreshReq.addProperty("command", "LIST_AUCTIONS");
        SocketClient.getInstance().requestData(refreshReq.toString());
    }

    private void renderAuctions() {
        ItemsTable.getItems().clear();
        for (List<Object> item : list) {
            String status = (String) item.get(3);
            if ("PENDING".equals(status)) {
                String itemname = (String) item.get(0);
                String itemid = (String) item.get(1);
                Double currentPrice = (Double) item.get(2);

                ItemsHolder newProduct = new ItemsHolder(
                        itemid, itemname, currentPrice,
                        LocalTime.of(10, 10, 10),
                        Time.valueOf(LocalTime.of(10, 10, 10))
                );
                ItemsTable.getItems().add(newProduct);
            }
        }
    }

    @Override
    public void onDataReceived(String data) {
        Platform.runLater(() -> {
            if (data == null || !data.startsWith("AUCTION_LIST|")) {
                return;
            }

            list.clear();
            String dataPart = data.substring("AUCTION_LIST|".length());
            if (!dataPart.isBlank()) {
                for (String itemData : dataPart.split(";")) {
                    String[] attr = itemData.split(":");
                    if (attr.length >= 4) {
                        list.add(List.of(attr[1], attr[0], Double.parseDouble(attr[2]), attr[3]));
                    }
                }
            }
            renderAuctions();
        });
    }

    public void LogOut(ActionEvent event) throws IOException {
        SocketClient.getInstance().removeListener(this);
        SceneSwitch sceneSwitch = new SceneSwitch();
        sceneSwitch.SwitchToLogin(event);
    }
}
