package action.controller.admin;

import action.Core.SceneSwitch;
import action.model.ItemsHolder;
import action.network.SocketClient;
import action.network.SocketListener;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdminSellingPageHandle implements Initializable, SocketListener {

    @FXML private TableView<ItemsHolder> ItemsTable;
    @FXML private TableColumn<ItemsHolder, String> ProductName;
    @FXML private TableColumn<ItemsHolder, String> ProductDescription;
    @FXML private TableColumn<ItemsHolder, String> Productinfomation1;
    @FXML private TableColumn<ItemsHolder, String> Productinfomation2;
    @FXML private TableColumn<ItemsHolder, String> ProductPrice;
    @FXML private TableColumn<ItemsHolder, String> ProductDate;
    @FXML private TableColumn<ItemsHolder, String> ProductTime;
    @FXML private TableColumn<ItemsHolder, String> SellingTime;
    @FXML private TableColumn<ItemsHolder, CheckBox> CheckBox;
    @FXML private TableColumn<ItemsHolder, String> STTColumn;

    private final List<List<String>> list = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resource) {
        ProductName.setCellValueFactory(new PropertyValueFactory<>("itemname"));
        ProductDescription.setCellValueFactory(new PropertyValueFactory<>("itemdescription"));
        Productinfomation1.setCellValueFactory(new PropertyValueFactory<>("iteminformation1"));
        Productinfomation2.setCellValueFactory(new PropertyValueFactory<>("iteminformation2"));
        ProductPrice.setCellValueFactory(new PropertyValueFactory<>("itemprice"));
        ProductDate.setCellValueFactory(new PropertyValueFactory<>("itemdate"));
        ProductTime.setCellValueFactory(new PropertyValueFactory<>("itemtime"));
        SellingTime.setCellValueFactory(new PropertyValueFactory<>("itemduration"));
        CheckBox.setCellValueFactory(new PropertyValueFactory<>("checkBox"));

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
        requestAuctionList();
    }

    private void requestAuctionList() {
        JsonObject req = new JsonObject();
        req.addProperty("command", "LIST_AUCTIONS");
        SocketClient.getInstance().requestData(req.toString());
    }

    public void Confirm(ActionEvent event) {
        for (ItemsHolder item : ItemsTable.getItems()) {
            JsonObject req = new JsonObject();
            req.addProperty("auctionId", item.getItemId());

            if (item.getCheckBox().isSelected()) {
                req.addProperty("command", "APPROVE_AUCTION");
            } else {
                req.addProperty("command", "UPDATE_STATUS");
                req.addProperty("status", "CANCELED");
            }
            SocketClient.getInstance().requestData(req.toString());
        }

        requestAuctionList();
    }

    private void renderAuctions() {
        ItemsTable.getItems().clear();
        for (List<String> item : list) {
            String status = item.get(3);
            if (!"PENDING".equals(status)) {
                continue;
            }

            ItemsHolder product = new ItemsHolder(
                    item.get(0),
                    item.get(1),
                    item.get(5),
                    item.get(6),
                    item.get(7),
                    formatMoney(item.get(2)),
                    item.get(8),
                    item.get(9),
                    item.get(10) + " phút"
            );
            ItemsTable.getItems().add(product);
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
                    if (attr.length >= 11) {
                        list.add(List.of(
                                attr[0],
                                attr[1],
                                attr[2],
                                attr[3],
                                attr[4],
                                attr[5],
                                attr[6],
                                attr[7],
                                attr[8],
                                attr[9],
                                attr[10]
                        ));
                    }
                }
            }
            renderAuctions();
        });
    }

    private String formatMoney(String raw) {
        try {
            return String.format("%,.0f VNĐ", Double.parseDouble(raw));
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    public void LogOut(ActionEvent event) throws IOException {
        SocketClient.getInstance().removeListener(this);
        SceneSwitch sceneSwitch = new SceneSwitch();
        sceneSwitch.SwitchToLogin(event);
    }
}
