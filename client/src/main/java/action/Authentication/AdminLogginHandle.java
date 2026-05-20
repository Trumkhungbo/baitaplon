package action.Authentication;

import action.Core.SceneSwitch;
import action.SocketClient;
import com.google.gson.JsonObject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import java.io.IOException;
import java.time.LocalDate;

public class AdminLogginHandle {
    LocalDate IndependentDay = LocalDate.of(2026,4,30);
    @FXML
    private Button button;
    @FXML
    private DatePicker datePicker;
    SceneSwitch sceneSwitch=new SceneSwitch();
    @FXML
    private Label label;
    public void Submitting(ActionEvent event) throws IOException {
        LocalDate currentDate = datePicker.getValue();
        if(currentDate==null){
            sceneSwitch.SwitchToLockPage(event, "/views/SomeThingUnFill.fxml");
        }
        else{
            if(currentDate.equals(IndependentDay)){
                JsonObject req = new JsonObject();
                req.addProperty("command", "ELEVATE");

                SocketClient.getInstance().requestData(req.toString());
                sceneSwitch.SwitchToAnyWhere(event, "/views/AdminLobby.fxml");
            }
            else{
                label.setText("WRONG!!!!");
            }
        }


    }
    public void ReturnLogin(ActionEvent event) throws IOException {
        sceneSwitch.SwitchToLogin(event);
    }
}
