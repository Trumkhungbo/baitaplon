package action.com.bidding.client.controller.auth;

import action.controller.auth.RegisterPopUpHandle;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(ApplicationExtension.class)
public class RegisterPopUpHandleTest {

    private Stage stage;

    @Start
    public void start(Stage stage) {
        this.stage = stage;
        // Tạo một button giả để làm source cho ActionEvent
        Button btn = new Button("Close");
        stage.setScene(new Scene(new StackPane(btn), 100, 100));
        stage.show();
    }

    @Test
    public void testReturnToLogin_ClosesStage() throws InterruptedException {
        RegisterPopUpHandle controller = new RegisterPopUpHandle();
        CountDownLatch latch = new CountDownLatch(1);

        // Gọi action trên JavaFX Thread
        Platform.runLater(() -> {
            try {
                // Tạo ActionEvent giả từ button trong scene
                javafx.event.ActionEvent event = new javafx.event.ActionEvent(
                        stage.getScene().getRoot().getChildrenUnmodifiable().get(0), null
                );
                controller.ReturnToLogin(event);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        // Chờ xử lý xong
        latch.await(2, TimeUnit.SECONDS);

        // Kiểm tra kết quả
        assertFalse(stage.isShowing(), "Cửa sổ phải được đóng sau khi gọi hàm");
    }
}