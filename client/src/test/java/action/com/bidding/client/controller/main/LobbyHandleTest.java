package action.com.bidding.client.controller.main;

import action.controller.main.LobbyHandle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class LobbyHandleTest {

    private LobbyHandle controller;

    @Start
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Lobby.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void testNavigationButtonsPresent(FxRobot robot) {
        assertTrue(robot.lookup("#navHomeButton").tryQuery().isPresent(), "Nav Home button must exist");
        assertTrue(robot.lookup("#navAboutButton").tryQuery().isPresent(), "Nav About button must exist");
        assertTrue(robot.lookup("#navSellerButton").tryQuery().isPresent(), "Nav Seller button must exist");
        assertTrue(robot.lookup("#navAuctionButton").tryQuery().isPresent(), "Nav Auction button must exist");
    }
}
