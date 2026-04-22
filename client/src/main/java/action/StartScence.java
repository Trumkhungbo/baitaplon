package action;

import com.auction.client.network.SocketClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class StartScence extends Application {
    // Trạm thu phát toàn cục, bất kỳ màn hình nào cũng gọi được StartScence.client
    public static final SocketClient client = new SocketClient();

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Khởi động mạng trước
        client.connect("127.0.0.1", 888);

        // 2. Tải màn hình Đăng nhập
        FXMLLoader fxmloader = new FXMLLoader(StartScence.class.getResource("/login.fxml"));
        Scene scene = new Scene(fxmloader.load());

        primaryStage.setScene(scene);
        primaryStage.setTitle("Hệ thống đấu giá");
        try {
            Image icon = new Image(StartScence.class.getResourceAsStream("/Icon.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.out.println("Không tìm thấy Icon.png");
        }
        primaryStage.show();
    }
}