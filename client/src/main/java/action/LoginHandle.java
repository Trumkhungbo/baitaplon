package action;

import com.auction.client.network.SocketClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;

public class LoginHandle {
    @FXML private TextField login;
    @FXML private PasswordField pass;
    private SceneSwitch sceneSwitch = new SceneSwitch();

    @FXML
    public void initialize() {
        // Cài đặt "tai nghe" để bắt kết quả từ Server
        StartScence.client.setServerListener(message -> {
            // QUAN TRỌNG: Mạng chạy luồng phụ, Giao diện chạy luồng chính.
            // Phải dùng Platform.runLater để nhờ UI đổi trang an toàn.
            Platform.runLater(() -> {
                if (message.startsWith("LOGIN_SUCCESS")) {
                    System.out.println("Đăng nhập thành công, chuẩn bị chuyển trang...");
                    try {
                        // Gọi hàm chuyển sang Lobby.fxml của Dev UI
                        sceneSwitch.SwitchToLobby(login);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (message.startsWith("ERROR")) {
                    System.out.println("Đăng nhập thất bại: " + message);
                    // Ở đây em có thể gọi code hiện Pop-up báo sai mật khẩu
                }
            });
        });
    }

    public void Login(ActionEvent clicked) throws IOException {
        String username = login.getText();
        String password = pass.getText();

        if (!username.isEmpty() && !password.isEmpty()) {
            // Đẩy lệnh xuống mạng
            StartScence.client.sendMessage("LOGIN|" + username + "|" + password);
        }
    }
    @FXML
    public void ForgotPassword(ActionEvent event) throws IOException {
        System.out.println("Đang mở trang Quên Mật Khẩu...");
        // Gọi hàm SwitchToAnyWhere có sẵn của em để mở giao diện có video con mèo
        // (Lưu ý: Đổi chữ "/ForgotPassword.fxml" thành tên file FXML chính xác của em nếu khác nhé)
        sceneSwitch.SwitchToAnyWhere(event, "/ForgotPassword.fxml");
    }
    @FXML
    public void Register(ActionEvent event) throws IOException {
        System.out.println("Đang chuyển sang trang Đăng ký...");
        // Gọi hàm SwitchToRegister có sẵn trong SceneSwitch của em
        sceneSwitch.SwitchToRegister(event);
    }
}