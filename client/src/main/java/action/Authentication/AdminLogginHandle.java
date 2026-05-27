package action.Authentication;

import action.Core.SceneSwitch;
import action.SocketClient;
import action.SocketListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminLogginHandle implements Initializable, SocketListener {

    @FXML private Button button;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label label;

    private final SceneSwitch sceneSwitch = new SceneSwitch();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SocketClient.getInstance().addListener(this);
    }

    public void Submitting(ActionEvent event) throws IOException {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            sceneSwitch.SwitchToLockPage(event, "/views/SomeThingUnFill.fxml");
            return;
        }

        StoreDataInput.username = username;
        StoreDataInput.password = password;

        JsonObject req = new JsonObject();
        req.addProperty("command", "LOGIN");
        req.addProperty("username", username);
        req.addProperty("password", password);
        SocketClient.getInstance().requestData(req.toString());
        label.setText("Đang đăng nhập admin...");
    }

    public void ReturnLogin(ActionEvent event) throws IOException {
        SocketClient.getInstance().removeListener(this);
        sceneSwitch.SwitchToLogin(event);
    }

    @Override
    public void onDataReceived(String data) {
        Platform.runLater(() -> {
            try {
                JsonObject response = JsonParser.parseString(data).getAsJsonObject();
                if (!response.has("command")
                        || !"LOGIN_RESULT".equals(response.get("command").getAsString())) {
                    return;
                }

                String status = getAsString(response, "status");
                String role = getAsString(response, "role");
                if ("SUCCESS".equalsIgnoreCase(status)
                        && "ADMIN".equalsIgnoreCase(role)) {
                    SocketClient.getInstance().removeListener(this);
                    sceneSwitch.SwitchToAnyWhere(
                            new ActionEvent(usernameField.getScene().getWindow(), null),
                            "/views/AdminLobby.fxml"
                    );
                    return;
                }

                if ("SUCCESS".equalsIgnoreCase(status)) {
                    label.setText("Tài khoản này không có quyền admin.");
                } else {
                    label.setText("Sai tài khoản hoặc mật khẩu admin.");
                }
            } catch (Exception ignored) {
                if (data.startsWith("ERROR|")) {
                    label.setText(data.substring("ERROR|".length()));
                }
            }
        });
    }

    private String getAsString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString()
                : "";
    }
}
