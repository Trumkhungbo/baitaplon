package action.Core;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.stage.Window;

import java.io.IOException;

public class SceneSwitch {
    private Stage stage;
    private Scene scene;

    /**
     * Hàm helper thông minh: Tự động phân tích ActionEvent để lấy ra Stage hiện tại.
     * Chấp nhận cả trường hợp source là Node (Button, TextField...) hoặc trực tiếp là Window/Stage.
     */
    private Stage getStageFromEvent(ActionEvent event) {
        Object source = event.getSource();
        if (source instanceof Node) {
            return (Stage) ((Node) source).getScene().getWindow();
        } else if (source instanceof Window) {
            return (Stage) source;
        }
        throw new IllegalArgumentException("Event source không hợp lệ, phải là Node hoặc Window!");
    }

    public void SwitchToLobby(TextField loginField) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/views/Lobby.fxml"));
        stage = (Stage) loginField.getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    public void SwitchToRegister(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/views/register.fxml"));
        stage = getStageFromEvent(event); // Đã thay thế
        scene = new Scene(root);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    public void SwitchToLogin(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/views/login.fxml"));
        stage = getStageFromEvent(event); // Đã thay thế
        scene = new Scene(root);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    public void SwitchToAnyWhere(ActionEvent event, String FXML) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(FXML));
        stage = getStageFromEvent(event); // Đã thay thế
        scene = new Scene(root);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    public void SwitchToLockPage(ActionEvent event, String FXML) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(FXML));

        // Tạo một popup Stage mới (Modal)
        Stage modalStage = new Stage();
        scene = new Scene(root);
        modalStage.setScene(scene);
        modalStage.centerOnScreen();
        modalStage.initModality(Modality.APPLICATION_MODAL);

        // Thêm icon an toàn
        try {
            Image icon = new Image(getClass().getResourceAsStream("/assets/Icon.png"));
            modalStage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Không tìm thấy tệp icon: " + e.getMessage());
        }

        modalStage.showAndWait();
    }
}