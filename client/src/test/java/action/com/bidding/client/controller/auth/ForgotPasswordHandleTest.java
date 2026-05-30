package action.com.bidding.client.controller.auth;

import action.controller.auth.ForgotPasswordHandle;
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
public class ForgotPasswordHandleTest {

    private ForgotPasswordHandle controller;

    @Start
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ForgotPassword.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void testFieldsAndButtons(FxRobot robot) {
        assertTrue(robot.lookup("#username").tryQuery().isPresent(), "Username field must exist");
        assertTrue(robot.lookup("#Id").tryQuery().isPresent(), "ID field must exist");
        assertTrue(robot.lookup("Lấy lại mật khẩu").tryQuery().isPresent(), "Submit button should be present");
    }
}
