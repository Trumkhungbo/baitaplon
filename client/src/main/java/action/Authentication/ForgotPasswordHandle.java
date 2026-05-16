package action.Authentication;

import action.SocketClient;
import action.SocketListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import action.Core.SceneSwitch;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;

public class ForgotPasswordHandle implements SocketListener {
    @FXML
    public TextField username;
    @FXML
    public TextField Id;
    @FXML
    public TextField phoneNumber;
    SceneSwitch sceneswitch = new SceneSwitch();

    @FXML
    public void initialize() {
        SocketClient.getInstance().addListener(this);
    }

    public void TakePass(ActionEvent event) throws IOException {
        String Username = username.getText();
        String id = Id.getText();
        String phone = phoneNumber.getText();


        if(Username.isBlank() || id.isBlank() || phone.isBlank()){
            sceneswitch.SwitchToLockPage(event, "/views/SomeThingUnFill.fxml");
        }
        else if(!phone.matches("^[0-9]{10}$")){
            sceneswitch.SwitchToLockPage(event,"/views/WrongInputShow.fxml");
        }
        else if(!id.matches("^[0-9]{12}$")){
            sceneswitch.SwitchToLockPage(event,"/views/WrongInputShow.fxml");
        } else {
            // 1. ĐÓNG GÓI JSON GỬI LÊN SERVER
            JsonObject req = new JsonObject();
            req.addProperty("command", "FORGOT_PASSWORD");
            req.addProperty("username", Username);
            req.addProperty("phone", phone);
            req.addProperty("personalID", id);

            SocketClient.getInstance().requestData(req.toString());
        }

    }
    public void BackToLogin(ActionEvent event) throws IOException {
        sceneswitch.SwitchToLogin(event);
    }

    @Override
    public void onDataReceived(String data) {
        Platform.runLater(() -> {
            try {
                JsonObject res = JsonParser.parseString(data).getAsJsonObject();

                if (res.has("command") && res.get("command").getAsString().equals("FORGOT_PASSWORD_RESULT")) {
                    if (res.get("status").getAsString().equals("SUCCESS")) {
                        Platform.runLater(() -> {
                            try {
                                // 1. Lưu tạm username vào biến static
                                StoreDataInput.username = username.getText();
                                // 2. Chuyển sang màn hình FXML mới
                                SceneSwitch sceneSwitch = new SceneSwitch();
                                sceneSwitch.SwitchToAnyWhere(new ActionEvent(username.getScene().getWindow(), null), "/views/ResetPassword.fxml");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });

                    } else {
                        // Sai thông tin -> Hiện màn hình lỗi
                        try {
                            sceneswitch.SwitchToLockPage(new ActionEvent(username.getScene().getWindow(), null), "/views/WrongValueShow.fxml");
                        } catch (IOException e) { e.printStackTrace(); }
                    }
                }
            } catch (Exception e) {}
        });
    }
}
