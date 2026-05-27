package action.SellingJobs;

import action.Authentication.StoreItemDataInit;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class ItemShowingHandleTest {

    private ItemShowingHandle handle;

    @Start
    public void start(Stage stage) throws Exception {
        // Giả lập dữ liệu món hàng người dùng vừa click vào (từ Lobby truyền sang)
        StoreItemDataInit.description = "12345"; // id
        StoreItemDataInit.name = "Test Item";

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ItemShowing.fxml"));
        Parent root = loader.load();
        handle = loader.getController();

        // TẠO MÔI TRƯỜNG GIẢ LẬP NHƯ LOBBY
        StackPane frame = new StackPane();
        frame.setStyle("-fx-background-color: #0F0F0F;");
        frame.getChildren().add(root);

        Scene scene = new Scene(frame, 1280, 768);

        // NHÚNG CSS
        String globalCss = getClass().getResource("/views/global.css").toExternalForm();
        String appCss = getClass().getResource("/views/app.css") != null ?
                getClass().getResource("/views/app.css").toExternalForm() : "";

        scene.getStylesheets().add(globalCss);
        if(!appCss.isEmpty()) scene.getStylesheets().add(appCss);

        stage.setScene(scene);
        stage.show();
    }

    @BeforeEach
    public void setUp() {
        // Đảm bảo Data ID luôn tồn tại trước mỗi Test
        StoreItemDataInit.description = "12345";
    }

    @Test
    public void testInitialization(FxRobot robot) {
        // Kiểm tra một số Label cơ bản đã được nhúng chưa
        Label nameLabel = robot.lookup("#name").queryAs(Label.class);
        Label priceLabel = robot.lookup("#price").queryAs(Label.class);
        TableView<?> bidTable = robot.lookup("#bidHistoryTable").queryAs(TableView.class);

        assertNotNull(nameLabel, "Label tên sản phẩm không được null");
        assertNotNull(priceLabel, "Label giá sản phẩm không được null");
        assertNotNull(bidTable, "Bảng lịch sử đặt giá không được null");
        assertTrue(bidTable.getItems().isEmpty(), "Lúc mới khởi tạo bảng lịch sử phải trống");
    }

    @Test
    public void testRenderAuctionDetail_Running(FxRobot robot) throws InterruptedException {
        // Giả lập Server gửi thông tin chi tiết của phiên đang RUNNING
        String data = "AUCTION_DETAIL|itemName=Đồng Hồ Rolex|currentPrice=15000000|status=RUNNING|startDate=15/05/2026|itemType=VEHICLE|duration=60";

        Platform.runLater(() -> {
            try {
                handle.onDataReceived(data);
            } catch (Exception e) {
                fail("Không được crash khi nhận data AUCTION_DETAIL: " + e.getMessage());
            }
        });

        Thread.sleep(1500); // Đợi JavaFX update UI

        // Kiểm chứng UI cập nhật đúng text
        Label nameLabel = robot.lookup("#name").queryAs(Label.class);
        Label priceLabel = robot.lookup("#price").queryAs(Label.class);
        Label statusLabel = robot.lookup("#status").queryAs(Label.class);
        Button raiseBidBtn = robot.lookup("#raiseBidButton").queryAs(Button.class);

        assertEquals("Đồng Hồ Rolex", nameLabel.getText());
        assertEquals("15,000,000 VNĐ", priceLabel.getText());
        assertEquals("RUNNING", statusLabel.getText());

        // Trạng thái RUNNING thì nút đặt giá phải enable
        assertFalse(raiseBidBtn.isDisabled(), "Nút đặt giá phải được Enable khi trạng thái là RUNNING");
        // Pause briefly so a human tester can visually inspect UI when running locally
        Thread.sleep(600);
    }

    @Test
    public void testRenderAuctionDetail_Finished(FxRobot robot) throws InterruptedException {
        // Giả lập Server gửi thông tin chi tiết của phiên đã FINISHED
        String data = "AUCTION_DETAIL|itemName=Vỏ Chai Nước|currentPrice=5000|status=FINISHED|startDate=15/05/2026|duration=0";

        Platform.runLater(() -> handle.onDataReceived(data));
        Thread.sleep(10500);

        // Kiểm chứng giao diện thay đổi theo trạng thái
        Label statusLabel = robot.lookup("#status").queryAs(Label.class);
        VBox activeBidCard = robot.lookup("#activeBidCard").queryAs(VBox.class);
        VBox finishedResultCard = robot.lookup("#finishedResultCard").queryAs(VBox.class);

        assertEquals("FINISHED", statusLabel.getText());

        // Khi kết thúc: Sidebar đặt giá (activeBidCard) bị ẩn, Sidebar kết quả (finishedResultCard) hiện ra
        assertFalse(activeBidCard.isVisible(), "Khung đặt giá phải bị ẩn khi kết thúc");
        assertTrue(finishedResultCard.isVisible(), "Khung kết quả phải hiện ra khi kết thúc");
        Thread.sleep(600);
    }

    @Test
    public void testRenderBidHistory(FxRobot robot) throws InterruptedException {
        // Dữ liệu mô phỏng Lịch sử đặt giá trả về từ server
        // Format: BID_HISTORY|entries=username,amount,timestamp;username,amount,timestamp...
        // 1704067200000 là timestamp giả
        String data = "BID_HISTORY|entries=UserA,1000000,1704067200000;UserB,2000000,1704067250000;UserC,3000000,1704067300000";

        Platform.runLater(() -> handle.onDataReceived(data));
        Thread.sleep(10500);

        TableView<?> bidTable = robot.lookup("#bidHistoryTable").queryAs(TableView.class);
        Label bidCountLabel = robot.lookup("#bidCountLabel").queryAs(Label.class);

        // Bảng phải có 3 người
        assertEquals(3, bidTable.getItems().size(), "TableView phải nạp thành công 3 dòng dữ liệu");

        // Text thống kê ở bên kia cũng phải đếm ra 3
        assertEquals("3 lượt", bidCountLabel.getText());
    }

    @Test
    public void testBidUpdateReRendersPrice(FxRobot robot) throws InterruptedException {
        // Giả lập có người vừa đặt giá thành công
        String data = "BID_SUCCESS|status=SUCCESS|highestBid=5500000|highestBidder=ProSniper";

        Platform.runLater(() -> handle.onDataReceived(data));
        Thread.sleep(10500);

        Label priceLabel = robot.lookup("#price").queryAs(Label.class);
        Label leaderLabel = robot.lookup("#leaderValueLabel").queryAs(Label.class);

        assertEquals("5,500,000 VNĐ", priceLabel.getText(), "Giá sản phẩm phải nhảy lên mức cao nhất mới");
        assertEquals("ProSniper", leaderLabel.getText(), "Tên người dẫn đầu phải cập nhật");
    }
    @Test
    public void testRaiseBidButton_InvalidInput(FxRobot robot) throws InterruptedException {
        // 1. Phải mồi data để mở khóa UI (chuyển sang RUNNING)
        String data = "AUCTION_DETAIL|status=RUNNING";
        Platform.runLater(() -> handle.onDataReceived(data));
        Thread.sleep(1500);

        // Lúc này nút Đặt giá và ô tiền đã Enable.
        // 2. Cố tình nhập chữ (thay vì số tiền) vào ô đặt giá
        robot.clickOn("#money").write("muoi_lam_trieu");

        // 3. Click nút Đặt giá ngay
        robot.clickOn("#raiseBidButton");
        Thread.sleep(1200);

        // 4. Kiểm tra xem hệ thống có bắt lỗi và in ra Label không
        Label statusSubLabel = robot.lookup("#statusSubLabel").queryAs(Label.class);
        assertEquals("Số tiền đặt giá không hợp lệ", statusSubLabel.getText(),
                "Hệ thống phải báo lỗi khi người dùng nhập chữ vào ô tiền");
    }

    @Test
    public void testAutoBidToggle_NotRunning(FxRobot robot) throws InterruptedException {
        // Mặc định lúc mới load trạng thái là UNKNOWN
        // hmm do đây là test sẽ hiện ra là không tương tá được ở autoBid khi hết hạn

        ToggleButton autoBidToggle = robot.lookup("#autoBidToggle").queryAs(ToggleButton.class);

        // Kiểm chứng: Nút phải bị khóa (Disable) ngay từ đầu
        assertTrue(autoBidToggle.isDisabled(), "Công tắc Auto-bid phải bị vô hiệu hóa khi phiên chưa RUNNING");

        // Cố tình đẩy trạng thái về FINISHED
        Platform.runLater(() -> handle.onDataReceived("AUCTION_DETAIL|status=FINISHED"));
        Thread.sleep(1300);
        assertTrue(autoBidToggle.isDisabled(), "Công tắc Auto-bid phải bị vô hiệu hóa khi phiên đã FINISHED");
    }

    @Test
    public void testAutoBidToggle_InvalidInput(FxRobot robot) throws InterruptedException {
        // 1. Phải mồi data để chuyển trạng thái phiên thành RUNNING trước
        String data = "AUCTION_DETAIL|status=RUNNING";
        Platform.runLater(() -> handle.onDataReceived(data));
        Thread.sleep(1500);

        // 2. Nhập linh tinh vào ô Giá tối đa và Bước giá
        robot.clickOn("#autoBidMax").write("khong_phai_la_so");
        robot.clickOn("#autoBidIncrement").write("10000");

        // 3. Bấm bật công tắc
        robot.clickOn("#autoBidToggle");
        Thread.sleep(11200);

        // 4. Kiểm chứng hệ thống phải chặn lại vì ô MaxBid nhập chữ
        Label autoBidStatus = robot.lookup("#autoBidStatus").queryAs(Label.class);
        assertEquals("Thông tin auto-bid không hợp lệ.", autoBidStatus.getText(),
                "Hệ thống phải kiểm tra validation các ô input Auto-bid");
    }
}
