package action.com.bidding.client.controller.auth;

import action.controller.auth.LoginHandle;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class LoginHandleTest {

    private LoginHandle loginHandle;

    @Start
    public void start(Stage stage) throws Exception {
        // 1. Tải màn hình login.fxml lên để TestFX bắt đầu giả lập click
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
        Parent root = loader.load();
        loginHandle = loader.getController();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void testTogglePasswordVisibility(FxRobot robot) {
        PasswordField passField = robot.lookup("#pass").queryAs(PasswordField.class);
        TextField passVisibleField = robot.lookup("#passVisible").queryAs(TextField.class);
        Button toggleBtn = robot.lookup("#togglePasswordButton").queryAs(Button.class);

        robot.clickOn(passField).write("123456");

        assertEquals("123456", passField.getText());
        assertFalse(passVisibleField.isVisible(), "Mặc định ô pass text phải bị ẩn");

        robot.clickOn(toggleBtn);

        assertTrue(passVisibleField.isVisible(), "Sau khi ấn nút, ô text phải hiện");
        assertFalse(passField.isVisible(), "Sau khi ấn nút, ô mã hóa phải ẩn đi");
        assertEquals("123456", passVisibleField.getText(), "Mật khẩu bên ô text phải khớp");

        robot.clickOn(toggleBtn);
        assertFalse(passVisibleField.isVisible(), "Phải ẩn lại ô text");
        assertTrue(passField.isVisible(), "Phải hiện lại ô mã hóa");
    }

    @Test
    public void testOnDataReceived_Success_DoesNotCrash() throws InterruptedException {
        String successJson = "{\"command\":\"LOGIN_RESULT\",\"status\":\"SUCCESS\"}";

        Platform.runLater(() -> {
            try {
                loginHandle.onDataReceived(successJson);
            } catch (Exception e) {
                fail("Hàm onDataReceived không được tung Exception khi nhận JSON hợp lệ: " + e.getMessage());
            }
        });

        // Đợi 1 chút cho UI kịp xử lý việc chuyển màn hình (SceneSwitch)
        Thread.sleep(500);

    }

    @Test
    public void testOnDataReceived_Failed_DoesNotCrash() throws InterruptedException {
        String failedJson = "{\"command\":\"LOGIN_RESULT\",\"status\":\"FAILED\"}";

        Platform.runLater(() -> {
            try {
                loginHandle.onDataReceived(failedJson);
            } catch (Exception e) {
                fail("Hàm onDataReceived không được tung Exception khi nhận JSON thất bại.");
            }
        });

        Thread.sleep(500);
    }
    @Test
    public void testSuccessfulLoginNavigatesToLobby(FxRobot robot) throws InterruptedException {
        String successJson = "{\"command\":\"LOGIN_RESULT\",\"status\":\"SUCCESS\"}";

        Platform.runLater(() -> {
            loginHandle.onDataReceived(successJson);
        });

        Thread.sleep(1000);

        boolean hasLobbyNavbar = robot.lookup("Trang Chủ").tryQuery().isPresent();

        assertTrue(hasLobbyNavbar, "Màn hình phải được chuyển sang Lobby (Xuất hiện nút Trang Chủ) sau khi Server báo SUCCESS.");
    }
    @Test
    public void testNavigateToForgotPassword(FxRobot robot) throws InterruptedException {
        robot.clickOn("Quên mật khẩu?");

        Thread.sleep(500);

        boolean hasQuestionMark = robot.lookup("?").tryQuery().isPresent();
        assertTrue(hasQuestionMark, "Phải chuyển sang màn hình Quên Mật Khẩu (có chứa dấu ?)");
    }


    @Test
    public void testNavigateToRegister(FxRobot robot) throws InterruptedException {
        robot.clickOn("Đăng ký");

        Thread.sleep(500);

        boolean hasRegisterBtn = robot.lookup(".btn-gold").tryQuery().isPresent();
        assertTrue(hasRegisterBtn, "Phải chuyển sang giao diện Đăng ký thành công.");
    }

    @Test
    public void testNavigateToAdminLogin(FxRobot robot) throws InterruptedException {
        robot.clickOn("Admin Access");

        Thread.sleep(500);

        boolean hasAdminLabel = robot.lookup("ADMIN").tryQuery().isPresent();
        assertTrue(hasAdminLabel, "Phải chuyển sang trang đăng nhập của Admin (Có chữ ADMIN).");
    }

}
