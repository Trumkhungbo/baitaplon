package action;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class StartScence extends Application {
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
}}
