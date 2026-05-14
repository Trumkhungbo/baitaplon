package action.Authentication;

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

public class ForgotPasswordHandle {
    @FXML
    public TextField username;
    @FXML
    public TextField Id;
    @FXML
    public TextField phoneNumber;
    public void TakePass(ActionEvent event) throws IOException {
        String Username = username.getText();
        String id = Id.getText();
        String phone = phoneNumber.getText();
        SceneSwitch sceneswitch = new SceneSwitch();

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

            action.Core.StartScence.client.sendMessage(req.toString());

            // 2. LẮNG NGHE KẾT QUẢ TỪ SERVER
            action.Core.StartScence.client.setServerListener(message -> {
                Platform.runLater(() -> {
                    try {
                        JsonObject res = JsonParser.parseString(message).getAsJsonObject();

                        if (res.has("command") && res.get("command").getAsString().equals("FORGOT_PASSWORD_RESULT")) {
                            if (res.get("status").getAsString().equals("SUCCESS")) {
                                Platform.runLater(() -> {
                                    try {
                                        // 1. Lưu tạm username vào biến static
                                        StoreDataInput.username = Username;
                                        // 2. Chuyển sang màn hình FXML mới
                                        SceneSwitch sceneSwitch = new SceneSwitch();
                                        sceneSwitch.SwitchToAnyWhere(event, "/views/ResetPassword.fxml");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });

                            } else {
                                // Sai thông tin -> Hiện màn hình lỗi
                                try {
                                    sceneswitch.SwitchToLockPage(event, "/views/WrongValueShow.fxml");
                                } catch (IOException e) { e.printStackTrace(); }
                            }
                        }
                    } catch (Exception e) {}
                });
            });
        }

    }
    public void BackToLogin(ActionEvent event) throws IOException {
        SceneSwitch sceneswitch = new SceneSwitch();
        sceneswitch.SwitchToLogin(event);
    }






}
