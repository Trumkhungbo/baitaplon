package action;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class StartScence extends Application {
    public static SocketClient client = new SocketClient();
    @Override
    public void start(Stage primaryStage) throws Exception {
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
