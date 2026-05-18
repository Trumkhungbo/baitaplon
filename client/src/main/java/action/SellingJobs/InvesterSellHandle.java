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

            // FIX LỖI 1: Chuẩn hóa chuỗi gửi lên Server theo đúng định dạng khóa=giá_trị
            pendingMessage = "ADD_AUCTION"
                    + "|seller=" + StoreDataInput.getUsername()
                    + "|itemType=" + description.getValue()
                    + "|itemName=" + itemname.getText()
                    + "|des1=" + description1.getText()
                    + "|des2=" + description2.getText()
                    + "|price=" + priceFunc
                    + "|startTime=" + startEpochMillis
                    + "|durationMinutes=" + durationMins;

            if (selectedImageFile != null) {
                uploadImage(selectedImageFile);
                setStatus("Đang upload ảnh...");
            } else {
                // FIX LỖI 2: Thêm trường imageUrl trống thay vì để dấu '|' dư thừa ở cuối
                SocketClient.getInstance().requestData(pendingMessage + "|imageUrl=");
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
        fileChooser.setTitle("Chọn ảnh sản phẩm");
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
                String filename = data.substring("UPLOAD_IMAGE_SUCCESS|".length()).trim();

                // Gửi kèm tham số imageUrl=tên_file chuẩn định dạng cấu trúc hệ thống
                SocketClient.getInstance().requestData(pendingMessage + "|imageUrl=" + filename);
                System.out.println("[SELL] Sent ADD_AUCTION with image: " + filename);

                pendingMessage = null;
                setStatus("Đang đăng bán..."); // FIX LỖI 3: Chờ Server xác nhận thành công thực sự

            } else if (data.startsWith("ADD_AUCTION_SUCCESS") || data.startsWith("CREATE_AUCTION_SUCCESS") || data.startsWith("ADD_AUCTION_PENDING")) {
                setStatus("Đăng bán thành công!");
                clearForm(); // FIX LỖI 4: Xóa sạch form sau khi đăng bán thành công

            } else if (data.startsWith("ERROR|")) {
                setStatus("Lỗi: " + data.substring("ERROR|".length()));
                pendingMessage = null; // Giải phóng bộ nhớ nếu có lỗi xảy ra giữa chừng
            }
        });
    }

    // Hàm bổ sung giúp dọn dẹp giao diện sau khi hoàn tất
    private void clearForm() {
        itemname.clear();
        price.clear();
        description1.clear();
        description2.clear();
        TimeStart.clear();
        duration.clear();
        imageset.setImage(null);
        selectedImageFile = null;
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
