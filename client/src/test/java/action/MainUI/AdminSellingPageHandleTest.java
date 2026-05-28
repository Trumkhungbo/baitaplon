package action.MainUI;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import action.SocketClient;
import javafx.scene.control.TableView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class AdminSellingPageHandleTest {

    private AdminSellingPageHandle controller;
    private FakeSocketClient fakeSocketClient;

    @Start
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/AdminSellingPage.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void testTableAndConfirmButton(FxRobot robot) {
        assertTrue(robot.lookup("#ItemsTable").tryQuery().isPresent(), "ItemsTable must exist");
        assertTrue(robot.lookup("Xác nhận duyệt").tryQuery().isPresent(), "Confirm button should be visible");
    }

    @Test
    public void testApprovePendingAuction(FxRobot robot) throws Exception {
        // Inject fake SocketClient instance
        fakeSocketClient = new FakeSocketClient();
        java.lang.reflect.Field instanceField = SocketClient.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        Object prev = instanceField.get(null);
        instanceField.set(null, fakeSocketClient);

        try {
            String data = "AUCTION_LIST|1:Test Item:1000:PENDING:x:x:x:x:2026-06-01:10:30";
            controller.onDataReceived(data);
            Thread.sleep(600);
            // Select the first item's checkbox programmatically via reflection
            Platform.runLater(() -> {
                try {
                    java.lang.reflect.Field itemsField = AdminSellingPageHandle.class.getDeclaredField("ItemsTable");
                    itemsField.setAccessible(true);
                    TableView<?> itemsTable = (TableView<?>) itemsField.get(controller);
                    if (!itemsTable.getItems().isEmpty()) {
                        Object first = itemsTable.getItems().get(0);
                        java.lang.reflect.Method getCheck = first.getClass().getMethod("getCheckBox");
                        javafx.scene.control.CheckBox cb = (javafx.scene.control.CheckBox) getCheck.invoke(first);
                        cb.setSelected(true);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            WaitForAsyncUtils.waitForFxEvents();
            Thread.sleep(1300);

            // Click Confirm button
            robot.clickOn("Xác nhận duyệt");
            Thread.sleep(1600);
            boolean hasApprove = fakeSocketClient.messages.stream().anyMatch(s -> s.contains("APPROVE_AUCTION"));
            assertTrue(hasApprove, "Should have sent APPROVE_AUCTION for selected item");
        } finally {
            instanceField.set(null, prev);
        }
    }

    // Simple fake SocketClient that records requests
    private static class FakeSocketClient extends SocketClient {
        public final java.util.List<String> messages = new java.util.ArrayList<>();
        @Override
        public void requestData(String message) {
            messages.add(message);
        }
    }
}
