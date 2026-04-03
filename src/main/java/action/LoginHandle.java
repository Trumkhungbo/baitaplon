package action;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginHandle {
    @FXML
    private TextField login;
    @FXML
    private PasswordField password;
    public void Login(ActionEvent clicked){
        login.getText();
        password.getText();
    }

}
