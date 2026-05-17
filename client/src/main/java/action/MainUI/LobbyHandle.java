package action.MainUI;

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
        // Kiểm tra xem Controller của bảng cũ có implements SocketListener không, nếu có thì gỡ luôn!
        if (ShowingStage.getCenter() != null) {
            Object oldController = ShowingStage.getCenter().getUserData();
            if (oldController instanceof SocketListener) {
                SocketClient.getInstance().removeListener((SocketListener) oldController);
            }
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource(URL));
        Parent root = loader.load();

        // Gắn chính xác Controller hiện tại vào UserData của Node để hàm vòng sau có thể lôi ra xóa
        root.setUserData(loader.getController());
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
