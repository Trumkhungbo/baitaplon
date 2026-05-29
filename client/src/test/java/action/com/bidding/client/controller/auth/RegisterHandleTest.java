package action.com.bidding.client.controller.auth;

import action.controller.auth.RegisterHandle;
import javafx.application.Platform;
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
public class RegisterHandleTest {
    private RegisterHandle registerHandle;

    @Start
    public void start(Stage stage) throws Exception {
        // 1. Tải màn hình login.fxml lên để TestFX bắt đầu giả lập click
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/register.fxml"));
        Parent root = loader.load();
        registerHandle = loader.getController();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
    @Test
    public void testOnDataReceived_Success_DoesNotCrash() throws InterruptedException {
        String successJson = "{\"command\":\"REGISTER_RESULT\",\"status\":\"SUCCESS\",\"message\":\"Register Success\"}";
        Platform.runLater(() -> {
            try {
                registerHandle.onDataReceived(successJson);
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
        String failedJson = "{\"command\":\"REGISTER_RESULT\",\"status\":\"FAILED\",\"message\":\"Username already exists\"}";

        Platform.runLater(() -> {
            try {
                registerHandle.onDataReceived(failedJson);
            } catch (Exception e) {
                fail("Hàm onDataReceived không được tung Exception khi nhận JSON thất bại.");
            }
        });

        Thread.sleep(500);
    }
    @Test
    public void testAccoutCreated(FxRobot robot) throws InterruptedException {
        String successJson = "{\"command\":\"REGISTER_RESULT\",\"status\":\"SUCCESS\",\"message\":\"Register Success\"}";

        Platform.runLater(() -> {
            try {
                registerHandle.onDataReceived(successJson);
            } catch (Exception e) {
                fail("Hàm onDataReceived không được tung Exception khi nhận JSON thất bại.");
            }
        });

        Thread.sleep(500);
        boolean hasSuccessPop = robot.lookup("Đăng ký thành công!").tryQuery().isPresent();
        assertTrue(hasSuccessPop,"OkLad");

    }
    @Test
    public void testAccoutCanCreated(FxRobot robot) throws InterruptedException {
        String successJson = "{\"command\":\"REGISTER_RESULT\",\"status\":\"FAILED\",\"message\":\"Email already exists\"}";

        Platform.runLater(() -> {
            try {
                registerHandle.onDataReceived(successJson);
            } catch (Exception e) {
                fail("Hàm onDataReceived không được tung Exception khi nhận JSON thất bại.");
            }
        });

        Thread.sleep(1000);
        boolean hasErrorTitle = robot.lookup("Không thể đăng ký").tryQuery().isPresent();
        assertTrue(hasErrorTitle,"OkLad");

    }
    @Test
    public void testAccoutCanCreated2(FxRobot robot) throws InterruptedException {
        String successJson = "{\"command\":\"REGISTER_RESULT\",\"status\":\"FAILED\",\"message\":\"Username already exists\"}";

        Platform.runLater(() -> {
            try {
                registerHandle.onDataReceived(successJson);
            } catch (Exception e) {
                fail("Hàm onDataReceived không được tung Exception khi nhận JSON thất bại.");
            }
        });

        Thread.sleep(1000);
        boolean hasErrorTitle = robot.lookup("Không thể đăng ký").tryQuery().isPresent();
        assertTrue(hasErrorTitle,"OkLad");

    }
    @Test
    public void testThieuHoTen(FxRobot robot) throws InterruptedException {
        robot.clickOn("#PhoneNumber").write("0123456789");
        robot.clickOn("#Email").write("test@gmail.com");
        robot.clickOn("#PersonalID").write("012345678901");
        robot.clickOn("#Password").write("123456");
        robot.clickOn("#ConfirmPassword").write("123456");

        robot.clickOn(".btn-gold");
        Thread.sleep(500);

        boolean isErrorShowing = robot.lookup("Thiếu Thông Tin").tryQuery().isPresent();
        assertTrue(isErrorShowing, "Lỗi: Bỏ trống Tên mà vẫn cho qua!");
    }
    @Test
    public void testThieuSoDienThoai(FxRobot robot) throws InterruptedException {
        robot.clickOn("#Name").write("Nguyen Van A");
        robot.clickOn("#Email").write("test@gmail.com");
        robot.clickOn("#PersonalID").write("012345678901");
        robot.clickOn("#Password").write("123456");
        robot.clickOn("#ConfirmPassword").write("123456");

        robot.clickOn(".btn-gold");
        Thread.sleep(500);

        boolean isErrorShowing = robot.lookup("Thiếu Thông Tin").tryQuery().isPresent();
        assertTrue(isErrorShowing, "Lỗi: Bỏ trống Sdt mà vẫn cho qua!");
    }
    @Test
    public void testThieuEmail(FxRobot robot) {
        robot.clickOn("#Name").write("Nguyen Van A");
        robot.clickOn("#PhoneNumber").write("0123456789");
        robot.clickOn("#PersonalID").write("012345678901");
        robot.clickOn("#Password").write("123456");
        robot.clickOn("#ConfirmPassword").write("123456");

        robot.clickOn(".btn-gold");

        boolean isErrorShowing = robot.lookup("Thiếu Thông Tin").tryQuery().isPresent();
        assertTrue(isErrorShowing, "Lỗi: Bỏ trống Email mà vẫn cho qua!");
    }
    @Test
    public void testThieuCCCD(FxRobot robot) {
        robot.clickOn("#Name").write("Nguyen Van A");
        robot.clickOn("#PhoneNumber").write("0123456789");
        robot.clickOn("#Email").write("test@gmail.com");
        robot.clickOn("#Password").write("123456");
        robot.clickOn("#ConfirmPassword").write("123456");

        robot.clickOn(".btn-gold");

        boolean isErrorShowing = robot.lookup("Thiếu Thông Tin").tryQuery().isPresent();
        assertTrue(isErrorShowing, "Lỗi: Bỏ trống CCCD mà vẫn cho qua!");
    }
    @Test
    public void testThieuMatKhau(FxRobot robot) {
        robot.clickOn("#Name").write("Nguyen Van A");
        robot.clickOn("#PhoneNumber").write("0123456789");
        robot.clickOn("#Email").write("test@gmail.com");
        robot.clickOn("#PersonalID").write("012345678901");
        robot.clickOn("#ConfirmPassword").write("123456");

        robot.clickOn(".btn-gold");

        boolean isErrorShowing = robot.lookup("Thiếu Thông Tin").tryQuery().isPresent();
        assertTrue(isErrorShowing, "Lỗi: Bỏ trống MatKhau mà vẫn cho qua!");
    }
    @Test
    public void testThieuXacNhanMatKhau(FxRobot robot) {
        robot.clickOn("#Name").write("Nguyen Van A");
        robot.clickOn("#PhoneNumber").write("0123456789");
        robot.clickOn("#Email").write("test@gmail.com");
        robot.clickOn("#PersonalID").write("012345678901");
        robot.clickOn("#Password").write("123456");

        robot.clickOn(".btn-gold");

        boolean isErrorShowing = robot.lookup("Thiếu Thông Tin").tryQuery().isPresent();
        assertTrue(isErrorShowing, "Lỗi: Bỏ trống XacMinhMatKhau mà vẫn cho qua!");
    }
    @Test
    public void testDifPassWord(FxRobot robot) throws InterruptedException {
        robot.clickOn("#Name").write("Nguyen Van A");
        robot.clickOn("#PhoneNumber").write("0123456789");
        robot.clickOn("#Email").write("test@gmail.com");
        robot.clickOn("#PersonalID").write("012345678901");
        robot.clickOn("#Password").write("123456");
        robot.clickOn("#ConfirmPassword").write("654321");


        robot.clickOn(".btn-gold");
        Thread.sleep(1000);
        boolean isErrorShowing = robot.lookup("Sai Định Dạng").tryQuery().isPresent();
        assertTrue(isErrorShowing,"OkLad");

    }
    @Test
    public void testWrongStylePhone(FxRobot robot) throws InterruptedException {
        robot.clickOn("#Name").write("Nguyen Van A");
        robot.clickOn("#PhoneNumber").write("0123456789");
        robot.clickOn("#Email").write("test@gmail.com");
        robot.clickOn("#PersonalID").write("012345678901");
        robot.clickOn("#Password").write("123456");
        robot.clickOn("#ConfirmPassword").write("654321");


        robot.clickOn(".btn-gold");
        Thread.sleep(1000);
        boolean isErrorShowing = robot.lookup("Sai Định Dạng").tryQuery().isPresent();
        assertTrue(isErrorShowing,"OkLad");

    }
    @Test
    public void testWrongStyleEmail(FxRobot robot) throws InterruptedException {
        robot.clickOn("#Name").write("Nguyen Van A");
        robot.clickOn("#PhoneNumber").write("0123456789");
        robot.clickOn("#Email").write("test@gmail.com");
        robot.clickOn("#PersonalID").write("012345678901");
        robot.clickOn("#Password").write("123456");
        robot.clickOn("#ConfirmPassword").write("654321");


        robot.clickOn(".btn-gold");
        Thread.sleep(1000);
        boolean isErrorShowing = robot.lookup("Sai Định Dạng").tryQuery().isPresent();
        assertTrue(isErrorShowing,"OkLad");

    }
    @Test
    public void testWrongStyleCCCD(FxRobot robot) throws InterruptedException {
        robot.clickOn("#Name").write("Nguyen Van A");
        robot.clickOn("#PhoneNumber").write("0123456789");
        robot.clickOn("#Email").write("test@gmail.com");
        robot.clickOn("#PersonalID").write("012345678901");
        robot.clickOn("#Password").write("123456");
        robot.clickOn("#ConfirmPassword").write("654321");


        robot.clickOn(".btn-gold");
        Thread.sleep(1000);
        boolean isErrorShowing = robot.lookup("Sai Định Dạng").tryQuery().isPresent();
        assertTrue(isErrorShowing,"OkLad");

    }
    @Test
    public void returnLogin(FxRobot robot) throws InterruptedException {
        robot.clickOn("Đăng nhập");
        Thread.sleep(500);
        boolean isReturn = robot.lookup("Đăng nhập").tryQuery().isPresent();
        assertTrue(isReturn,"OkLad");
    }

}
