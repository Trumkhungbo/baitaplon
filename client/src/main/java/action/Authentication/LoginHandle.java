package action.Authentication;

import action.Core.SceneSwitch;
import action.Core.StartScence;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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
    private Label label;
    public void Login(ActionEvent clicked) throws IOException {
        String password1 = pass.getText();
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
                            label.setText("Wrong !");

                        }

                });
            });

            StartScence.client.sendMessage("LOGIN|" + user1 + "|" + password1);
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
}
