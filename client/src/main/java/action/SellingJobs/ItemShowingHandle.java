package action.SellingJobs;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @FXML private Button buttonLeft;
    @FXML private Button buttonRight;
    @FXML private ImageView imageView;
    @FXML private ImageView image; // Để tương thích với FXML cũ nếu có

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
        // Đồng bộ lệnh gửi dạng số ít, có tham số cấu trúc key=value rõ ràng
        SocketClient.getInstance().requestData("GET_AUCTION_DETAIL|auctionId=" + StoreItemDataInit.description);
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

        SocketClient.getInstance().requestData("BID|auctionId=" + auctionId + "|amount=" + amountText);
    }

    @FXML
    public void ReturnToInvesment(ActionEvent actionEvent) throws IOException {
        SocketClient.getInstance().removeListener(this);
        SceneSwitch sceneSwitch = new SceneSwitch();
        sceneSwitch.SwitchToAnyWhere(actionEvent, "/views/Lobby.fxml");
    }

    @Override
    public void onDataReceived(String data) {
        Platform.runLater(() -> {
            if (data == null || data.isEmpty()) return;

            String[] parts = data.split("\\|");
            String command = parts[0];

            // Tự động đưa toàn bộ tham số nhận được vào Map để đọc theo tên khóa (Key) an toàn
            Map<String, String> params = new HashMap<>();
            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];
                if (part.contains("=")) {
                    String[] keyValue = part.split("=", 2);
                    params.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }

            if (command.equals("AUCTION_DETAIL")) {
                name.setText(params.getOrDefault("itemName", "Đang cập nhật"));
                price.setText(params.getOrDefault("currentPrice", "0"));
                status.setText(params.getOrDefault("status", "UNKNOWN"));
                date.setText(params.getOrDefault("startDate", "--/--/----"));

                if (params.containsKey("startTime")) {
                    starTime.setText(params.get("startTime"));
                }

                String durationVal = params.containsKey("duration") ? params.get("duration") : params.getOrDefault("durationMinutes", "0");
                duration.setText(durationVal + " phút");

                // Cập nhật ảnh sản phẩm lên ImageView nếu có link từ Server
                String imageUrl = params.getOrDefault("imageUrl", "");
                if (!imageUrl.isEmpty() && imageView != null) {
                    try {
                        imageView.setImage(new Image(imageUrl));
                    } catch (Exception ignored) {}
                }
            }
            else if (command.equals("BID_UPDATE") || command.equals("BID_RESULT") || command.equals("BID_SUCCESS")) {
                String resStatus = params.getOrDefault("status", "SUCCESS");
                if (resStatus.equals("SUCCESS")) {
                    status.setText("RUNNING");
                    // Cập nhật giá cao nhất chính xác trả về từ Server
                    if (params.containsKey("highestBid")) {
                        price.setText(params.get("highestBid"));
                    } else if (params.containsKey("amount")) {
                        price.setText(params.get("amount"));
                    }
                    money.clear();
                } else {
                    status.setText(params.getOrDefault("message", "Lỗi đặt cược"));
                }
            }
            else if (command.equals("ERROR")) {
                status.setText(params.getOrDefault("message", "Đã xảy ra lỗi!"));
            }
        });
    }
}
