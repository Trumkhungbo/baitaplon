package action.Core;

import action.SocketClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class StartScence extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        SocketClient.getInstance().connect("127.0.0.1", 888);
        FXMLLoader fxmloader = new FXMLLoader(StartScence.class.getResource("/views/login.fxml"));
        Scene scene = new Scene(fxmloader.load());
        Stage stage = new Stage();
        stage.setScene(scene);
        scene.getStylesheets().add(getClass().getResource("/views/global.css").toExternalForm());
        stage.show();
        Image icon = new Image(StartScence.class.getResourceAsStream("/assets/Icon.png"));
        stage.getIcons().add(icon);
    }

    @Override
    public void stop() throws Exception {
        System.exit(0);
    }
}