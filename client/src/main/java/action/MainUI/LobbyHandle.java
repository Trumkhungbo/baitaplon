package action.MainUI;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LobbyHandle implements Initializable {

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            MovingCenter("/views/Lobbypane.fxml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static LobbyHandle instance;

    public LobbyHandle() {
        instance = this;
    }

    public static LobbyHandle getInstance() {
        return instance;
    }

    @FXML
    public BorderPane ShowingStage;
    public void MovingCenter(String URL) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(URL));
        Parent root = loader.load();
        root.prefHeight(500);
        root.prefWidth(500);
        ShowingStage.setCenter(root);
    }
    public void ReturnLobby(ActionEvent event) throws IOException {
        MovingCenter("/views/Lobbypane.fxml");
    }
    public void ReturnInvesmentSite(ActionEvent event) throws IOException {
        MovingCenter("/views/InvesmentSite.fxml");
    }
    public void ReturnInvesmentWait(ActionEvent event) throws IOException {
       MovingCenter(("/views/InvesmentWait.fxml"));
    }
    public void ReturnDesiner(ActionEvent event) throws IOException {
        MovingCenter("/views/DesignerHouner.fxml");

    }
    public void ReturnSeller(ActionEvent event) throws IOException {
        MovingCenter("/views/InvesterSell.fxml");}
    public void AccountInformation(ActionEvent event) throws IOException {
        MovingCenter("/views/AccountInformation.fxml");
    }
    public void ItemShowing() throws IOException {
        MovingCenter("/views/ItemShowing.fxml");
    }
}
