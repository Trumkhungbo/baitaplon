package action.Authentication;

import action.Core.SceneSwitch;
import action.Core.StartScence;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
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
        JsonObject req = new JsonObject();
        req.addProperty("command", "GET_ACCOUNTINFORMATION");
        req.addProperty("username", StoreDataInput.username);
        StartScence.client.sendMessage(req.toString());

        StartScence.client.setServerListener(message -> {
            Platform.runLater(() -> {
                try {
                    // Dùng máy quét JSON để đọc tin nhắn
                    JsonObject res = JsonParser.parseString(message).getAsJsonObject();

                    // Nếu đúng là nhãn dán ACCOUNT_INFO thì mới bóc quà
                    if (res.get("command").getAsString().equals("ACCOUNT_INFO")) {
                        name.setText(res.get("username").getAsString());
                        password.setText(res.get("password").getAsString());
                        phone.setText(res.get("phone").getAsString());
                        email.setText(res.get("email").getAsString());
                        personalID.setText(res.get("personalID").getAsString());
                        money.setText(res.get("balance").getAsString()); // Hiện BALANCE=0.0
                    }
                } catch (Exception e) {
                    // Mọi lỗi định dạng sẽ bị bỏ qua
                }
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
            JsonObject addReq = new JsonObject();
            addReq.addProperty("command", "ADD_MONEY");
            addReq.addProperty("username", StoreDataInput.username);
            addReq.addProperty("money", moneyVao);
            StartScence.client.sendMessage(addReq.toString());

            StartScence.client.setServerListener(message -> {
                Platform.runLater(() -> {
                    try {
                        // Bóc hộp quà JSON
                        JsonObject res = JsonParser.parseString(message).getAsJsonObject();
                        // Nếu đúng là nhãn MONEY_UPDATE
                        if (res.has("command") && res.get("command").getAsString().equals("MONEY_UPDATE")) {
                            // Lấy số dư mới và cập nhật lên Label
                            money.setText(res.get("balance").getAsString());
                            moneyIn.clear();
                        }
                    } catch (Exception e) {}
                });
            });
        }
    }
  public void ReturnToLogin(ActionEvent event) throws IOException {
    SceneSwitch sceneSwitch = new SceneSwitch();
    sceneSwitch.SwitchToLogin(event);
  }
}
