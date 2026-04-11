package action;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.auction.client.network.SocketClient; // Import mạng vào

public class StartScence extends Application {

    // Khai báo mạng toàn cục
    public static SocketClient client = new SocketClient();

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Vừa bật app là tự động kết nối ngầm tới Server luôn!
        client.connect();

        FXMLLoader fxmloader = new FXMLLoader(StartScence.class.getResource("/login.fxml"));
        Scene scene = new Scene((Parent)fxmloader.load());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Đấu Giá Client - Đã nối mạng!");
        primaryStage.show();

        try {
            Image icon = new Image(StartScence.class.getResourceAsStream("/icon.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {}
    }
}