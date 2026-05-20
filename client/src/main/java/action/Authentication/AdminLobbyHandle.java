package action.Authentication;

import action.SocketClient;
import action.SocketListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminLobbyHandle implements Initializable {
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            MovingCenter("/views/AdminAccountPage.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @FXML
    public BorderPane ShowingStage;

    public void MovingCenter(String URL) throws IOException {
        if (ShowingStage.getCenter() != null) {
            Object oldController = ShowingStage.getCenter().getUserData();
            if (oldController instanceof SocketListener) {
                SocketClient.getInstance().removeListener((SocketListener) oldController);
            }
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource(URL));
        Parent root = loader.load();
        root.setUserData(loader.getController());
        ShowingStage.setCenter(root);
    }

    public void MovingToAccountPage(ActionEvent event) throws IOException {
        MovingCenter("/views/AdminAccountPage.fxml");
    }
    public void MovingToHandleUserRequest(ActionEvent event) throws IOException {
        MovingCenter("/views/AdminHandleUserRequestPage.fxml");
    }
    public void MovingToSellingPage(ActionEvent event) throws IOException {
        MovingCenter("/views/AdminSellingPage.fxml");
    }
}