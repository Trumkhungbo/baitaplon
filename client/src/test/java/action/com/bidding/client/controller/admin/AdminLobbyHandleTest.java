package action.com.bidding.client.controller.admin;

import action.controller.admin.AdminLobbyHandle;
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
public class AdminLobbyHandleTest {

    private AdminLobbyHandle controller;

    @Start
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AdminLobby.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void testAdminNavButtonsExist(FxRobot robot) throws InterruptedException {
        Thread.sleep(1000);
        assertTrue(robot.lookup("Tài Khoản").tryQuery().isPresent(), "Admin 'Tài Khoản' button should be visible");
        assertTrue(robot.lookup("Yêu Cầu").tryQuery().isPresent(), "Admin 'Yêu Cầu' button should be visible");
        assertTrue(robot.lookup("Hàng Chờ").tryQuery().isPresent(), "Admin 'Hàng Chờ' button should be visible");
    }
}
