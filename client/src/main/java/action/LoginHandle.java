package action;

import java.io.IOException;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginHandle {
    @FXML private TextField login;
    @FXML private PasswordField pass;
    private SceneSwitch sceneSwitch = new SceneSwitch();

    @FXML
    public void initialize() {
        StartScence.client.setServerListener(message -> {
            Platform.runLater(() -> {
                if (message.startsWith("LOGIN_SUCCESS")) {
                    System.out.println("Login Success!");
                    try {
                        sceneSwitch.SwitchToLobby(login);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (message.startsWith("ERROR")) {
                    System.out.println("Login Failed: " + message);
                }
            });
        });
    }

    public void Login(ActionEvent clicked) throws IOException {
        String user1 = login.getText();
        String password1 = pass.getText();

        StoreDataInput.username = user1;
        StoreDataInput.password = password1;

        if (user1.isBlank() || password1.isBlank()) {
            sceneSwitch.SwitchToLockPage(clicked, "/SomeThingUnFill.fxml");
        }
        else {
            StartScence.client.sendMessage("LOGIN|" + user1 + "|" + password1);
        }
    }

    public void Register(ActionEvent clicked) throws IOException {
        sceneSwitch.SwitchToRegister(clicked);
    }

    public void ForgotPassword(ActionEvent clicked) throws IOException {
        sceneSwitch.SwitchToAnyWhere(clicked, "/ForgotPassword.fxml");
    }

    public void Adminloggin(ActionEvent clicked) throws IOException {
        sceneSwitch.SwitchToAnyWhere(clicked, "/AdminLoggin.fxml");
    }
}
