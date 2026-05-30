package action.com.bidding.client.controller.auth;

import action.controller.auth.ResetPasswordHandle;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class ResetPasswordHandleTest {

    private ResetPasswordHandle resetPasswordHandle;

    @Start
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ResetPassword.fxml"));
        Parent root = loader.load();
        resetPasswordHandle = loader.getController();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void testEmptyFieldsShowError(FxRobot robot) {
        robot.clickOn("Xác nhận");

        Label errorLabel = robot.lookup("#errorLabel").queryAs(Label.class);
        assertEquals("Vui lòng điền đầy đủ 2 ô!", errorLabel.getText(), "Nếu không nhập gì, phải báo lỗi.");
    }

    @Test
    public void testMismatchedPasswordsShowError(FxRobot robot) {
        PasswordField newPassword = robot.lookup("#newPassword").queryAs(PasswordField.class);
        PasswordField confirmPassword = robot.lookup("#confirmPassword").queryAs(PasswordField.class);

        robot.clickOn(newPassword).write("newpass123");
        robot.clickOn(confirmPassword).write("differentpass");

        robot.clickOn("Xác nhận");

        Label errorLabel = robot.lookup("#errorLabel").queryAs(Label.class);
        assertEquals("Mật khẩu nhập lại không khớp!", errorLabel.getText(), "Nếu mật khẩu khác nhau, phải báo lỗi.");
    }

    @Test
    public void testValidInputDoesNotCrash(FxRobot robot) {
        PasswordField newPassword = robot.lookup("#newPassword").queryAs(PasswordField.class);
        PasswordField confirmPassword = robot.lookup("#confirmPassword").queryAs(PasswordField.class);

        robot.clickOn(newPassword).write("newpass123");
        robot.clickOn(confirmPassword).write("newpass123");

        assertDoesNotThrow(() -> robot.clickOn("Xác nhận"), "Chạy phương thức ConfirmReset() không được gây crash.");
    }

    @Test
    public void testCancelButtonNavigate(FxRobot robot) throws InterruptedException {
        assertDoesNotThrow(() -> robot.clickOn("Hủy / Quay lại"));
        // Đợi một khoảng để SceneSwitch thực thi
        Thread.sleep(500);
    }

    @Test
    public void testOnDataReceived_ErrorShowsOnLabel(FxRobot robot) throws InterruptedException {
        String errorData = "ERROR|Lỗi từ server";

        Platform.runLater(() -> {
            resetPasswordHandle.onDataReceived(errorData);
        });

        Thread.sleep(500);

        Label errorLabel = robot.lookup("#errorLabel").queryAs(Label.class);
        assertEquals("Lỗi Server: Lỗi từ server", errorLabel.getText());
    }

    @Test
    public void testOnDataReceived_FailedStatusShowsMessage(FxRobot robot) throws InterruptedException {
        String failedJson = "{\"status\":\"FAILED\",\"message\":\"Tài khoản không tồn tại\"}";

        Platform.runLater(() -> {
            resetPasswordHandle.onDataReceived(failedJson);
        });

        Thread.sleep(500);

        Label errorLabel = robot.lookup("#errorLabel").queryAs(Label.class);
        assertEquals("Tài khoản không tồn tại", errorLabel.getText());
    }

    @Test
    public void testOnDataReceived_Success() throws InterruptedException {
        String successJson = "{\"status\":\"SUCCESS\"}";

        Platform.runLater(() -> {
            resetPasswordHandle.onDataReceived(successJson);
        });

        Thread.sleep(500);
        // Kiểm tra cơ bản xem parse JSON success có bị crash không
    }
}
