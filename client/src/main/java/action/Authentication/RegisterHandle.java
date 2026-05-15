package action.Authentication;

import action.Core.SceneSwitch;
import action.Core.StartScence;
import com.google.gson.JsonObject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class RegisterHandle {
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
        }
        else if (!email.matches("^[a-zA-Z0-9._%+-]+@gmail\\.com$")) {
            sceneswitch.SwitchToLockPage(event, "/views/WrongInputShow.fxml");
        }
        else if (!phoneNumber.matches("^[0-9]{10}$")) {
            sceneswitch.SwitchToLockPage(event, "/views/WrongInputShow.fxml");
        }
        else if (!personalID.matches("[0-9]{12}$"))
            {
                sceneswitch.SwitchToLockPage(event, "/views/WrongInputShow.fxml");
            }
        else {

            StartScence.client.setServerListener(message -> {
                javafx.application.Platform.runLater(() -> {
                    if (message.startsWith("REGISTER_SUCCESS")) {
                        try {
                            SceneSwitch sceneswitch = new SceneSwitch();
                            sceneswitch.SwitchToLogin(event);
                            sceneswitch.SwitchToLockPage(event, "/views/RegisterPopUp.fxml");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (message.startsWith("REGISTER_FAILED")) {
                        try {
                            // Tài khoản đã tồn tại hoặc lỗi
                            SceneSwitch sceneswitch = new SceneSwitch();
                            sceneswitch.SwitchToLockPage(event, "/views/UsedAccount.fxml");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            });
            JsonObject req = new JsonObject();
            req.addProperty("command", "REGISTER");
            req.addProperty("username", name);
            req.addProperty("password", password);
            req.addProperty("phone", phoneNumber);
            req.addProperty("email", email);
            req.addProperty("personalID", personalID);
            StartScence.client.sendMessage(req.toString());
        }

    }
    public void Login(ActionEvent event) throws IOException {
        sceneswitch.SwitchToLogin(event);
    }
}
