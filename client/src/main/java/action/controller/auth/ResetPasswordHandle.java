package action.controller.auth;

import action.Core.SceneSwitch;
import action.model.StoreDataInput;
import action.network.SocketClient;
import action.network.SocketListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

import java.io.IOException;

public class ResetPasswordHandle implements SocketListener {

  @FXML private PasswordField newPassword;
  @FXML private PasswordField confirmPassword;
  @FXML private Label errorLabel; // Nhãn báo lỗi nếu có

  SceneSwitch sceneSwitch = new SceneSwitch();

  @FXML
  public void initialize() {
    SocketClient.getInstance().addListener(this);
  }

  public void ConfirmReset(ActionEvent event) {
    String pass = newPassword.getText();
    String confirm = confirmPassword.getText();

    if (pass.isBlank() || confirm.isBlank()) {
      errorLabel.setText("Vui lòng điền đầy đủ 2 ô!");
      return;
    }
    if (!pass.equals(confirm)) {
      errorLabel.setText("Mật khẩu nhập lại không khớp!");
      return;
    }

    JsonObject req = new JsonObject();
    req.addProperty("command", "RESET_PASSWORD");
    req.addProperty("username", StoreDataInput.username);
    req.addProperty("newPassword", pass);
    SocketClient.getInstance().requestData(req.toString());
  }

  public void Cancel(ActionEvent event) throws IOException {
    SocketClient.getInstance().removeListener(this);
    sceneSwitch.SwitchToLogin(event);
  }

  @Override
  public void onDataReceived(String data) {
    Platform.runLater(() -> {
      if (data.startsWith("ERROR|")) {
        errorLabel.setText("Lỗi Server: " + data.replace("ERROR|", ""));
        return;
      }

      try {
        JsonObject res = JsonParser.parseString(data).getAsJsonObject();
        String status = res.get("status").getAsString();
        System.out.println(status);

        if (status.equals("SUCCESS")) {
          try {

            ActionEvent dummyEvent = new ActionEvent(newPassword, null);

            SocketClient.getInstance().removeListener(this);
            sceneSwitch.SwitchToLockPage(dummyEvent, "/views/ResetPasswordPopUp.fxml");

            sceneSwitch.SwitchToLogin(dummyEvent);
          } catch (IOException e) {
            e.printStackTrace();
          }
        } else {
          String errorMsg = res.get("message").getAsString();
          errorLabel.setText(errorMsg);
        }
      } catch (Exception e) {
        errorLabel.setText("Server phản hồi sai định dạng!");
        System.out.println("Lỗi parse JSON: " + data);
      }
    });
  }
}