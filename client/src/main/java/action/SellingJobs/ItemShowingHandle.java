package action.SellingJobs;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import action.Authentication.StoreItemDataInit;
import action.Core.SceneSwitch;
import action.Core.StartScence;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ItemShowingHandle implements Initializable {

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

    /**
     * Trong JavaFX, hàm initialize() luôn tự động được gọi SAU KHI
     * file FXML đã nạp xong các giao diện. Đây là nơi chuẩn nhất để nạp dữ liệu.
     */
    @FXML
    private Label name;
    @FXML
    private Label price;
    @FXML
    private Label status;
    @FXML
    private Label description;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle){
        getItem();
    }
    public void getItem(){
        name.setText(StoreItemDataInit.name);
        price.setText(StoreItemDataInit.price);
        status.setText(StoreItemDataInit.status);
        description.setText(StoreItemDataInit.description);
        String image = StoreItemDataInit.image;
        if(image!=null){
            Image image1 = new Image("/assets/rubberDuck.jfif");
        }

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

        StartScence.client.setServerListener(message -> {
            Platform.runLater(() -> {
                if (message.startsWith("BID_SUCCESS|") || message.startsWith("BID_UPDATE|")) {
                    status.setText("RUNNING");
                    price.setText(amountText);
                    money.clear();
                } else if (message.startsWith("ERROR|")) {
                    status.setText(message.substring("ERROR|".length()));
                }
            });
        });

        StartScence.client.sendMessage("BID|" + auctionId + "|" + amountText);
    }
    @FXML
    public void ReturnToInvesment(ActionEvent actionEvent) throws IOException {
        SceneSwitch sceneSwitch = new SceneSwitch();
        sceneSwitch.SwitchToAnyWhere(actionEvent,"/views/Lobby.fxml");
        }
    @FXML
    public ImageView image;
    }



