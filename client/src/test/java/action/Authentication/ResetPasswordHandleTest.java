package action.Authentication;

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
    public void testEmptyFieldsValidation(FxRobot robot) {
        PasswordField newPassword = robot.lookup("#newPassword").queryAs(PasswordField.class);
        PasswordField confirmPassword = robot.lookup("#confirmPassword").queryAs(PasswordField.class);
        Label errorLabel = robot.lookup("#errorLabel").queryAs(Label.class);

        robot.clickOn("Xác nhận");

        assertEquals("Vui lòng điền đầy đủ 2 ô!", errorLabel.getText(), "Phải báo lỗi khi chưa nhập thông tin");

        robot.clickOn(newPassword).write("newpass");
        robot.clickOn("Xác nhận");
        assertEquals("Vui lòng điền đầy đủ 2 ô!", errorLabel.getText(), "Phải báo lỗi khi chỉ nhập 1 ô");
    }

    @Test
    public void testPasswordMismatchValidation(FxRobot robot) {
        PasswordField newPassword = robot.lookup("#newPassword").queryAs(PasswordField.class);
        PasswordField confirmPassword = robot.lookup("#confirmPassword").queryAs(PasswordField.class);
        Label errorLabel = robot.lookup("#errorLabel").queryAs(Label.class);

        robot.clickOn(newPassword).write("newpass123");
        robot.clickOn(confirmPassword).write("differentpass");
        robot.clickOn("Xác nhận");

        assertEquals("Mật khẩu nhập lại không khớp!", errorLabel.getText(), "Phải báo lỗi khi mật khẩu không khớp");
    }

    @Test
    public void testOnDataReceived_ErrorText(FxRobot robot) throws InterruptedException {
        Label errorLabel = robot.lookup("#errorLabel").queryAs(Label.class);

        Platform.runLater(() -> {
            resetPasswordHandle.onDataReceived("ERROR|Lỗi từ máy chủ");
        });

        Thread.sleep(500);
        assertEquals("Lỗi Server: Lỗi từ máy chủ", errorLabel.getText(), "Phải hiển thị lỗi text từ server");
    }

    @Test
    public void testOnDataReceived_FailedJson(FxRobot robot) throws InterruptedException {
        Label errorLabel = robot.lookup("#errorLabel").queryAs(Label.class);

        String failedJson = "{\"status\":\"FAILED\",\"message\":\"Tài khoản không tồn tại\"}";

        Platform.runLater(() -> {
            resetPasswordHandle.onDataReceived(failedJson);
        });

        Thread.sleep(500);
        assertEquals("Tài khoản không tồn tại", errorLabel.getText(), "Phải hiển thị thông báo lỗi từ JSON");
    }

    @Test
    public void testOnDataReceived_SuccessJson_DoesNotCrash() throws InterruptedException {
        String successJson = "{\"status\":\"SUCCESS\"}";

        Platform.runLater(() -> {
            try {
                resetPasswordHandle.onDataReceived(successJson);
            } catch (Exception e) {
                fail("Hàm onDataReceived không được tung Exception khi nhận JSON hợp lệ: " + e.getMessage());
            }
        });

        Thread.sleep(1000); // Chờ UI xử lý popup và chuyển trang
    }

    @Test
    public void testCancelButtonNavigatesToLogin(FxRobot robot) throws InterruptedException {
        robot.clickOn("Hủy / Quay lại");
        Thread.sleep(500);

        // Kiểm tra chuyển hướng về trang đăng nhập bằng cách tìm một phần tử trên giao diện login
        boolean hasLoginBtn = robot.lookup("Đăng nhập").tryQuery().isPresent();
        assertTrue(hasLoginBtn, "Phải chuyển về màn hình đăng nhập (có nút Đăng nhập)");
    }
}
