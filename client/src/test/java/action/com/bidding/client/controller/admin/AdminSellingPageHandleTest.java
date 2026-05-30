package action.com.bidding.client.controller.admin;

import action.controller.admin.AdminSellingPageHandle;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import action.network.SocketClient;
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
            // 1. Nhận dữ liệu và đợi JavaFX cập nhật UI hoàn toàn
            Platform.runLater(() -> {
                String data = "AUCTION_LIST|1:Test Item:1000:PENDING:x:x:x:x:2026-06-01:10:30";
                controller.onDataReceived(data);
            });
            WaitForAsyncUtils.waitForFxEvents();

            // 2. Tích chọn CheckBox thông qua Reflection trên luồng FX
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

            // Ép đợi cho đến khi trạng thái CheckBox thực sự được thay đổi và cập nhật vào Model
            WaitForAsyncUtils.waitForFxEvents();

            // 3. Thực hiện Click nút Xác nhận bằng FxRobot
            robot.clickOn("Xác nhận duyệt");

            // Đợi các event phát sinh sau khi click (như việc gọi hàm gửi dữ liệu socket) thực thi xong
            WaitForAsyncUtils.waitForFxEvents();

            // 4. In log debug nếu cần (Nếu vẫn lỗi, hãy bỏ comment dòng dưới để check xem client gửi gì)
            // System.out.println("Thực tế gửi đi: " + fakeSocketClient.messages);

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