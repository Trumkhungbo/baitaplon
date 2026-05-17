package action.SellingJobs;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import action.Authentication.StoreItemDataInit;
import action.MainUI.LobbyHandle;
import action.SocketClient;
import action.SocketListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ItemShowingHandle implements Initializable, SocketListener {

    @FXML private Button buttonLeft;
    @FXML private Button buttonRight;
    @FXML private ImageView imageView;
    @FXML private ImageView image;

    private List<Image> imageList = new ArrayList<>();
    private int currentIndex = 0;

    @FXML private Label name;
    @FXML private Label price;
    @FXML private Label status;
    @FXML private Label description;
    @FXML private Label date;
    @FXML private Label starTime;
    @FXML private Label duration;
    @FXML private TextField money;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle){
        SocketClient.getInstance().addListener(this);
        getItem();
    }

    public void getItem(){
        JsonObject req = new JsonObject();
        req.addProperty("command", "GET_AUCTION_DETAIL");
        req.addProperty("auctionId", StoreItemDataInit.description);
        SocketClient.getInstance().requestData(req.toString());
    }

    @FXML
    public void RaiseBind(ActionEvent actionEvent) {
        String auctionId = StoreItemDataInit.description;
        String amountText = money.getText();

        if (auctionId == null || auctionId.isBlank()) {
            status.setText("Missing auction id");
            return;
        }

        if (amountText == null || !amountText.matches("\\d+(\\.\\d+)?")) {
            status.setText("Invalid bid amount");
            return;
        }

        JsonObject req = new JsonObject();
        req.addProperty("command", "BID");
        req.addProperty("auctionId", auctionId);
        req.addProperty("amount", amountText);
        SocketClient.getInstance().requestData(req.toString());
    }

    @FXML
    public void ReturnToInvesment(ActionEvent actionEvent) throws IOException {
        SocketClient.getInstance().removeListener(this);

        //Sử dụng LobbyHandle để tráo ruột Center, KHÔNG dùng SceneSwitch nữa!
        if (LobbyHandle.getInstance() != null) {
            LobbyHandle.getInstance().ReturnInvesmentSite(actionEvent);
        }
    }

    @Override
    public void onDataReceived(String data) {
        Platform.runLater(() -> {
            try {
                JsonObject res = JsonParser.parseString(data).getAsJsonObject();
                String command = res.has("command") ? res.get("command").getAsString() : "";

                if (command.equals("AUCTION_DETAIL_RESULT")) {
                    if (res.get("status").getAsString().equals("SUCCESS")) {
                        name.setText(res.has("itemName") ? res.get("itemName").getAsString() : "Đang cập nhật");
                        price.setText(res.has("currentPrice") ? res.get("currentPrice").getAsString() : "0");
                        status.setText(res.has("auctionStatus") ? res.get("auctionStatus").getAsString() : "UNKNOWN");

                        // Lắp dữ liệu thời gian
                        date.setText(res.has("startDate") ? res.get("startDate").getAsString() : "--/--/----");
                        starTime.setText(res.has("startTime") ? res.get("startTime").getAsString() : "--:--");
                        duration.setText(res.has("duration") ? res.get("duration").getAsString() + " phút" : "0 phút");

                    } else {
                        status.setText(res.has("message") ? res.get("message").getAsString() : "Lỗi tải dữ liệu");
                    }
                }
                else if (command.equals("BID_RESULT")) {
                    if (res.get("status").getAsString().equals("SUCCESS")) {
                        status.setText("RUNNING");
                        price.setText(money.getText());
                        money.clear();
                    } else {
                        status.setText(res.get("message").getAsString());
                    }
                }
            } catch (Exception e) {
                // Tương thích ngược nếu Server chưa bọc JSON kịp
                if (data.startsWith("ERROR|")) {
                    status.setText(data.substring("ERROR|".length()));
                } else if (data.startsWith("BID_SUCCESS|")) {
                    status.setText("RUNNING");
                    price.setText(money.getText());
                    money.clear();
                }
            }
        });
    }
}