package action.controller.auth;

import action.Core.SceneSwitch;
import action.model.StoreDataInput;
import action.network.SocketClient;
import action.network.SocketListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class LoginHandle implements SocketListener {
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
        SocketClient.getInstance().addListener(this);
    }

    public void Login(ActionEvent clicked) throws IOException {
        String password1 = isPasswordVisible() ? passVisible.getText() : pass.getText();
        String user1 = login.getText();
        StoreDataInput.username=user1;
        StoreDataInput.password=password1;
        if (password1.isBlank() || user1.isBlank()) {
            sceneSwitch.SwitchToLockPage(clicked, "/views/SomeThingUnFill.fxml");

        }
        else {
            JsonObject req = new JsonObject();
            req.addProperty("command", "LOGIN");
            req.addProperty("username", user1);
            req.addProperty("password", password1);
            SocketClient.getInstance().requestData(req.toString());
        }
    }

    public void Register(ActionEvent clicked) throws IOException {
        SocketClient.getInstance().removeListener(this);
        sceneSwitch.SwitchToRegister(clicked);
    }
    public void ForgotPassword(ActionEvent clicked) throws IOException {
        SocketClient.getInstance().removeListener(this);
        sceneSwitch.SwitchToAnyWhere(clicked, "/views/ForgotPassword.fxml");
    }
    public void Adminloggin(ActionEvent clicked) throws IOException {
        SocketClient.getInstance().removeListener(this);
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

    @Override
    public void onDataReceived(String data) {
        Platform.runLater(() -> {
            try {
                JsonObject res = JsonParser.parseString(data).getAsJsonObject();
                if (res.has("command") && res.get("command").getAsString().equals("LOGIN_RESULT")) {
                    String status = res.get("status").getAsString();

                    if (status.equals("SUCCESS")) {
                        SocketClient.getInstance().removeListener(this);
                        sceneSwitch.SwitchToAnyWhere(new ActionEvent(login.getScene().getWindow(), null), "/views/Lobby.fxml");
                    } else {
                        sceneSwitch.SwitchToLockPage(new ActionEvent(login.getScene().getWindow(), null), "/views/FailedLogin.fxml");
                    }
                }
            } catch (Exception e) {
                // Hỗ trợ backward compatibility nếu Server chưa sửa kịp phản hồi JSON
                if (data.startsWith("LOGIN_SUCCESS")) {
                    SocketClient.getInstance().removeListener(this);
                    try {
                        sceneSwitch.SwitchToAnyWhere(new ActionEvent(login.getScene().getWindow(), null), "/views/Lobby.fxml");
                    } catch (IOException ioException) { ioException.printStackTrace(); }
                } else if (data.startsWith("LOGIN_FAILED")) {
                    try {
                        sceneSwitch.SwitchToLockPage(new ActionEvent(login.getScene().getWindow(), null), "/views/FailedLogin.fxml");
                    } catch (IOException ioException) { ioException.printStackTrace(); }
                }
            }
        });
    }
}