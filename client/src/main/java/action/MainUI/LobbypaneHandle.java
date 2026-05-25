package action.MainUI;

import action.Core.SceneSwitch;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import java.io.IOException;

public class LobbypaneHandle {
    @FXML
    public void Clicked(ActionEvent event) throws IOException {
        if (LobbyHandle.getInstance() != null) {
            try {
                LobbyHandle.getInstance().ReturnInvesmentSite(event);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
