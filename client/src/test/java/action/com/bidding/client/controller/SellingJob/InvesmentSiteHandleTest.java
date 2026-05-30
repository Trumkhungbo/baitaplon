package action.com.bidding.client.controller.SellingJob;

import action.controller.SellingJobs.InvesmentSiteHandle;
import action.model.StoreItemDataInit;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class InvesmentSiteHandleTest {

    private InvesmentSiteHandle handle;
    private FlowPane flowPane;

    @Start
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/InvesmentSite.fxml"));
        Parent siteRoot = loader.load();
        handle = loader.getController();

        // TẠO MÔI TRƯỜNG GIẢ LẬP NHƯ LOBBY
        StackPane root = new StackPane();
        // Set màu nền chuẩn theo mã -fx-dark-bg của app bạn
        root.setStyle("-fx-background-color: #0F0F0F;");
        root.getChildren().add(siteRoot);

        Scene scene = new Scene(root, 1200, 800);

        // NHÚNG GÓI CSS CỦA DỰ ÁN VÀO TEST ĐỂ CÓ MÀU VÀNG GOLD
        String cssPath = getClass().getResource("/views/global.css").toExternalForm();
        scene.getStylesheets().add(cssPath);

        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void testInitialization(FxRobot robot) {
        Button btnAll = robot.lookup("#filterAllButton").queryAs(Button.class);
        Button btnElec = robot.lookup("#filterElectronicsButton").queryAs(Button.class);
        Button btnArt = robot.lookup("#filterArtButton").queryAs(Button.class);
        Button btnVehicles = robot.lookup("#filterVehiclesButton").queryAs(Button.class);

        assertNotNull(btnAll, "Nút Filter All phải tồn tại");
        assertNotNull(btnElec, "Nút Filter Electronics phải tồn tại");
        assertNotNull(btnArt, "Nút Filter Art phải tồn tại");
        assertNotNull(btnVehicles, "Nút Filter Vehicles phải tồn tại");

        flowPane = robot.lookup("#flowPane").queryAs(FlowPane.class);
        assertNotNull(flowPane, "FlowPane phải tồn tại");

        assertTrue(flowPane.getChildren().isEmpty(), "FlowPane phải trống ở trạng thái khởi tạo");
    }

    @Test
    public void testOnDataReceived_JsonFormat(FxRobot robot) throws InterruptedException {
        String jsonData = "{\"command\":\"AUCTION_LIST_RESULT\",\"items\":[" +
                "{\"id\":\"1\",\"itemName\":\"Art Item 1\",\"currentPrice\":100.0,\"status\":\"RUNNING\",\"imageUrl\":\"img1\",\"itemType\":\"ART\",\"startTime\":\"10:00\",\"endTime\":1234567890,\"serverTime\":1234567000}," +
                "{\"id\":\"2\",\"itemName\":\"Elec Item 1\",\"currentPrice\":200.0,\"status\":\"OPEN\",\"imageUrl\":\"img2\",\"itemType\":\"ELECTRONICS\",\"startTime\":\"11:00\",\"endTime\":1234567890,\"serverTime\":1234567000}," +
                "{\"id\":\"3\",\"itemName\":\"Vehicle Item 1\",\"currentPrice\":300.0,\"status\":\"FINISHED\",\"imageUrl\":\"img3\",\"itemType\":\"VEHICLE\",\"startTime\":\"12:00\",\"endTime\":1234567890,\"serverTime\":1234567000}" +
                "]}";

        Platform.runLater(() -> {
            try {
                handle.onDataReceived(jsonData);
            } catch (Exception e) {
                fail("Hàm onDataReceived bị crash khi nhận JSON hợp lệ: " + e.getMessage());
            }
        });

        WaitForAsyncUtils.waitForFxEvents();
        flowPane = robot.lookup("#flowPane").queryAs(FlowPane.class);
        assertEquals(6, flowPane.getChildren().size(), "FlowPane phải có 6 con (3 thẻ item + 3 text title nhóm trạng thái)");
    }

    @Test
    public void testFilterFunctionality(FxRobot robot) throws InterruptedException {
        String jsonData = "{\"command\":\"AUCTION_LIST_RESULT\",\"items\":[" +
                "{\"id\":\"1\",\"itemName\":\"Art Item 1\",\"currentPrice\":100.0,\"status\":\"RUNNING\",\"imageUrl\":\"img1\",\"itemType\":\"ART\",\"startTime\":\"10:00\",\"endTime\":1234567890,\"serverTime\":1234567000}," +
                "{\"id\":\"2\",\"itemName\":\"Elec Item 1\",\"currentPrice\":200.0,\"status\":\"OPEN\",\"imageUrl\":\"img2\",\"itemType\":\"ELECTRONICS\",\"startTime\":\"11:00\",\"endTime\":1234567890,\"serverTime\":1234567000}," +
                "{\"id\":\"3\",\"itemName\":\"Vehicle Item 1\",\"currentPrice\":300.0,\"status\":\"FINISHED\",\"imageUrl\":\"img3\",\"itemType\":\"VEHICLE\",\"startTime\":\"12:00\",\"endTime\":1234567890,\"serverTime\":1234567000}" +
                "]}";

        Platform.runLater(() -> {
            handle.onDataReceived(jsonData);
        });

        WaitForAsyncUtils.waitForFxEvents();
        flowPane = robot.lookup("#flowPane").queryAs(FlowPane.class);
        assertEquals(6, flowPane.getChildren().size(), "Lọc ALL (mặc định) phải hiển thị đủ 6 phần tử");

        robot.clickOn("#filterArtButton");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(2, flowPane.getChildren().size(), "Lọc Art phải hiển thị 2 phần tử (Title + 1 Card)");

        robot.clickOn("#filterElectronicsButton");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(2, flowPane.getChildren().size(), "Lọc Electronics phải hiển thị 2 phần tử (Title + 1 Card)");

        robot.clickOn("#filterVehiclesButton");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(2, flowPane.getChildren().size(), "Lọc Vehicles phải hiển thị 2 phần tử (Title + 1 Card)");

        robot.clickOn("#filterAllButton");
        WaitForAsyncUtils.waitForFxEvents();
        assertEquals(6, flowPane.getChildren().size(), "Lọc All phải trả lại đủ 6 phần tử");
    }

    @Test
    public void testOnDataReceived_StringFormat(FxRobot robot) throws InterruptedException {
        String stringData = "AUCTION_LIST|1:Art Item 1:100.0:RUNNING:img1:x:x:x:x:10-00:x:ART:12345:1234;2:Elec Item 1:200.0:OPEN:img2:x:x:x:x:11-00:x:ELECTRONICS:12345:1234";

        Platform.runLater(() -> {
            try {
                handle.onDataReceived(stringData);
            } catch (Exception e) {
                fail("Hàm onDataReceived bị crash khi nhận dạng String: " + e.getMessage());
            }
        });

        WaitForAsyncUtils.waitForFxEvents();

        flowPane = robot.lookup("#flowPane").queryAs(FlowPane.class);
        assertEquals(4, flowPane.getChildren().size(), "Phải khởi tạo thành công 4 phần tử (2 title + 2 thẻ) đối với string thuần");
    }

    @Test
    public void testOnDataReceived_InvalidData(FxRobot robot) throws InterruptedException {
        String invalidData = "INVALID_COMMAND|SOMETHING_WEIRD";

        Platform.runLater(() -> {
            handle.onDataReceived(invalidData);
        });

        WaitForAsyncUtils.waitForFxEvents();

        flowPane = robot.lookup("#flowPane").queryAs(FlowPane.class);
        assertTrue(flowPane.getChildren().isEmpty(), "Khi nhận lệnh sai, FlowPane phải giữ nguyên trạng thái trống");
    }

    @Test
    public void testClickOnAuctionCardSetsStaticData(FxRobot robot) throws InterruptedException {
        // 1. Bơm Data vào để nó vẽ ra 1 cái thẻ sản phẩm
        String jsonData = "{\"command\":\"AUCTION_LIST_RESULT\",\"items\":[" +
                "{\"id\":\"999\",\"itemName\":\"Siêu Xe Ferrari\",\"currentPrice\":5000000.0,\"status\":\"RUNNING\",\"imageUrl\":\"ferrari.png\",\"itemType\":\"VEHICLE\",\"startTime\":\"10:00\",\"endTime\":1234567890,\"serverTime\":1234567000}" +
                "]}";

        Platform.runLater(() -> {
            handle.onDataReceived(jsonData);
        });
        WaitForAsyncUtils.waitForFxEvents();

        // 2. Click vào nút "Tham gia đấu giá" bên trong thẻ đó
        robot.clickOn("Tham gia đấu giá");
        WaitForAsyncUtils.waitForFxEvents();

        // 3. Kiểm chứng (Verify)
        assertEquals("Siêu Xe Ferrari", StoreItemDataInit.name,
                "Tên sản phẩm tĩnh phải được gán bằng 'Siêu Xe Ferrari' sau khi click");
        assertEquals("999", StoreItemDataInit.description,
                "ID sản phẩm tĩnh (dùng chung biến description) phải được gán bằng '999'");

        // --- SỬA TẠI ĐÂY ---
        // Thay đổi định dạng kỳ vọng từ "5,000,000" thành "5.000.000" để khớp hoàn toàn với môi trường Locale máy hiện tại.
        assertEquals("5.000.000", StoreItemDataInit.price,
                "Giá sản phẩm phải được format chuẩn sau khi click");
    }
}