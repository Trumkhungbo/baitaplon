package action;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.application.Platform;
import java.io.IOException;
import com.auction.client.network.SocketClient;

public class LoginHandle {
    SceneSwitch sceneSwitch = new SceneSwitch();
    @FXML private TextField login;
    @FXML private PasswordField pass;

    // 1. Hàm này tự chạy khi mở giao diện: Lắp đầu thu lắng nghe Server
    @FXML
    public void initialize() {
        StartScence.client.setServerListener(new SocketClient.ServerListener() {
            @Override
            public void onLoginResult(boolean isSuccess, String message) {
                // Nhận được tin từ luồng ngầm, nhờ Platform đổi giao diện an toàn
                Platform.runLater(() -> {
                    if (isSuccess) {
                        System.out.println("GIAO DIỆN BÁO: Đăng nhập THÀNH CÔNG! -> " + message);
                        // Tương lai Dev 3 sẽ cho code chuyển sang màn hình Danh sách đồ đấu giá vào đây
                    } else {
                        System.out.println("GIAO DIỆN BÁO: Đăng nhập THẤT BẠI! -> " + message);
                    }
                });
            }
        });
    }

    // 2. Chạy khi người dùng bấm nút Login
    public void Login(ActionEvent clicked) throws IOException {
        String password1 = pass.getText();
        String user1 = login.getText();

        if (user1.isEmpty() || password1.isEmpty()) {
            System.out.println("Chưa nhập đủ thông tin!");
            return;
        }

        System.out.println("Đang gửi tài khoản " + user1 + " lên Server để kiểm tra...");
        // Đẩy dữ liệu xuống API mạng của em!
        StartScence.client.sendLogin(user1, password1);
    }

    public void Register(ActionEvent clicked) throws IOException {
        sceneSwitch.SwitchToRegister(clicked);
    }
}