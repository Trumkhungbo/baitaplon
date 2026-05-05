package action;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class LobbyHandle {

    @FXML
    private BorderPane ShowingStage;
    public void ReturnLobby(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Lobbypane.fxml"));
        Parent root = loader.load();
        ShowingStage.setCenter(root);
    }
    public void ReturnInvesmentSite(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/InvesmentSite.fxml"));
        Parent root = loader.load();
        ShowingStage.setCenter(root);
    }
    public void ReturnInvesmentWait(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/InvesmentWait.fxml"));
        Parent root = loader.load();
        ShowingStage.setCenter(root);
    }
    public void ReturnDesiner(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/DesignerHouner.fxml"));
        Parent root = loader.load();
        ShowingStage.setCenter(root);

    }

}
