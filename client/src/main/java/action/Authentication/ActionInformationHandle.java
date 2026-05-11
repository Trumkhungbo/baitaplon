package action.Authentication;

import action.Core.StartScence;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ActionInformationHandle {
    @FXML
    private Label personalID ;
    @FXML
    private Label name ;
    @FXML
    private Label email ;
    @FXML
    private Label phone ;
    @FXML
    private Label password ;
    @FXML
    public void initialize(){
        StartScence.client.sendMessage("GET_ACCOUNTINFORMATION|"+StoreDataInput.username);
        StartScence.client.setServerListener(message -> {
            Platform.runLater(() -> {
                String[] box=message.split("\\|");
                personalID.setText(box[6]);
                email.setText(box[5]);
                phone.setText(box[4]);
                password.setText(box[3]);
                name.setText(box[2]);
            });
       });
    }
}
