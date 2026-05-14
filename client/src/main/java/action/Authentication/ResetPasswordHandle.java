package action.Authentication;

import action.Core.SceneSwitch;
import action.Core.StartScence;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

import java.io.IOException;

public class ResetPasswordHandle {

  @FXML private PasswordField newPassword;
  @FXML private PasswordField confirmPassword;
  @FXML private Label errorLabel; // Nhãn báo lỗi nếu có

  SceneSwitch sceneSwitch = new SceneSwitch();

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
    StartScence.client.setServerListener(message -> {
      Platform.runLater(() -> {
        // 1. Kiểm tra xem Server có gửi lỗi dạng Text thuần không
        if (message.startsWith("ERROR|")) {
          errorLabel.setText("Lỗi Server: " + message.replace("ERROR|", ""));
          return;
        }

        try {
          // 2. Cố gắng dịch JSON
          JsonObject res = JsonParser.parseString(message).getAsJsonObject();
          String status = res.get("status").getAsString();

          if (status.equals("SUCCESS")) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thành công");
            alert.setHeaderText(null);
            alert.setContentText("Đổi mật khẩu thành công! Mời đăng nhập lại.");
            alert.showAndWait();
            try {
              sceneSwitch.SwitchToLogin(event);
            } catch (IOException e) {
              e.printStackTrace();
            }
          } else {
            String errorMsg = res.get("message").getAsString();
            errorLabel.setText(errorMsg);
          }
        } catch (Exception e) {
          errorLabel.setText("Server phản hồi sai định dạng!");
          System.out.println("Lỗi parse JSON: " + message);
        }
      });
    });

    String req = "RESET_PASSWORD|" + StoreDataInput.username + "|" + pass;
    StartScence.client.sendMessage(req);
  }

  public void Cancel(ActionEvent event) throws IOException {
    sceneSwitch.SwitchToLogin(event);
  }
}