package action;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import java.io.IOException;
import java.time.LocalDate;

public class AdminLogginHandle {
    LocalDate IndependentDay = LocalDate.of(1975,4,30);
    @FXML
    private Button button;
    @FXML
    private DatePicker datePicker;
    SceneSwitch sceneSwitch=new SceneSwitch();
    @FXML
    private Label label;
    public void Submitting(ActionEvent event) throws IOException {
        if(datePicker.getValue()==null){
            sceneSwitch.SwitchToLockPage(event,"/SomeThingUnFill.fxml");
        }
        else{
            if(datePicker.getValue().equals(IndependentDay)){
                sceneSwitch.SwitchToAnyWhere(event,"/Adminpage.fxml");
            }
            else{
                label.setText("");
                label.setText("WRONG!!!!");
            }

        }


    }
    public void ReturnLogin(ActionEvent event) throws IOException {
        sceneSwitch.SwitchToLogin(event);
    }
}
