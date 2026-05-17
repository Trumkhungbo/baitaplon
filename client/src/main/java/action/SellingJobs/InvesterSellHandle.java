package action.SellingJobs;

import action.Authentication.StoreDataInput;
import action.Core.SceneSwitch;
import action.SocketClient;
import action.SocketListener;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalTime;
import java.util.ResourceBundle;

public class InvesterSellHandle implements Initializable, SocketListener {

    @FXML private TextField itemname;
    @FXML private ChoiceBox<String> description;
    @FXML private TextField description1;
    @FXML private TextField description2;
    @FXML private TextField price;
    @FXML private TextField TimeStart;
    @FXML private TextField duration;
    @FXML private ImageView imageset;

    SceneSwitch sceneSwitch = new SceneSwitch();

    @Override
    public void initialize(URL location, ResourceBundle resource){
        description.getItems().addAll("Thông Tin","ELECTRONICS","ART","VEHICLE");
        description.setValue(description.getItems().get(0));

        //Lắng nghe phản hồi từ Server
        SocketClient.getInstance().addListener(this);
    }

    @FXML
    public void Clicked(ActionEvent actionEvent) throws IOException {
        try {
            // Kiểm tra Validation cục bộ
            LocalTime StarTime = LocalTime.parse(TimeStart.getText());
            int minutesInput = Integer.parseInt(duration.getText());
            Long priceFunc = Long.parseLong(price.getText());

            //Đóng gói JSON chuẩn mực gửi lên Server
            JsonObject req = new JsonObject();
            req.addProperty("command", "ADD_AUCTION");
            req.addProperty("sellerUsername", StoreDataInput.getUsername());
            req.addProperty("itemType", description.getValue());
            req.addProperty("itemName", itemname.getText());
            req.addProperty("des1", description1.getText());
            req.addProperty("des2", description2.getText());
            req.addProperty("price", priceFunc);
            req.addProperty("startTime", StarTime.toString());
            int hours = minutesInput / 60;
            int minutes = minutesInput % 60;
            String timeString = String.format("%02d:%02d:00", hours, minutes);
            req.addProperty("durationMinutes", timeString);

            SocketClient.getInstance().requestData(req.toString());
            System.out.println("[CLIENT] Đang gửi yêu cầu đăng bán...");

        } catch (Exception e) {
            sceneSwitch.SwitchToLockPage(actionEvent, "/views/WrongInputShow.fxml");
            e.printStackTrace();
        }
    }

    @FXML
    public void AddImage(ActionEvent actionEvent) {
        Stage currentStage = (Stage) ((javafx.scene.Node) actionEvent.getSource()).getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(currentStage);

        if (selectedFile != null) {
            Image image = new Image(selectedFile.toURI().toString());
            imageset.setImage(image);
        }
    }

    @Override
    public void onDataReceived(String data) {
        Platform.runLater(() -> {
            // Đón gói tin báo thành công của hàm createPendingAuction trên Server
            if (data.startsWith("ADD_AUCTION_PENDING|")) {
                itemname.clear();
                price.clear();
                description1.clear();
                description2.clear();
                TimeStart.clear();
                duration.clear();
                imageset.setImage(null);
            } else if (data.startsWith("ERROR|")) {
                System.out.println("Lỗi từ Server: " + data);
            }
        });
    }
}