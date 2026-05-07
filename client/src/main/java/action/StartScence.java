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
    Stage stage = new Stage();
    stage.setScene(scene);
    stage.setWidth(700);
    stage.setHeight(700);
    stage.show();
    Image icon = new Image(StartScence.class.getResourceAsStream("/icon.png"));
    stage.getIcons().add(icon);
}
