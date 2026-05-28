package action.Authentication;

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
        // Lấy các thành phần UI thông qua ID trong file FXML (#id)
        PasswordField passField = robot.lookup("#pass").queryAs(PasswordField.class);
        TextField passVisibleField = robot.lookup("#passVisible").queryAs(TextField.class);
        Button toggleBtn = robot.lookup("#togglePasswordButton").queryAs(Button.class);

        // 1. Gõ thử mật khẩu "123456" vào ô PasswordField (chế độ ẩn)
        robot.clickOn(passField).write("123456");

        // Xác nhận mật khẩu đã được nhập và ô nhìn thấy được đang bị tắt
        assertEquals("123456", passField.getText());
        assertFalse(passVisibleField.isVisible(), "Mặc định ô pass text phải bị ẩn");

        // 2. Click vào nút "Con mắt"
        robot.clickOn(toggleBtn);

        // Xác nhận: Ô chữ phải hiện lên, ô chấm đen phải biến mất
        assertTrue(passVisibleField.isVisible(), "Sau khi ấn nút, ô text phải hiện");
        assertFalse(passField.isVisible(), "Sau khi ấn nút, ô mã hóa phải ẩn đi");
        assertEquals("123456", passVisibleField.getText(), "Mật khẩu bên ô text phải khớp");

        // 3. Click lại nút "Con mắt" lần nữa để tắt
        robot.clickOn(toggleBtn);
        assertFalse(passVisibleField.isVisible(), "Phải ẩn lại ô text");
        assertTrue(passField.isVisible(), "Phải hiện lại ô mã hóa");
    }

    @Test
    public void testOnDataReceived_Success_DoesNotCrash() throws InterruptedException {
        // Bỏ qua Mockito, ta giả lập việc Server trả về 1 chuỗi JSON thành công
        String successJson = "{\"command\":\"LOGIN_RESULT\",\"status\":\"SUCCESS\"}";

        // Chạy hàm onDataReceived trên luồng giao diện (JavaFX Thread)
        Platform.runLater(() -> {
            try {
                loginHandle.onDataReceived(successJson);
            } catch (Exception e) {
                fail("Hàm onDataReceived không được tung Exception khi nhận JSON hợp lệ: " + e.getMessage());
            }
        });

        // Đợi 1 chút cho UI kịp xử lý việc chuyển màn hình (SceneSwitch)
        Thread.sleep(500);

        // Vì SceneSwitch chuyển sang màn hình Lobby.fxml mới, ta chỉ cần đảm bảo
        // hàm xử lý logic không bị Crash/Exception là đã Test qua luồng thành công.
    }

    @Test
    public void testOnDataReceived_Failed_DoesNotCrash() throws InterruptedException {
        // Giả lập Server báo sai tài khoản mật khẩu
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
        // Giả lập Server phản hồi đăng nhập thành công
        String successJson = "{\"command\":\"LOGIN_RESULT\",\"status\":\"SUCCESS\"}";

        // Nạp cục JSON giả kia vào hàm nhận Data của LoginHandle
        Platform.runLater(() -> {
            loginHandle.onDataReceived(successJson);
        });

        // Đợi 1 giây để hệ thống load file Lobby.fxml
        Thread.sleep(1000);

        // KIỂM CHỨNG: Tìm xem trên giao diện hiện tại đã xuất hiện nút "Trang Chủ" chưa?
        // (Trong file Lobby.fxml của bạn có 1 ToggleButton tên là "Trang Chủ")
        boolean hasLobbyNavbar = robot.lookup("Trang Chủ").tryQuery().isPresent();

        assertTrue(hasLobbyNavbar, "Màn hình phải được chuyển sang Lobby (Xuất hiện nút Trang Chủ) sau khi Server báo SUCCESS.");
    }
    @Test
    public void testNavigateToForgotPassword(FxRobot robot) throws InterruptedException {
        // Tìm và click vào nút "Quên mật khẩu?"
        robot.clickOn("Quên mật khẩu?");

        // Đợi một xíu để ứng dụng load giao diện mới (SceneSwitch)
        Thread.sleep(500);

        // KIỂM CHỨNG: Ở màn hình ForgotPassword có 1 dấu hỏi chấm "?" to đùng màu vàng (Theo code FXML của bạn)
        boolean hasQuestionMark = robot.lookup("?").tryQuery().isPresent();
        assertTrue(hasQuestionMark, "Phải chuyển sang màn hình Quên Mật Khẩu (có chứa dấu ?)");
    }

    /**
     * TEST 5: CHUYỂN SANG MÀN HÌNH ĐĂNG KÝ
     * Kỳ vọng: Click vào nút "Đăng ký" thì hệ thống chuyển sang trang register.fxml
     */
    @Test
    public void testNavigateToRegister(FxRobot robot) throws InterruptedException {
        // Tìm và click vào chữ "Đăng ký"
        robot.clickOn("Đăng ký");

        Thread.sleep(500);

        // KIỂM CHỨNG: Màn hình Register có tiêu đề nhập liệu, ta tìm thử ID của nút bấm hoặc chữ
        // Trong đăng ký thường có chữ "Đăng ký" trên nút bấm to (btn-gold)
        boolean hasRegisterBtn = robot.lookup(".btn-gold").tryQuery().isPresent();
        assertTrue(hasRegisterBtn, "Phải chuyển sang giao diện Đăng ký thành công.");
    }

    /**
     * TEST 6: CHUYỂN SANG MÀN HÌNH ADMIN LOGIN
     * Kỳ vọng: Click vào nút "Admin Access" dưới cùng màn hình thì mở giao diện Admin
     */
    @Test
    public void testNavigateToAdminLogin(FxRobot robot) throws InterruptedException {
        // Tìm và click nút Admin
        robot.clickOn("Admin Access");

        Thread.sleep(500);

        // KIỂM CHỨNG: Màn hình AdminLoggin.fxml có chữ "ADMIN" to ở giữa màn hình
        boolean hasAdminLabel = robot.lookup("ADMIN").tryQuery().isPresent();
        assertTrue(hasAdminLabel, "Phải chuyển sang trang đăng nhập của Admin (Có chữ ADMIN).");
    }

}
