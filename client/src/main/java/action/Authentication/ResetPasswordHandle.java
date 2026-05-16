package action.Authentication;

import action.Core.SceneSwitch;
import action.SocketClient;
import action.SocketListener;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
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

    // 1. Kiểm tra Client-side Validation
    if (pass.isBlank() || confirm.isBlank()) {
      errorLabel.setText("Vui lòng điền đầy đủ 2 ô!");
      return;
    }
    if (!pass.equals(confirm)) {
      errorLabel.setText("Mật khẩu nhập lại không khớp!");
      return;
    }

    // 2. Lắng nghe phản hồi từ Server
    String req = "RESET_PASSWORD|" + StoreDataInput.username + "|" + pass;
    SocketClient.getInstance().requestData(req);
  }

  public void Cancel(ActionEvent event) throws IOException {
    sceneSwitch.SwitchToLogin(event);
  }

  @Override
  public void onDataReceived(String data) {
    Platform.runLater(() -> {
      // 1. Kiểm tra xem Server có gửi lỗi dạng Text thuần không
      if (data.startsWith("ERROR|")) {
        errorLabel.setText("Lỗi Server: " + data.replace("ERROR|", ""));
        return;
      }

      try {
        // 2. Cố gắng dịch JSON
        JsonObject res = JsonParser.parseString(data).getAsJsonObject();
        String status = res.get("status").getAsString();
        System.out.println(status);

        if (status.equals("SUCCESS")) {
          try {

            ActionEvent dummyEvent = new ActionEvent(newPassword, null);

            // 1. Hiện PopUp báo Reset thành công (Màn hình này sẽ khóa giao diện cho đến khi user tắt đi)
            sceneSwitch.SwitchToLockPage(dummyEvent, "/views/ResetPasswordPopUp.fxml");

            // 2. Sau khi user tắt PopUp, tự động chuyển họ về màn hình đăng nhập
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