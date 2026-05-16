package action.SellingJobs;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import action.Authentication.StoreItemDataInit;
import action.Core.SceneSwitch;
import action.SocketClient;
import action.SocketListener;
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

    // Nếu bạn có gán ID cho 2 nút này trong Scene Builder thì thêm @FXML vào
    @FXML
    private Button buttonLeft;
    @FXML
    private Button buttonRight;

    @FXML
    private ImageView imageView;

    // Khởi tạo list trống để tránh NullPointer
    private List<Image> imageList = new ArrayList<>();

    // Biến lưu giữ vị trí ảnh đang hiển thị
    private int currentIndex = 0;
    @FXML
    private Label name;
    @FXML
    private Label price;
    @FXML
    private Label status;
    @FXML
    private Label description;
    @FXML
    private Label date;
    @FXML
    private Label starTime;
    @FXML
    private Label duration;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle){
        SocketClient.getInstance().addListener(this);
        getItem();
    }
    public void getItem(){
        SocketClient.getInstance().requestData("GET_AUCTION_DETAILS|"+ StoreItemDataInit.description);
    }
    @FXML
    private TextField money;
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

        SocketClient.getInstance().requestData("BID|" + auctionId + "|" + amountText);
    }
    @FXML
    public void ReturnToInvesment(ActionEvent actionEvent) throws IOException {
        SceneSwitch sceneSwitch = new SceneSwitch();
        sceneSwitch.SwitchToAnyWhere(actionEvent,"/views/Lobby.fxml");
    }
    @FXML
    public ImageView image;

    @Override
    public void onDataReceived(String data) {
        Platform.runLater(() -> {
            if (data.startsWith("AUCTION_DETAIL|")) {
                String dataPart = data.substring("AUCTION_DETAIL|".length());
                String[] attributes = dataPart.split(":");
                String id = attributes[0];
                name.setText(attributes[1]);
                price.setText(attributes[3]);
                status.setText(attributes[4]);
                date.setText(attributes[5]);
                starTime.setText(attributes[6]);
                duration.setText(attributes[7]);
            } else if (data.startsWith("BID_SUCCESS|") || data.startsWith("BID_UPDATE|")) {
                status.setText("RUNNING");
                price.setText(money.getText());
                money.clear();
            } else if (data.startsWith("ERROR|")) {
                status.setText(data.substring("ERROR|".length()));
            }
        });
    }
}