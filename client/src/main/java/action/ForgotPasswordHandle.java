package action;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.scene.input.KeyCombination;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class ForgotPasswordHandle implements Initializable {
    @FXML
    private MediaView mediaView;
    private File file;
    private Media media;
    private MediaPlayer mediaPlayer;
    private Stage stage;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        file = new File("src/main/resources/memecat.mp4");
        media = new Media(file.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);
        mediaPlayer.play();




}}
