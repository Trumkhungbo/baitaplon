package action.com.bidding.client.controller.SellingJob;

import action.controller.SellingJobs.InvesmentWaitHandle;
import action.controller.main.LobbyHandle;
import action.model.StoreDataInput;
import action.network.SocketClient;
import action.network.SocketListener;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.testfx.framework.junit5.ApplicationTest;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class InvesmentWaitHandleTest extends ApplicationTest {

    private InvesmentWaitHandle controller;

    // Các thành phần UI Mock/Faked để inject vào Controller
    private VBox productListBox;
    private Label totalCountLabel;
    private Label runningCountLabel;
    private Label upcomingCountLabel;
    private Label finishedCountLabel;
    private Label feedbackLabel;
    private TextField searchField;
    private ChoiceBox<String> typeFilter;
    private ChoiceBox<String> statusFilter;

    // Mock / injected objects
    private FakeSocketClient mockSocketInstance;
    private FakeLobbyHandle mockLobbyInstance;

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
            // don't call super (which loads FXML)
        }
    }

    @BeforeAll
    public static void setupSpec() throws Exception {
        // Khởi chạy JavaFX Toolkit song song (TestFX lo phần này)
        System.setProperty("glass.platform", "Monocle");
        System.setProperty("monocle.platform", "Headless");
        System.setProperty("prism.order", "sw");
    }

    @BeforeEach
    public void setUp() throws Exception {
        controller = new InvesmentWaitHandle();

        // Khởi tạo các thành phần UI thực tế (không dùng mock cho UI Control để chạy logic JavaFX bên dưới)
        productListBox = new VBox();
        totalCountLabel = new Label();
        runningCountLabel = new Label();
        upcomingCountLabel = new Label();
        finishedCountLabel = new Label();
        feedbackLabel = new Label();
        searchField = new TextField();
        typeFilter = new ChoiceBox<>(FXCollections.observableArrayList());
        statusFilter = new ChoiceBox<>(FXCollections.observableArrayList());

        // Inject các trường FXML thông qua Reflection (hoặc gán trực tiếp nếu chỉnh sửa access modifier)
        setPrivateField(controller, "productListBox", productListBox);
        setPrivateField(controller, "totalCountLabel", totalCountLabel);
        setPrivateField(controller, "runningCountLabel", runningCountLabel);
        setPrivateField(controller, "upcomingCountLabel", upcomingCountLabel);
        setPrivateField(controller, "finishedCountLabel", finishedCountLabel);
        setPrivateField(controller, "feedbackLabel", feedbackLabel);
        setPrivateField(controller, "searchField", searchField);
        setPrivateField(controller, "typeFilter", typeFilter);
        setPrivateField(controller, "statusFilter", statusFilter);

        // Thiết lập Fake/Mock cho các lớp tĩnh (avoid Mockito inline mocking):
        mockSocketInstance = new FakeSocketClient();
        // inject mock socket client via test seam
        SocketClient.setInstance(mockSocketInstance);

        // set StoreDataInput.username directly
        StoreDataInput.username = "test_seller";

        // set LobbyHandle.instance via reflection using a fake implementation
        mockLobbyInstance = new FakeLobbyHandle();
        try {
            java.lang.reflect.Field lobbyInstanceField = LobbyHandle.class.getDeclaredField("instance");
            lobbyInstanceField.setAccessible(true);
            lobbyInstanceField.set(null, mockLobbyInstance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    public void tearDown() {
        // Restore injected static singletons
        SocketClient.setInstance(null);
        StoreDataInput.username = null;
        try {
            java.lang.reflect.Field lobbyInstanceField = LobbyHandle.class.getDeclaredField("instance");
            lobbyInstanceField.setAccessible(true);
            lobbyInstanceField.set(null, null);
        } catch (Exception ignored) {
        }
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // --- TEST SECTIONS ---

    @Test
    public void testInitialize() {
        controller.initialize(null, null);

        // Kiểm tra xem đã đăng ký Listener và gửi đúng lệnh lấy data chưa
        assertTrue(mockSocketInstance.listenersAdded.contains(controller));
        assertTrue(mockSocketInstance.sent.contains("WATCH|"));

        // Kiểm tra khởi tạo giá trị mặc định cho bộ lọc
        assertEquals("Tất cả danh mục", typeFilter.getValue());
        assertEquals("Tất cả trạng thái", statusFilter.getValue());
    }

    @Test
    public void testRefreshProducts() {
        controller.refreshProducts();
        JsonObject expectedReq = new JsonObject();
        expectedReq.addProperty("command", "LIST_MY_AUCTIONS");
        expectedReq.addProperty("sellerUsername", "test_seller");

        assertTrue(mockSocketInstance.sent.contains(expectedReq.toString()));
    }

    @Test
    public void testOnDataReceived_EmptyOrNull() {
        // Test biên với dữ liệu rỗng
        controller.onDataReceived(null);
        controller.onDataReceived("");
        controller.onDataReceived("   ");

        // Không có ngoại lệ xảy ra là đạt yêu cầu
        assertTrue(productListBox.getChildren().isEmpty());
    }

    @Test
    public void testOnDataReceived_MyAuctions_Success_And_Filtering() throws Exception {
        // Giả lập chuỗi payload trả về từ server (bao gồm các loại trạng thái khác nhau để bao phủ switch-case)
        // Cấu trúc: auctionId:itemId:itemName:itemType:startPrice:currentPrice:status:startDate:startClockTime:durationMinutes:bidCount:imageUrl:description:info1:info2:startMillis:endMillis
        String fakeData = "MY_AUCTIONS|" +
                "1:101:Dien Thoai iPhone:ELECTRONICS:10000000:12000000:RUNNING:2026-05-30:20:00:60:5:img1.jpg:desc:i1:i2:111:222;" +
                "2:102:Tranh Phuong Hoang:ART:5000000:5000000:PENDING:2026-05-31:09:00:120:0:img2.jpg:desc:i1:i2:333:444;" +
                "3:103:Sieu Xe BMW:VEHICLE:2000000000:2500000000:FINISHED:2026-05-28:10:00:180:12:img3.jpg:desc:i1:i2:555:666";

        // Chạy trên JavaFX thread để tương thích với Platform.runLater trong controller
        Platform.runLater(() -> controller.onDataReceived(fakeData));
        waitForFxEvents();

        // 1. Kiểm tra tổng số lượng đã render (3 dòng)
        assertEquals(3, productListBox.getChildren().size());
        assertEquals("3", totalCountLabel.getText());
        assertEquals("1", runningCountLabel.getText()); // RUNNING
        assertEquals("1", upcomingCountLabel.getText()); // PENDING
        assertEquals("1", finishedCountLabel.getText()); // FINISHED

        // 2. Test chức năng Tìm kiếm (Search)
        Platform.runLater(() -> {
            searchField.setText("iPhone");
            controller.applyFilters();
        });
        waitForFxEvents();
        assertEquals(1, productListBox.getChildren().size()); // Chỉ còn lại dòng iPhone

        // 3. Test chức năng Lọc Danh Mục (Type Filter)
        Platform.runLater(() -> {
            searchField.setText("");
            typeFilter.setValue("Nghệ thuật");
            controller.applyFilters();
        });
        waitForFxEvents();
        assertEquals(1, productListBox.getChildren().size()); // Chỉ còn tranh nghệ thuật

        // 4. Test chức năng Lọc Trạng Thái (Status Filter)
        Platform.runLater(() -> {
            typeFilter.setValue("Tất cả danh mục");
            statusFilter.setValue("Đã kết thúc");
            controller.applyFilters();
        });
        waitForFxEvents();
        assertEquals(1, productListBox.getChildren().size()); // Chỉ còn xe BMW (FINISHED)
    }

    @Test
    public void testOnDataReceived_UpdatesAndTriggers() {
        // Test khi nhận được gói lệnh cập nhật danh sách
        Platform.runLater(() -> controller.onDataReceived("AUCTION_LIST|"));
        waitForFxEvents();
        // ensure at least one request was sent
        assertTrue(mockSocketInstance.sent.size() >= 1);

        Platform.runLater(() -> controller.onDataReceived("AUCTION_PAYMENT_UPDATE|"));
        waitForFxEvents();
        assertTrue(mockSocketInstance.sent.size() >= 2);
    }

    @Test
    public void testOnDataReceived_SuccessFeedback() {
        Platform.runLater(() -> controller.onDataReceived("DELETE_AUCTION_SUCCESS|"));
        waitForFxEvents();
        assertEquals("Xóa sản phẩm thành công.", feedbackLabel.getText());

        Platform.runLater(() -> controller.onDataReceived("UPDATE_AUCTION_SUCCESS|"));
        waitForFxEvents();
        assertEquals("Cập nhật sản phẩm thành công.", feedbackLabel.getText());

        Platform.runLater(() -> controller.onDataReceived("ERROR|Lỗi kết nối database"));
        waitForFxEvents();
        assertEquals("Lỗi kết nối database", feedbackLabel.getText());
    }

    @Test
    public void testGoToAddProduct_Success() throws IOException {
        controller.goToAddProduct();
        // verify fake received the call
        assertTrue(mockLobbyInstance.calls.contains("/views/InvesterSell.fxml"));
    }

    @Test
    public void testDeleteProduct_LockedStatus() throws Exception {
        // Giả lập nạp 1 sản phẩm đang RUNNING vào danh sách private `products`
        String fakeData = "MY_AUCTIONS|1:101:Item:ELECTRONICS:10:10:RUNNING:date:time:60:0:img:desc:i1:i2:1:2";
        Platform.runLater(() -> controller.onDataReceived(fakeData));
        waitForFxEvents();

        // Gọi hàm delete thông qua invoke method hoặc kích hoạt thông qua việc gọi logic xóa trực tiếp sản phẩm bị lock
        // Vì deleteProduct là private, ta có thể test gián tiếp thông qua việc click nút Xóa trên Row (nếu mock đầy đủ Event)
        // Hoặc tối ưu qua Reflection để đạt coverage trực tiếp cho nhánh kiểm tra trạng thái:
        java.lang.reflect.Method deleteMethod = InvesmentWaitHandle.class.getDeclaredMethod("deleteProduct", Class.forName("action.controller.SellingJobs.InvesmentWaitHandle$MyProductRowData"));
        deleteMethod.setAccessible(true);

        // Lấy danh sách sản phẩm hiện tại trong controller ra để test
        java.lang.reflect.Field productsField = InvesmentWaitHandle.class.getDeclaredField("products");
        productsField.setAccessible(true);
        java.util.List<?> currentProducts = (java.util.List<?>) productsField.get(controller);
        Object runningProduct = currentProducts.get(0);

        Platform.runLater(() -> {
            try {
                deleteMethod.invoke(controller, runningProduct);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        waitForFxEvents();

        // Kiểm tra xem có bị chặn và hiển thị feedback không, đồng thời không gửi request lên Server
        assertEquals("Bạn không thể xóa vì phiên đấu giá đang diễn ra hoặc đã kết thúc.", feedbackLabel.getText());
        assertTrue(mockSocketInstance.sent.stream().noneMatch(s -> s.contains("DELETE_AUCTION")));
    }

    @Test
    public void testLoadImage_TriggerAndCallback() throws Exception {
        // Test hàm xử lý ảnh bất đồng bộ qua SocketListener ẩn bên trong hàm loadImage
        java.lang.reflect.Method loadImageMethod = InvesmentWaitHandle.class.getDeclaredMethod("loadImage", String.class, javafx.scene.image.ImageView.class);
        loadImageMethod.setAccessible(true);

        javafx.scene.image.ImageView mockImageView = new javafx.scene.image.ImageView();

        Platform.runLater(() -> {
            try {
                loadImageMethod.invoke(controller, "test_avatar.jpg", mockImageView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        waitForFxEvents();

        // Kiểm tra xem đã gửi lệnh yêu cầu ảnh lên server chưa
        assertTrue(mockSocketInstance.sent.contains("GET_IMAGE|test_avatar.jpg"));
    }

    // Helper hỗ trợ đồng bộ hóa luồng xử lý UI JavaFX (Chờ Platform.runLater chạy xong)
    private void waitForFxEvents() {
        try {
            Thread.sleep(200); // Đợi ngắn để JavaFX Thread xử lý hết hàng đợi sự kiện
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}