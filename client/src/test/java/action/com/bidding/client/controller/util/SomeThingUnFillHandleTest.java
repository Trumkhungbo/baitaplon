package action.com.bidding.client.controller.util;

import action.controller.util.SomeThingUnFillHandle;
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

import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(ApplicationExtension.class)
public class SomeThingUnFillHandleTest {

    private Button mockButton;
    private Stage testStage;

    @Start
    private void start(Stage stage) {
        this.testStage = stage;
        mockButton = new Button("Close Me");
        Scene scene = new Scene(new StackPane(mockButton), 200, 200);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void testReturnPreviousPage_ClosesStage() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        SomeThingUnFillHandle controller = new SomeThingUnFillHandle();

        Platform.runLater(() -> {
            try {
                ActionEvent event = new ActionEvent(mockButton, null);
                controller.ReturnPreviousPage(event);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        CountDownLatch checkLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            assertFalse(testStage.isShowing(), "Stage (Popup) phải bị đóng sau khi gọi ReturnPreviousPage");
            checkLatch.countDown();
        });
        checkLatch.await();
    }
}
