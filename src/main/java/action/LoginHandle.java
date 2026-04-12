package action;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginHandle {
    SceneSwitch sceneSwitch = new SceneSwitch();
    @FXML
    private TextField login;
    @FXML
    private PasswordField pass;
    public void Login(ActionEvent clicked) throws IOException {
        String password1 = pass.getText();
        String user1 = login.getText();
        StoreDataInput.username=user1;
        StoreDataInput.password=password1;
        if (password1.isBlank() || user1.isBlank()) {
            sceneSwitch.SwitchToLockPage(clicked,"/SomeThingUnFill.fxml");

        }


    }
    public void Register(ActionEvent clicked) throws IOException {
        sceneSwitch.SwitchToRegister(clicked);
    }
    public void ForgotPassword(ActionEvent clicked) throws IOException {
        sceneSwitch.SwitchToAnyWhere(clicked,"/ForgotPassword.fxml");
    }
}
