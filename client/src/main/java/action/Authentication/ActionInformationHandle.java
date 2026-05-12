package action.Authentication;

import action.Core.SceneSwitch;
import action.Core.StartScence;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

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
    private Label money;
    @FXML
    private TextField moneyIn ;
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
                money.setText(box[7]);
            });
       });
    }

    public void addMoney(ActionEvent event) throws IOException {
        String moneyVao =  moneyIn.getText();
        if(moneyVao.isEmpty()){
            SceneSwitch sceneSwitch = new SceneSwitch();
            sceneSwitch.SwitchToLockPage(event,"/views/SomeThingUnFill.fxml");
        }
        else if(!moneyVao.matches("\\d+(\\.\\d{1,2})?")){
            SceneSwitch sceneSwitch = new SceneSwitch();
            sceneSwitch.SwitchToLockPage(event,"/views/WrongInputShow.fxml");
        }
        else{
        StartScence.client.sendMessage("ADD_MONEY|"+StoreDataInput.username+"|"+moneyVao);
        StartScence.client.setServerListener(message -> {
            Platform.runLater(() -> {
                String[] box = message.split("\\|");
                money.setText(box[1]);
            });
        });

    }}public void ReturnToLogin(ActionEvent event) throws IOException {
        SceneSwitch sceneSwitch = new SceneSwitch();
        sceneSwitch.SwitchToLogin(event);
    }
}
