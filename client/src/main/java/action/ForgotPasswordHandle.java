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
        try {
            URL videoUrl = getClass().getResource("/memecat.mp4");

            if (videoUrl == null) {
                System.err.println("Báo động đỏ: Không tìm thấy memecat.mp4 trong thư mục client/src/main/resources!");
                return;
            }

            media = new Media(videoUrl.toExternalForm());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.play();

        } catch (Exception e) {
            System.err.println("Lỗi phát video: " + e.getMessage());
        }
    }}
