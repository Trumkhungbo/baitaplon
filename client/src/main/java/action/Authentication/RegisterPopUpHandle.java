package action.Authentication;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterPopUpHandle {
    public void ReturnToLogin(ActionEvent clicked) throws IOException {
       Stage stage = (Stage)((Node)clicked.getSource()).getScene().getWindow();
       stage.close();

    }
}
