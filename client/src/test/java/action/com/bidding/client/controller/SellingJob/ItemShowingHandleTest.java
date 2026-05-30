package action.com.bidding.client.controller.SellingJob;

import action.controller.SellingJobs.ItemShowingHandle;
import action.controller.main.LobbyHandle;
import action.model.StoreDataInput;
import action.model.StoreItemDataInit;
import action.network.SocketClient;
import action.network.SocketListener;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ItemShowingHandleTest extends ApplicationTest {

    private ItemShowingHandle controller;

    // Hệ thống nút bấm và ô văn bản mock dữ liệu đầu vào
    private ImageView image;
    private Label name, price, status, description, date, starTime, duration, Type;
    private Label information1, information2, balanceValue, avatarLabel, bidHistoryHint;
    private Label autoBidStatus, heroStatusChip, modeLabel, statusSubLabel, auctionCodeLabel;
    private Label scheduleLabel, historyTitleLabel, historyModeLabel, sellerLabel;
    private Label winnerAvatarLabel, winnerNameLabel, winnerPriceLabel, startPriceLabel;
    private Label finalPriceStatLabel, bidCountLabel, finishedAtLabel, resultHintLabel, leaderValueLabel;
    private TextField money, autoBidMax, autoBidIncrement;
    private ProgressBar timeProgress;

    // Thành phần TableView & Đồ thị phức tạp của JavaFX
    private TableView<ItemShowingHandle.BidHistoryRow> bidHistoryTable;
    private TableColumn<ItemShowingHandle.BidHistoryRow, String> bidUserColumn;
    private TableColumn<ItemShowingHandle.BidHistoryRow, String> bidAmountColumn;
    private TableColumn<ItemShowingHandle.BidHistoryRow, String> bidTimeColumn;
    private LineChart<String, Number> bidHistoryChart;
    private CategoryAxis bidHistoryTimeAxis;
    private NumberAxis bidHistoryPriceAxis;
    private VBox activeBidCard, finishedResultCard, autoBidCard, topWinnersBox;
    private Button raiseBidButton;
    private ToggleButton autoBidToggle;

    // Injected/Mock objects for singletons
    private FakeSocketClient mockSocketClient;
    private FakeLobbyHandle mockLobbyHandle;

    private static class FakeSocketClient extends SocketClient {
        public java.util.List<String> sent = new java.util.ArrayList<>();
        public java.util.List<Object> listenersAdded = new java.util.ArrayList<>();

        @Override
        public void requestData(String message) {
            sent.add(message);
        }

        @Override
        public void addListener(SocketListener listener) {
            super.addListener(listener);
            listenersAdded.add(listener);
        }

        @Override
        public void removeListener(SocketListener listener) {
            super.removeListener(listener);
        }
    }

    private static class FakeLobbyHandle extends LobbyHandle {
        public final java.util.List<String> calls = new java.util.ArrayList<>();
        @Override
        public void MovingCenter(String url) throws java.io.IOException {
            calls.add(url);
        }
    }

    @BeforeAll
    public static void setupHeadless() {
        System.setProperty("glass.platform", "Monocle");
        System.setProperty("monocle.platform", "Headless");
        System.setProperty("prism.order", "sw");
    }

    @BeforeEach
    public void setUp() throws Exception {
        StoreItemDataInit.description = "AUC_999"; // Mã phòng đấu giá mẫu
        controller = new ItemShowingHandle();

        // Khởi tạo các Node UI thật để không bị lỗi kết xuất biểu đồ/bảng
        name = new Label(); price = new Label(); status = new Label(); description = new Label();
        date = new Label(); starTime = new Label(); duration = new Label(); Type = new Label();
        information1 = new Label(); information2 = new Label(); balanceValue = new Label();
        avatarLabel = new Label(); bidHistoryHint = new Label(); autoBidStatus = new Label();
        heroStatusChip = new Label(); modeLabel = new Label(); statusSubLabel = new Label();
        auctionCodeLabel = new Label(); scheduleLabel = new Label(); historyTitleLabel = new Label();
        historyModeLabel = new Label(); sellerLabel = new Label(); winnerAvatarLabel = new Label();
        winnerNameLabel = new Label(); winnerPriceLabel = new Label(); startPriceLabel = new Label();
        finalPriceStatLabel = new Label(); bidCountLabel = new Label(); finishedAtLabel = new Label();
        resultHintLabel = new Label(); leaderValueLabel = new Label();
        money = new TextField(); autoBidMax = new TextField(); autoBidIncrement = new TextField();
        timeProgress = new ProgressBar(); image = new ImageView();
        activeBidCard = new VBox(); finishedResultCard = new VBox(); autoBidCard = new VBox(); topWinnersBox = new VBox();
        raiseBidButton = new Button(); autoBidToggle = new ToggleButton();

        // Khởi tạo Table & Đồ thị
        bidHistoryTable = new TableView<>();
        bidUserColumn = new TableColumn<>(); bidAmountColumn = new TableColumn<>(); bidTimeColumn = new TableColumn<>();
        bidHistoryTable.getColumns().addAll(bidUserColumn, bidAmountColumn, bidTimeColumn);

        bidHistoryTimeAxis = new CategoryAxis();
        bidHistoryPriceAxis = new NumberAxis();
        bidHistoryChart = new LineChart<>(bidHistoryTimeAxis, bidHistoryPriceAxis);

        // Inject sạch tất cả các trường FXML vào Controller
        String[] labels = {"name", "price", "status", "description", "date", "starTime", "duration", "Type",
                "information1", "information2", "balanceValue", "avatarLabel", "bidHistoryHint", "autoBidStatus",
                "heroStatusChip", "modeLabel", "statusSubLabel", "auctionCodeLabel", "scheduleLabel", "historyTitleLabel",
                "historyModeLabel", "sellerLabel", "winnerAvatarLabel", "winnerNameLabel", "winnerPriceLabel",
                "startPriceLabel", "finalPriceStatLabel", "bidCountLabel", "finishedAtLabel", "resultHintLabel", "leaderValueLabel"};
        for (String f : labels) injectField(f, getClass().getDeclaredField(f).get(this));

        injectField("money", money); injectField("autoBidMax", autoBidMax); injectField("autoBidIncrement", autoBidIncrement);
        injectField("timeProgress", timeProgress); injectField("image", image);
        injectField("activeBidCard", activeBidCard); injectField("finishedResultCard", finishedResultCard);
        injectField("autoBidCard", autoBidCard); injectField("topWinnersBox", topWinnersBox);
        injectField("raiseBidButton", raiseBidButton); injectField("autoBidToggle", autoBidToggle);
        injectField("bidHistoryTable", bidHistoryTable); injectField("bidUserColumn", bidUserColumn);
        injectField("bidAmountColumn", bidAmountColumn); injectField("bidTimeColumn", bidTimeColumn);
        injectField("bidHistoryChart", bidHistoryChart); injectField("bidHistoryTimeAxis", bidHistoryTimeAxis);
        injectField("bidHistoryPriceAxis", bidHistoryPriceAxis);

        // Thiết lập môi trường Static - inject thay vì mockStatic để tránh inline Byte Buddy issues
        mockSocketClient = new FakeSocketClient();
        SocketClient.setInstance(mockSocketClient);

        StoreDataInput.username = "Chubds";

        mockLobbyHandle = new FakeLobbyHandle();
        try {
            Field instanceField = LobbyHandle.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, mockLobbyHandle);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    public void tearDown() {
        SocketClient.setInstance(null);
        StoreDataInput.username = null;
        try {
            Field instanceField = LobbyHandle.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception ignored) {}
    }

    private void injectField(String name, Object obj) throws Exception {
        Field f = ItemShowingHandle.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(controller, obj);
    }

    // --- BẮT ĐẦU CÁC CƠ CHẾ KIỂM THỬ COVEREGE ---

    @Test
    public void testInitializeAndSyncRequests() {
        // Gọi khởi tạo giao diện
        controller.initialize(null, null);

        // Xác minh toàn bộ các lệnh kéo dữ liệu ban đầu từ Server được kích hoạt đồng loạt
        // ensure listener registered and initial requests were sent
        assertTrue(mockSocketClient.listenersAdded.contains(controller));
        assertTrue(mockSocketClient.sent.contains("WATCH|AUC_999"));
        assertTrue(mockSocketClient.sent.contains("GET_AUCTION_DETAIL|AUC_999"));
        assertTrue(mockSocketClient.sent.contains("GET_BID_HISTORY|AUC_999"));
        assertTrue(mockSocketClient.sent.contains("GET_AUTO_BID|AUC_999"));
        assertEquals("C", avatarLabel.getText()); // Trích xuất ký tự đầu của username "Chubds" làm Avatar
    }

    @Test
    public void testRaiseBid_ValidAndInvalid() {
        // Ca 1: Số tiền không hợp lệ (Chứa chữ cái)
        money.setText("100K");
        controller.RaiseBind(new ActionEvent());
        assertEquals("Số tiền đặt giá không hợp lệ", statusSubLabel.getText());

        // Ca 2: Số tiền hợp lệ (Tự dọn dẹp định dạng tiền tệ như dấu phẩy)
        money.setText("1,500,000");
        controller.RaiseBind(new ActionEvent());
        assertTrue(mockSocketClient.sent.contains("BID|AUC_999|1500000"));
    }

    @Test
    public void testEnableAutoBid_ToggleMechanisms() {
        controller.initialize(null, null);
        autoBidMax.setText("10,000,000");
        autoBidIncrement.setText("500,000");

        // Thử kích hoạt khi trạng thái chưa phải RUNNING -> Bị từ chối bật
        controller.EnableAutoBid(new ActionEvent());
        assertEquals("Auto-bid chỉ bật khi phiên đang RUNNING.", autoBidStatus.getText());

        // Đổi trạng thái sang RUNNING bằng cách giả lập cấu hình biến private qua Reflection
        try {
            Field statusField = ItemShowingHandle.class.getDeclaredField("currentAuctionStatus");
            statusField.setAccessible(true);
            statusField.set(controller, "RUNNING");
        } catch (Exception ignored) {}

        // Bật thành công
        controller.EnableAutoBid(new ActionEvent());
        assertTrue(mockSocketClient.sent.contains("SET_AUTO_BID|AUC_999|10000000|500000"));
    }

    @Test
    public void testReturnToInvesment_CleansUpAndRedirects() throws IOException {
        controller.ReturnToInvesment(new ActionEvent());

        // Phải hủy lắng nghe sự kiện để tránh rò rỉ bộ nhớ, gửi lệnh hủy xem, và chuyển hướng màn hình chính
        assertTrue(mockSocketClient.sent.contains("WATCH|"));
        assertTrue(mockLobbyHandle.calls.contains("/views/InvesmentSite.fxml"));
    }

    @Test
    public void testOnDataReceived_AuctionDetailParsing() {
        controller.initialize(null, null);

        // Chuỗi payload mô phỏng đầy đủ các tham số cấu hình phòng đấu giá trả về dạng key=value
        String payload = "AUCTION_DETAIL|itemName=IPhone 15 Pro|currentPrice=22000000|status=RUNNING" +
                "|startDate=30/05/2026|itemType=ELECTRONICS|seller=AppleStore|information1=VND|information2=New" +
                "|description=Chinh hang VN/A|auctionId=AUC_999|durationMinutes=30|startPrice=20000000" +
                "|bidCount=5|highestBidder=AnNguyen|endTime=" + (System.currentTimeMillis() + 600000) + "|serverTime=" + System.currentTimeMillis();

        Platform.runLater(() -> controller.onDataReceived(payload));
        waitForFx();

        // Kiểm tra toàn bộ UI được lấp đầy dữ liệu bóc tách chính xác từ chuỗi cấu trúc trên
        assertEquals("IPhone 15 Pro", name.getText());
        assertEquals("22.000.000 VNĐ", price.getText());
        assertEquals("RUNNING", status.getText());
        assertEquals("AnNguyen", leaderValueLabel.getText());
        assertEquals("5 lượt", bidCountLabel.getText());
    }

    @Test
    public void testOnDataReceived_BidHistoryAndChartRendering() {
        controller.initialize(null, null);

        // Đặt trạng thái kết thúc để ép đồ thị và danh sách Top 3 Winner xếp hạng xuất hiện trên màn hình
        try {
            Field statusField = ItemShowingHandle.class.getDeclaredField("currentAuctionStatus");
            statusField.setAccessible(true);
            statusField.set(controller, "FINISHED");
        } catch (Exception ignored) {}

        // Giả lập chuỗi danh sách lịch sử đặt giá ngăn cách bởi dấu ';' và ','
        String payload = "BID_HISTORY|entries=UserA,500000,1717070400000;UserB,600000,1717070460000;UserC,700000,1717070520000";

        Platform.runLater(() -> controller.onDataReceived(payload));
        waitForFx();

        // Kiểm tra xem bảng và bảng xếp hạng Top 3 Winner có sinh ra đúng 3 hàng hay không
        assertEquals(3, bidHistoryTable.getItems().size());
        assertEquals("Đã tải 3 lượt đặt giá.", bidHistoryHint.getText());
        assertEquals(3, topWinnersBox.getChildren().size()); // Hàm createWinnerRow được bao phủ hoàn toàn tại đây
    }

    @Test
    public void testOnDataReceived_AuctionClosed() {
        controller.initialize(null, null);

        String payload = "AUCTION_CLOSED|winner=BaoHoang|finalPrice=35000000";

        Platform.runLater(() -> controller.onDataReceived(payload));
        waitForFx();

        // Xác minh khi đóng phòng, giao diện tự động lật thẻ kết quả và hiển thị người thắng cuộc chính xác
        assertEquals("FINISHED", status.getText());
        assertEquals("BaoHoang", winnerNameLabel.getText());
        assertEquals("35.000.000 VNĐ", winnerPriceLabel.getText());
        assertEquals("00:00:00", starTime.getText());
    }

    @Test
    public void testOnDataReceived_ImageDataMapping() {
        controller.initialize(null, null);
        StoreItemDataInit.image = "sample_duck.png";

        // Mô phỏng chuỗi Base64 ảnh thật gửi về
        String base64Image = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3, 4});
        String payload = "IMAGE_DATA|sample_duck.png|unused|" + base64Image;

        Platform.runLater(() -> controller.onDataReceived(payload));
        waitForFx();

        // Đảm bảo không ném ra exception và ảnh được tiếp nhận kết xuất thành công vào ImageView
        assertNotNull(image.getImage());
    }

    @Test
    public void testOnDataReceived_AccountInfoJson() {
        controller.initialize(null, null);

        // Nhận chuỗi JSON thông tin tài khoản thay vì chuỗi thuần cắt bởi dấu '|'
        String jsonPayload = "{\"command\":\"ACCOUNT_INFO\",\"balance\":\"8500000\"}";

        Platform.runLater(() -> controller.onDataReceived(jsonPayload));
        waitForFx();

        // Ép định dạng tiền tệ VNĐ hiển thị lên màn hình
        assertEquals("8.500.000 VNĐ", balanceValue.getText());
    }

    @Test
    public void testOnDataReceived_ErrorRouting() {
        controller.initialize(null, null);

        String payload = "ERROR|message=Số dư không đủ để thực hiện đặt giá";

        Platform.runLater(() -> controller.onDataReceived(payload));
        waitForFx();

        // Báo lỗi hiển thị đúng trên thanh trạng thái phụ của người dùng
        assertEquals("Số dư không đủ để thực hiện đặt giá", statusSubLabel.getText());
    }

    private void waitForFx() {
        try {
            Thread.sleep(200); // Khoảng chờ an toàn để JavaFX Thread vẽ lại toàn bộ giao diện bảng xếp hạng và đồ thị
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Tiện ích hỗ trợ định dạng tiền tệ thủ công phục vụ kiểm thử nhanh nội bộ
    private String formatMoney(String money) {
        if (money == null || money.isBlank()) return "0";
        try {
            double value = Double.parseDouble(money.replaceAll("[^\\d.]", ""));
            return String.format("%,.0f", value).replace(",", ".");
        } catch (Exception e) {
            return money;
        }
    }
}