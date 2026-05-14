package action.Authentication;

import action.Core.SceneSwitch;
import action.Core.StartScence;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class LoginHandle {
    SceneSwitch sceneSwitch = new SceneSwitch();
    @FXML
    private TextField login;
    @FXML
    private PasswordField pass;
    @FXML
    private TextField passVisible;
    @FXML
    private Button togglePasswordButton;
    @FXML
    private Label label;

    @FXML
    public void initialize() {
        passVisible.setManaged(false);
        passVisible.setVisible(false);
        passVisible.textProperty().bindBidirectional(pass.textProperty());
    }

    public void Login(ActionEvent clicked) throws IOException {
        String password1 = isPasswordVisible() ? passVisible.getText() : pass.getText();
        String user1 = login.getText();
        StoreDataInput.username=user1;
        StoreDataInput.password=password1;
        if (password1.isBlank() || user1.isBlank()) {
            sceneSwitch.SwitchToLockPage(clicked, "/views/SomeThingUnFill.fxml");

        }
        //else-if(false){
        // sceneSwitch.SwitchToLockPage(clicked,"/SomeThingUnFill.fxml")}
        else {
            StartScence.client.setServerListener(message -> {
                Platform.runLater(() -> {
                    if (message.startsWith("LOGIN_SUCCESS")) {
                        try {
                            sceneSwitch.SwitchToAnyWhere(clicked, "/views/Lobby.fxml");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else if (message.startsWith("LOGIN_FAILED")) {
                            SceneSwitch sceneSwitch = new SceneSwitch();
                        try {
                            sceneSwitch.SwitchToLockPage(clicked,"/víews/SomeThingUnFill.fxml");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                    }

                });
            });

            JsonObject req = new JsonObject();
            req.addProperty("command", "LOGIN");
            req.addProperty("username", user1);
            req.addProperty("password", password1);
            StartScence.client.sendMessage(req.toString());
        }
    }

    public void Register(ActionEvent clicked) throws IOException {
        sceneSwitch.SwitchToRegister(clicked);
    }
    public void ForgotPassword(ActionEvent clicked) throws IOException {
        sceneSwitch.SwitchToAnyWhere(clicked, "/views/ForgotPassword.fxml");
    }
    public void Adminloggin(ActionEvent clicked) throws IOException {
        sceneSwitch.SwitchToAnyWhere(clicked, "/views/AdminLoggin.fxml");
    }

    @FXML
    public void togglePasswordVisibility() {
        boolean showPassword = !passVisible.isVisible();
        passVisible.setVisible(showPassword);
        passVisible.setManaged(showPassword);
        pass.setVisible(!showPassword);
        pass.setManaged(!showPassword);
        togglePasswordButton.setText(showPassword ? "🙈" : "👁");
    }

    private boolean isPasswordVisible() {
        return passVisible != null && passVisible.isVisible();
    }
}
