package action.SellingJobs;

import action.Authentication.StoreDataInput;
import action.MainUI.LobbyHandle;
import action.SocketClient;
import action.SocketListener;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
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
    @FXML private TextArea productDescription;
    @FXML private ChoiceBox<String> description;
    @FXML private TextField description1;
    @FXML private TextField description2;
    @FXML private TextField price;
    @FXML private TextField TimeStart;
    @FXML private DatePicker auctionDate;
    @FXML private TextField duration;
    @FXML private ImageView imageset;
    @FXML private Label statusLabel;
    @FXML private Label pageTitle;
    @FXML private Button submitButton;

    private File selectedImageFile;
    private String pendingImageFilename = "";

    @Override
    public void initialize(URL location, ResourceBundle resource) {
        initializeDefaults();
        SocketClient.getInstance().addListener(this);
        loadEditState();
    }

    @FXML
    public void Clicked(ActionEvent actionEvent) {
        try {
            LocalDate date = auctionDate != null && auctionDate.getValue() != null
                    ? auctionDate.getValue()
                    : LocalDate.now();
            LocalTime time = LocalTime.parse(TimeStart.getText().trim());
            if ((auctionDate == null || auctionDate.getValue() == null) && time.isBefore(LocalTime.now())) {
                date = date.plusDays(1);
            }

            long startEpochMillis = date.atTime(time)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
            long durationMins = Long.parseLong(duration.getText().trim());
            long priceValue = Long.parseLong(price.getText().trim().replace(",", ""));

            if (selectedImageFile != null) {
                uploadImage(selectedImageFile);
                setStatus("Đang upload ảnh...");
                return;
            }

            sendAuctionPayload(startEpochMillis, durationMins, priceValue, pendingImageFilename);
        } catch (Exception e) {
            setStatus("Dữ liệu không hợp lệ, vui lòng kiểm tra lại.");
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

    private void sendAuctionPayload(long startEpochMillis, long durationMins, long priceValue, String imageUrl) {
        JsonObject req = new JsonObject();
        req.addProperty("command", StoreSellerProductEdit.editing ? "UPDATE_AUCTION" : "ADD_AUCTION");
        if (StoreSellerProductEdit.editing) {
            req.addProperty("auctionId", StoreSellerProductEdit.auctionId);
        }
        req.addProperty("seller", StoreDataInput.getUsername());
        req.addProperty("itemType", description.getValue());
        req.addProperty("itemName", itemname.getText());
        req.addProperty("des1", description1.getText());
        req.addProperty("des2", description2.getText());
        req.addProperty("price", String.valueOf(priceValue));
        req.addProperty("startTime", String.valueOf(startEpochMillis));
        req.addProperty("durationMinutes", String.valueOf(durationMins));
        req.addProperty("description", productDescription == null ? "" : productDescription.getText());
        req.addProperty("imageUrl", imageUrl == null ? "" : imageUrl);
        SocketClient.getInstance().requestData(req.toString());
        setStatus(StoreSellerProductEdit.editing ? "Đang cập nhật sản phẩm..." : "Đang đăng bán...");
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
            if (data.startsWith("UPLOAD_IMAGE_SUCCESS|")) {
                pendingImageFilename = data.substring("UPLOAD_IMAGE_SUCCESS|".length()).trim();
                if (StoreSellerProductEdit.editing) {
                    StoreSellerProductEdit.imageUrl = pendingImageFilename;
                }
                selectedImageFile = null;
                try {
                    LocalDate date = auctionDate != null && auctionDate.getValue() != null
                            ? auctionDate.getValue()
                            : LocalDate.now();
                    LocalTime time = LocalTime.parse(TimeStart.getText().trim());
                    long startEpochMillis = date.atTime(time)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli();
                    long durationMins = Long.parseLong(duration.getText().trim());
                    long priceValue = Long.parseLong(price.getText().trim().replace(",", ""));
                    sendAuctionPayload(startEpochMillis, durationMins, priceValue, pendingImageFilename);
                } catch (Exception e) {
                    setStatus("Không thể gửi dữ liệu sau khi upload ảnh.");
                }
                return;
            }

            if (data.startsWith("IMAGE_DATA|")
                    && StoreSellerProductEdit.editing
                    && StoreSellerProductEdit.imageUrl != null
                    && !StoreSellerProductEdit.imageUrl.isBlank()) {
                String[] parts = data.split("\\|", 4);
                if (parts.length >= 4 && StoreSellerProductEdit.imageUrl.equals(parts[1])) {
                    try {
                        byte[] bytes = Base64.getDecoder().decode(parts[3]);
                        imageset.setImage(new Image(new ByteArrayInputStream(bytes)));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                return;
            }

            if (data.startsWith("ADD_AUCTION_SUCCESS")
                    || data.startsWith("CREATE_AUCTION_SUCCESS")
                    || data.startsWith("ADD_AUCTION_PENDING")
                    || data.startsWith("UPDATE_AUCTION_SUCCESS")) {
                boolean wasEditing = StoreSellerProductEdit.editing;
                if (wasEditing) {
                    resetToFreshCreateForm();
                } else {
                    setStatus("Đăng bán thành công!");
                    clearForm();
                    StoreSellerProductEdit.clear();
                }
                return;
            }

            if (data.startsWith("ERROR|")) {
                if (StoreSellerProductEdit.editing) {
                    resetToFreshCreateForm();
                } else {
                    setStatus("Lỗi: " + data.substring("ERROR|".length()));
                }
            }
        });
    }

    private void initializeDefaults() {
        description.getItems().setAll("ELECTRONICS", "ART", "VEHICLE");
        clearForm();
    }

    private void clearForm() {
        itemname.clear();
        if (productDescription != null) {
            productDescription.clear();
        }
        description1.clear();
        description2.clear();
        price.clear();
        TimeStart.clear();
        duration.clear();
        if (description != null && !description.getItems().isEmpty()) {
            description.setValue(description.getItems().get(0));
        }
        if (auctionDate != null) {
            auctionDate.setValue(LocalDate.now());
        }
        if (imageset != null) {
            imageset.setImage(null);
        }
        selectedImageFile = null;
        pendingImageFilename = "";
        if (pageTitle != null) {
            pageTitle.setText("Đăng bán sản phẩm mới");
        }
        if (submitButton != null) {
            submitButton.setText("Đăng bán ngay");
        }
    }

    private void resetToFreshCreateForm() {
        StoreSellerProductEdit.clear();
        clearForm();
        try {
            if (LobbyHandle.getInstance() != null) {
                LobbyHandle.getInstance().MovingCenter("/views/InvesterSell.fxml");
            }
        } catch (IOException e) {
            setStatus("Không thể tải lại form đăng bán.");
        }
    }

    private void setStatus(String msg) {
        if (statusLabel != null) {
            statusLabel.setText(msg);
        }
    }

    private void loadEditState() {
        if (!StoreSellerProductEdit.editing) {
            return;
        }

        pageTitle.setText("Chỉnh sửa sản phẩm");
        submitButton.setText("Cập nhật sản phẩm");
        itemname.setText(StoreSellerProductEdit.itemName);
        if (productDescription != null) {
            productDescription.setText(StoreSellerProductEdit.description);
        }
        description.setValue(StoreSellerProductEdit.itemType);
        description1.setText(StoreSellerProductEdit.information1);
        description2.setText(StoreSellerProductEdit.information2);
        price.setText(StoreSellerProductEdit.price);
        TimeStart.setText(StoreSellerProductEdit.time);
        duration.setText(StoreSellerProductEdit.duration);
        if (auctionDate != null && StoreSellerProductEdit.date != null && !StoreSellerProductEdit.date.isBlank()) {
            auctionDate.setValue(LocalDate.parse(StoreSellerProductEdit.date));
        }
        pendingImageFilename = StoreSellerProductEdit.imageUrl == null ? "" : StoreSellerProductEdit.imageUrl;
        if (!pendingImageFilename.isBlank()) {
            SocketClient.getInstance().requestData("GET_IMAGE|" + pendingImageFilename);
        }
    }
}
