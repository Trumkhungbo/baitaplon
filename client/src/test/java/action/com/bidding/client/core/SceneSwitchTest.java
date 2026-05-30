package action.com.bidding.client.core;

import action.Core.SceneSwitch;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(ApplicationExtension.class)
public class SceneSwitchTest {

    private Button mockButton;

    @Start
    private void start(Stage stage) {
        mockButton = new Button("Test Button");
        Scene scene = new Scene(new StackPane(mockButton), 100, 100);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void testSwitchToLogin_WithValidEvent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ActionEvent mockEvent = new ActionEvent(mockButton, null);
                SceneSwitch sceneSwitch = new SceneSwitch();

                assertDoesNotThrow(() -> {
                    sceneSwitch.SwitchToLogin(mockEvent);
                }, "Chuyển sang trang Login bị lỗi, có thể do file /views/login.fxml không tồn tại");

            } finally {
                latch.countDown();
            }
        });

        latch.await();
        Thread.sleep(1000);
    }

    @Test
    void testSwitchToAnyWhere_WithValidFXML() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                ActionEvent mockEvent = new ActionEvent(mockButton, null);
                SceneSwitch sceneSwitch = new SceneSwitch();

                assertDoesNotThrow(() -> {
                    sceneSwitch.SwitchToAnyWhere(mockEvent, "/views/register.fxml");
                });

            } finally {
                latch.countDown();
            }
        });

        latch.await();
        Thread.sleep(1000);
    }
}