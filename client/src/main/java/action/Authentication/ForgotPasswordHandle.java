package action.Authentication;

import action.SocketClient;
import action.SocketListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import action.Core.SceneSwitch;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.io.IOException;

public class ForgotPasswordHandle implements SocketListener {
    @FXML
    public TextField username;
    @FXML
    public TextField Id;
    @FXML
    public TextField phoneNumber;

    @FXML
    public void initialize() {
        // Lắng nghe tín hiệu mạng khi màn hình được load
        SocketClient.getInstance().addListener(this);
    }

    public void TakePass(ActionEvent event) throws IOException {
        String Username = username.getText();
        String id = Id.getText();
        String phone = phoneNumber.getText();
        SceneSwitch sceneswitch = new SceneSwitch();

        if (Username.isBlank() || id.isBlank() || phone.isBlank()) {
            sceneswitch.SwitchToLockPage(event, "/views/SomeThingUnFill.fxml");
        } else if (!phone.matches("^[0-9]{10}$")) {
            sceneswitch.SwitchToLockPage(event, "/views/WrongInputShow.fxml");
        } else if (!id.matches("^[0-9]{12}$")) {
            sceneswitch.SwitchToLockPage(event, "/views/WrongInputShow.fxml");
        } else {
            // 1. ĐÓNG GÓI JSON GỬI LÊN SERVER
            JsonObject req = new JsonObject();
            req.addProperty("command", "FORGOT_PASSWORD");
            req.addProperty("username", Username);
            req.addProperty("phone", phone);
            req.addProperty("personalID", id);

            // Gửi dữ liệu qua SocketClient mới
            SocketClient.getInstance().requestData(req.toString());
        }
    }

    public void BackToLogin(ActionEvent event) throws IOException {
        // Hủy lắng nghe mạng khi rời màn hình
        SocketClient.getInstance().removeListener(this);
        SceneSwitch sceneswitch = new SceneSwitch();
        sceneswitch.SwitchToLogin(event);
    }

    @Override
    public void onDataReceived(String message) {
        Platform.runLater(() -> {
            try {
                JsonObject res = JsonParser.parseString(message).getAsJsonObject();

                if (res.has("command") && res.get("command").getAsString().equals("FORGOT_PASSWORD_RESULT")) {
                    if (res.get("status").getAsString().equals("SUCCESS")) {
                        // Nếu thành công thì xóa listener và chuyển tab
                        SocketClient.getInstance().removeListener(this);

                        // 1. Lưu tạm username vào biến static
                        StoreDataInput.username = username.getText();
                        // 2. Chuyển sang màn hình FXML mới
                        SceneSwitch sceneSwitch = new SceneSwitch();
                        sceneSwitch.SwitchToAnyWhere(new ActionEvent(username.getScene().getWindow(), null), "/views/ResetPassword.fxml");
                    } else {
                        // Sai thông tin -> Hiện màn hình lỗi
                        SceneSwitch sceneswitch = new SceneSwitch();
                        sceneswitch.SwitchToLockPage(new ActionEvent(username.getScene().getWindow(), null), "/views/WrongValueShow.fxml");
                    }
                }
            } catch (Exception e) {
                // Bỏ qua nếu parse JSON lỗi
            }
        });
    }
}