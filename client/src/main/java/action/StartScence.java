package action;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class StartScence extends Application {
    public static Client client;
    @Override
    public void start(Stage primaryStage) throws Exception {
    FXMLLoader fxmloader = new FXMLLoader(StartScence.class.getResource("/views/login.fxml"));
    Scene scene = new Scene(fxmloader.load());
    Stage stage = new Stage();
    stage.setScene(scene);
    stage.setWidth(700);
    stage.setHeight(700);
    stage.show();
    Image icon = new Image(StartScence.class.getResourceAsStream("/assets/Icon.png"));
    stage.getIcons().add(icon);
}

        // Sửa lại đoạn này ở cuối file của bạn
    public static class Client { // Chuyển thành public để class khác dùng được

        public Client() {
        }

        // Thêm hàm này vào để hết lỗi "cannot find symbol"
        public void sendMessage(String message) {
            // Tạm thời để trống hoặc in ra console để kiểm tra
            System.out.println("Gửi dữ liệu: " + message);
            
            // Sau này bạn sẽ thêm logic kết nối Socket vào đây
        }
    }

    }

