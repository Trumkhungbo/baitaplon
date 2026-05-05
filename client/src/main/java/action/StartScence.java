package action;

import com.auction.client.network.SocketClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class StartScence extends Application {

    public static final SocketClient client = new SocketClient();

    @Override
    public void start(Stage primaryStage) throws Exception {
        client.connect("127.0.0.1", 888);

        FXMLLoader fxmloader = new FXMLLoader(StartScence.class.getResource("/login.fxml"));
        Scene scene = new Scene(fxmloader.load());

        primaryStage.setScene(scene);
        primaryStage.setTitle("Hệ thống đấu giá");
        try {
            Image icon = new Image(StartScence.class.getResourceAsStream("/Icon.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.out.println("Undiscovered Icon.png");
        }
        primaryStage.show();
    }
}