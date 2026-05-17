package action.Authentication;

import action.Core.SceneSwitch;
import action.SocketClient;
import action.SocketListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class RegisterHandle implements SocketListener {
    SceneSwitch sceneswitch = new SceneSwitch();
    @FXML
    private TextField Name;
    @FXML
    private TextField PhoneNumber;
    @FXML
    private TextField Email;
    @FXML
    private TextField PersonalID;
    @FXML
    private PasswordField Password;
    @FXML
    private PasswordField ConfirmPassword;
    @FXML
    private Button Login;
    @FXML
    private Button Signup;

    @FXML
    public void initialize() {
        SocketClient.getInstance().addListener(this);
    }

    public void Register(ActionEvent event) throws IOException {
        String name = Name.getText();
        String phoneNumber = PhoneNumber.getText();
        String email = Email.getText();
        String personalID = PersonalID.getText();
        String password = Password.getText();
        String confirmPassword = ConfirmPassword.getText();
        if(name.isBlank() || phoneNumber.isBlank() || email.isBlank() || personalID.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            SceneSwitch sceneswitch = new SceneSwitch();
            sceneswitch.SwitchToLockPage(event, "/views/SomeThingUnFill.fxml");
        }
        else if(!password.equals(confirmPassword)) {
            System.out.println("Passwords do not match");
            sceneswitch.SwitchToLockPage(event, "/views/WrongInputShow.fxml");
        }
        else if (!email.matches("^[a-zA-Z0-9._%+-]+@gmail\\.com$")) {
            sceneswitch.SwitchToLockPage(event, "/views/WrongInputShow.fxml");
        }
        else if (!phoneNumber.matches("^[0-9]{10}$")) {
            sceneswitch.SwitchToLockPage(event, "/views/WrongInputShow.fxml");
        }
        else if (!personalID.matches("^[0-9]{12}$"))
        {
            sceneswitch.SwitchToLockPage(event, "/views/WrongInputShow.fxml");
        }
        else {
            JsonObject req = new JsonObject();
            req.addProperty("command", "REGISTER");
            req.addProperty("username", name);
            req.addProperty("password", password);
            req.addProperty("phone", phoneNumber);
            req.addProperty("email", email);
            req.addProperty("personalID", personalID);
            SocketClient.getInstance().requestData(req.toString());
        }
    }

    public void Login(ActionEvent event) throws IOException {
        SocketClient.getInstance().removeListener(this);
        sceneswitch.SwitchToLogin(event);
    }

    @Override
    public void onDataReceived(String data) {
        Platform.runLater(() -> {
            try {
                // 1. Dịch chuỗi nhận được thành JSON
                JsonObject res = JsonParser.parseString(data).getAsJsonObject();

                // 2. Kiểm tra xem có đúng là gói tin phản hồi Đăng ký không
                if (res.has("command") && res.get("command").getAsString().equals("REGISTER_RESULT")) {
                    String status = res.get("status").getAsString();

                    if (status.equals("SUCCESS")) {
                        // Đăng ký thành công -> Gỡ tai nghe -> Bật Pop-up -> Chuyển về Login
                        SocketClient.getInstance().removeListener(this);
                        try {
                            sceneswitch.SwitchToLogin(new ActionEvent(Name.getScene().getWindow(), null));
                            sceneswitch.SwitchToLockPage(new ActionEvent(Name.getScene().getWindow(), null), "/views/RegisterPopUp.fxml");
                        } catch (IOException e) { e.printStackTrace(); }

                    } else {
                        // Đăng ký thất bại (Trùng tài khoản) -> Bật Pop-up báo lỗi
                        try {
                            sceneswitch.SwitchToLockPage(new ActionEvent(Name.getScene().getWindow(), null), "/views/UsedAccount.fxml");
                        } catch (IOException e) { e.printStackTrace(); }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}