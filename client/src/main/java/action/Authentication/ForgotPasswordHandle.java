package action.Authentication;
import action.Core.SceneSwitch;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;

public class ForgotPasswordHandle {
    @FXML
    public TextField username;
    @FXML
    public TextField Id;
    @FXML
    public TextField phoneNumber;
    public void TakePass(ActionEvent event) throws IOException {
        String Username = username.getText();
        String id = Id.getText();
        String phone = phoneNumber.getText();
        if(Username.isBlank()||id.isBlank()||phone.isBlank()){
            SceneSwitch sceneswitch = new SceneSwitch();
            sceneswitch.SwitchToLockPage(event, "/views/SomeThingUnFill.fxml");
        }
        else if(phone.matches("^[0-9]{10}")){
            SceneSwitch sceneswitch = new SceneSwitch();
            sceneswitch.SwitchToLockPage(event,"/resources/views/WrongInputShow.fxml");
        }
        else if(id.matches("^[0-9]{12}")){
            SceneSwitch sceneswitch = new SceneSwitch();
            sceneswitch.SwitchToLockPage(event,"/views/WrongInputShow.fxml");
        }

    }






}
