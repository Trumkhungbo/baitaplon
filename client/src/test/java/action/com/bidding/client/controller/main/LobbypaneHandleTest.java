package action.com.bidding.client.controller.main;

import action.controller.main.LobbypaneHandle;
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
public class LobbypaneHandleTest {

    private LobbypaneHandle controller;

    @Start
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Lobbypane.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void testHeroCardAndButtonPresent(FxRobot robot) {
        assertTrue(robot.lookup("Khám Phá Ngay").tryQuery().isPresent() || robot.lookup(".btn-gold-hover").tryQuery().isPresent(), "Hero card button must exist");
        assertTrue(robot.lookup(".label").tryQuery().isPresent(), "There should be at least one Label in the pane");
    }
}
