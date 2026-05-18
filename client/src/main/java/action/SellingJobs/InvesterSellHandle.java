package action.SellingJobs;

import action.Authentication.StoreDataInput;
import action.Core.SceneSwitch;
import action.SocketClient;
import action.SocketListener;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Base64;
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
    @FXML private Label statusLabel;

    private File selectedImageFile;
    private String pendingMessage;

    SceneSwitch sceneSwitch = new SceneSwitch();

    @Override
    public void initialize(URL location, ResourceBundle resource) {
        setDescription();
        SocketClient.getInstance().addListener(this);
    }

    @FXML
    public void Clicked(ActionEvent actionEvent) throws IOException {
        try {
            LocalTime t = LocalTime.parse(TimeStart.getText());
            long startEpochMillis = t.atDate(LocalDate.now())
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            long durationMins = Long.parseLong(duration.getText());
            long priceFunc = Long.parseLong(price.getText());
            pendingMessage = "ADD_AUCTION|" + StoreDataInput.getUsername()
                    + "|" + description.getValue()
                    + "|" + itemname.getText()
                    + "|" + description1.getText()
                    + "|" + description2.getText()
                    + "|" + priceFunc
                    + "|" + startEpochMillis
                    + "|" + durationMins;

            if (selectedImageFile != null) {
                uploadImage(selectedImageFile);
                setStatus("Đang upload ảnh...");
            } else {
                SocketClient.getInstance().requestData(pendingMessage + "|");
                setStatus("Đang đăng bán...");
                pendingMessage = null;
            }

        } catch (Exception e) {
            sceneSwitch.SwitchToLockPage(actionEvent, "/views/WrongInputShow.fxml");
            e.printStackTrace();
        }
    }

    @FXML
    public void AddImage(ActionEvent actionEvent) {
        Stage currentStage = (Stage) ((javafx.scene.Node) actionEvent.getSource()).getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh của bạn");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        File file = fileChooser.showOpenDialog(currentStage);
        if (file != null) {
            selectedImageFile = file;
            imageset.setImage(new Image(file.toURI().toString()));
            setStatus("Ảnh đã chọn: " + file.getName());
        }
    }

    private void uploadImage(File imageFile) throws IOException {
        byte[] bytes = Files.readAllBytes(imageFile.toPath());
        String base64 = Base64.getEncoder().encodeToString(bytes);
        String filename = imageFile.getName();
        String ext = filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase()
                : "jpg";
        SocketClient.getInstance().requestData("UPLOAD_IMAGE|" + ext + "|" + base64);
    }

    @Override
    public void onDataReceived(String data) {
        Platform.runLater(() -> {
            if (data.startsWith("UPLOAD_IMAGE_SUCCESS|") && pendingMessage != null) {
                // Server trả về filename đã lưu → gắn vào ADD_AUCTION rồi gửi
                String filename = data.substring("UPLOAD_IMAGE_SUCCESS|".length()).trim();
                SocketClient.getInstance().requestData(pendingMessage + "|" + filename);
                System.out.println("[SELL] Sent ADD_AUCTION with image: " + filename);
                pendingMessage = null;
                setStatus("Đăng bán thành công!");

            } else if (data.startsWith("ADD_AUCTION_SUCCESS") || data.startsWith("CREATE_AUCTION_SUCCESS")) {
                setStatus("Đăng bán thành công!");

            } else if (data.startsWith("ERROR|") && pendingMessage == null) {
                setStatus("Lỗi: " + data.substring("ERROR|".length()));
            }
        });
    }

    private void setStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
        System.out.println("[SELL] " + msg);
    }

    public void setDescription() {
        description.getItems().addAll("Thông Tin", "ELECTRONICS", "ART", "VEHICLE");
        description.setValue(description.getItems().get(0));
    }
}
