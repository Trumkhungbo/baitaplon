package action.MainUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class LobbyHandle {

    @FXML
    private BorderPane ShowingStage;
    public void ReturnLobby(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Lobbypane.fxml"));
        Parent root = loader.load();
        root.prefWidth(500);
        root.prefHeight(500);
        ShowingStage.setCenter(root);
    }
    public void ReturnInvesmentSite(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/InvesmentSite.fxml"));
        Parent root = loader.load();
        root.prefWidth(500);
        root.prefHeight(500);
        ShowingStage.setCenter(root);
    }
    public void ReturnInvesmentWait(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/InvesmentWait.fxml"));
        Parent root = loader.load();
        root.prefWidth(500);
        root.prefHeight(500);
        ShowingStage.setCenter(root);
    }
    public void ReturnDesiner(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DesignerHouner.fxml"));
        Parent root = loader.load();
        root.prefWidth(500);
        root.prefHeight(500);
        ShowingStage.setCenter(root);

    }
    public void ReturnSeller(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/InvesterSell.fxml"));
        Parent root = loader.load();
        root.prefWidth(500);
        root.prefHeight(500);
        ShowingStage.setCenter(root);
    }
    public void AccountInformation(ActionEvent event) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AccountInformation.fxml"));
        Parent root = loader.load();
        root.prefWidth(500);
        root.prefHeight(500);
        ShowingStage.setCenter(root);
    }

}
