package action.SellingJobs;

import action.Authentication.StoreDataInput;
import action.Core.SceneSwitch;
import action.SocketClient;
import action.SocketListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

    private final SceneSwitch sceneSwitch = new SceneSwitch();

    @Override
    public void initialize(URL location, ResourceBundle resource) {
        setDescription();
        SocketClient.getInstance().addListener(this);
    }

    @FXML
    public void Clicked(ActionEvent actionEvent) throws IOException {
        try {
            LocalDate date = LocalDate.now();
            LocalTime time = LocalTime.parse(TimeStart.getText());
            if (time.isBefore(LocalTime.now())) {
                date = date.plusDays(1);
            }

            long startEpochMillis = date.atTime(time)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            long durationMins = Long.parseLong(duration.getText());
            long priceValue = Long.parseLong(price.getText());

            JsonObject req = new JsonObject();
            req.addProperty("command", "ADD_AUCTION");
            req.addProperty("seller", StoreDataInput.getUsername());
            req.addProperty("itemType", description.getValue());
            req.addProperty("itemName", itemname.getText());
            req.addProperty("des1", description1.getText());
            req.addProperty("des2", description2.getText());
            req.addProperty("price", String.valueOf(priceValue));
            req.addProperty("startTime", String.valueOf(startEpochMillis));
            req.addProperty("durationMinutes", String.valueOf(durationMins));
            pendingMessage = req.toString();

            if (selectedImageFile != null) {
                uploadImage(selectedImageFile);
                setStatus("Dang upload anh...");
            } else {
                JsonObject payload = JsonParser.parseString(pendingMessage).getAsJsonObject();
                payload.addProperty("imageUrl", "");
                SocketClient.getInstance().requestData(payload.toString());
                setStatus("Dang dang ban...");
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
        fileChooser.setTitle("Chon anh san pham");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        File file = fileChooser.showOpenDialog(currentStage);
        if (file != null) {
            selectedImageFile = file;
            imageset.setImage(new Image(file.toURI().toString()));
            setStatus("Anh da chon: " + file.getName());
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

                JsonObject payload = JsonParser.parseString(pendingMessage).getAsJsonObject();
                payload.addProperty("imageUrl", filename);
                SocketClient.getInstance().requestData(payload.toString());
                System.out.println("[SELL] Sent ADD_AUCTION with image: " + filename);

                pendingMessage = null;
                setStatus("Dang dang ban...");

            } else if (data.startsWith("ADD_AUCTION_SUCCESS")
                    || data.startsWith("CREATE_AUCTION_SUCCESS")
                    || data.startsWith("ADD_AUCTION_PENDING")) {
                setStatus("Dang ban thanh cong!");
                clearForm();

            } else if (data.startsWith("ERROR|")) {
                setStatus("Loi: " + data.substring("ERROR|".length()));
                pendingMessage = null;
            }
        });
    }

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
        if (statusLabel != null) {
            statusLabel.setText(msg);
        }
        System.out.println("[SELL] " + msg);
    }

    public void setDescription() {
        description.getItems().addAll("ELECTRONICS", "ART", "VEHICLE");
        description.setValue(description.getItems().get(0));
    }
}
