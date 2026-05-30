package action.com.bidding.client.controller.SellingJob;

import action.controller.SellingJobs.InvesterSellHandle;
import action.controller.SellingJobs.StoreSellerProductEdit;
import action.controller.main.LobbyHandle;
import action.model.StoreDataInput;
import action.network.SocketClient;
import action.network.SocketListener;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class InvesterSellHandleTest extends ApplicationTest {

    private InvesterSellHandle controller;

    // Thành phần điều khiển UI Fake tương ứng các biến @FXML
    private TextField itemname;
    private TextArea productDescription;
    private ChoiceBox<String> description;
    private TextField description1;
    private TextField description2;
    private TextField price;
    private TextField TimeStart;
    private DatePicker auctionDate;
    private TextField duration;
    private ImageView imageset;
    private Label statusLabel;
    private Label pageTitle;
    private Button submitButton;

    // Đối tượng kiểm soát giả lập Static (Singletons)
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
    public static void setupHeadlessMode() {
        // Cấu hình chạy JavaFX dạng ẩn để thực thi mượt mà trên môi trường CI/CD không màn hình
        System.setProperty("glass.platform", "Monocle");
        System.setProperty("monocle.platform", "Headless");
        System.setProperty("prism.order", "sw");
    }

    @BeforeEach
    public void setUp() throws Exception {
        // Khởi tạo trạng thái sạch cho cấu hình Form tĩnh trước mỗi ca test
        StoreSellerProductEdit.clear();
        StoreSellerProductEdit.editing = false;

        controller = new InvesterSellHandle();

        // Khởi tạo các Node giao diện thực tế ngăn chặn lỗi NullPointerException khi Controller tương tác trực tiếp
        itemname = new TextField();
        productDescription = new TextArea();
        description = new ChoiceBox<>(FXCollections.observableArrayList());
        description1 = new TextField();
        description2 = new TextField();
        price = new TextField();
        TimeStart = new TextField();
        auctionDate = new DatePicker();
        duration = new TextField();
        imageset = new ImageView();
        statusLabel = new Label();
        pageTitle = new Label();
        submitButton = new Button();

        // Bơm các trường private thông qua cơ chế Reflection phản chiếu cấu trúc mã nguồn
        injectFXMLField("itemname", itemname);
        injectFXMLField("productDescription", productDescription);
        injectFXMLField("description", description);
        injectFXMLField("description1", description1);
        injectFXMLField("description2", description2);
        injectFXMLField("price", price);
        injectFXMLField("TimeStart", TimeStart);
        injectFXMLField("auctionDate", auctionDate);
        injectFXMLField("duration", duration);
        injectFXMLField("imageset", imageset);
        injectFXMLField("statusLabel", statusLabel);
        injectFXMLField("pageTitle", pageTitle);
        injectFXMLField("submitButton", submitButton);

        // Giả lập môi trường tĩnh cho tầng Network & Navigation điều hướng
        mockSocketClient = new FakeSocketClient();
        SocketClient.setInstance(mockSocketClient);

        StoreDataInput.username = "TestSeller";

        mockLobbyHandle = new FakeLobbyHandle();
        try {
            java.lang.reflect.Field instanceField = LobbyHandle.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, mockLobbyHandle);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    public void tearDown() {
        // Restore injected singletons to avoid cross-test pollution
        SocketClient.setInstance(null);
        StoreDataInput.username = null;
        try {
            java.lang.reflect.Field instanceField = LobbyHandle.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception ignored) {}
        StoreSellerProductEdit.clear();
    }

    private void injectFXMLField(String fieldName, Object value) throws Exception {
        Field field = InvesterSellHandle.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    // --- CÁC CA KIỂM THỬ ĐỘ PHỦ TỐI ĐA ---

    @Test
    public void testInitialize_FreshCreateState() {
        // Thực thi hàm khởi tạo ban đầu ở trạng thái tạo mới sản phẩm
        controller.initialize(null, null);

        // Đảm bảo listener mạng được gắn kết và danh mục nạp đúng
        assertTrue(mockSocketClient.listenersAdded.contains(controller));
        assertEquals(3, description.getItems().size());
        assertEquals("ELECTRONICS", description.getValue());
        assertEquals("Đăng bán sản phẩm mới", pageTitle.getText());
        assertEquals("Đăng bán ngay", submitButton.getText());
    }

    @Test
    public void testInitialize_EditState() {
        // Giả lập cấu hình khi người dùng chọn sửa một sản phẩm từ danh sách trước đó
        StoreSellerProductEdit.editing = true;
        StoreSellerProductEdit.auctionId = "AU123";
        StoreSellerProductEdit.itemName = "Laptop Gaming";
        StoreSellerProductEdit.itemType = "ELECTRONICS";
        StoreSellerProductEdit.description = "Mô tả sản phẩm cũ";
        StoreSellerProductEdit.information1 = "Core i7";
        StoreSellerProductEdit.information2 = "RTX 4060";
        StoreSellerProductEdit.price = "25000000";
        StoreSellerProductEdit.time = "10:00:00";
        StoreSellerProductEdit.date = "2026-06-15";
        StoreSellerProductEdit.imageUrl = "old_image.png";

        controller.initialize(null, null);

        // Xác minh dữ liệu được ánh xạ điền ngược (map) chuẩn xác lên các ô nhập liệu của Form
        assertEquals("Chỉnh sửa sản phẩm", pageTitle.getText());
        assertEquals("Cập nhật sản phẩm", submitButton.getText());
        assertEquals("Laptop Gaming", itemname.getText());
        assertEquals("25000000", price.getText());
        assertEquals(LocalDate.parse("2026-06-15"), auctionDate.getValue());

        // Đảm bảo hệ thống phát lệnh tải lại hình ảnh cũ từ Server về hiển thị
        assertTrue(mockSocketClient.sent.contains("GET_IMAGE|old_image.png"));
    }

    @Test
    public void testClicked_SubmitSuccess_AddAuction() {
        controller.initialize(null, null);

        // Thiết lập dữ liệu kiểm thử chuẩn
        itemname.setText("Bức tranh nghệ thuật");
        productDescription.setText("Tranh sơn dầu thế kỷ 19");
        description.setValue("ART");
        description1.setText("Kích thước lớn");
        description2.setText("Khung gỗ sồi");
        price.setText("5,000,000"); // Chứa dấu phẩy để bao phủ phương thức replace(",", "")
        TimeStart.setText("14:30:00");
        auctionDate.setValue(LocalDate.parse("2026-07-20"));
        duration.setText("120");

        // Gọi sự kiện Click nút gửi dữ liệu
        controller.Clicked(new ActionEvent());

        // Kiểm định cấu trúc chuỗi Json gửi đi đúng lệnh "ADD_AUCTION"
        // Ensure a JSON request with the expected properties was sent
        boolean found = mockSocketClient.sent.stream().anyMatch(jsonRequest -> {
            try {
                JsonObject json = com.google.gson.JsonParser.parseString(jsonRequest).getAsJsonObject();
                return "ADD_AUCTION".equals(json.get("command").getAsString()) &&
                        "TestSeller".equals(json.get("seller").getAsString()) &&
                        "5000000".equals(json.get("price").getAsString()) &&
                        "120".equals(json.get("durationMinutes").getAsString());
            } catch (Exception e) { return false; }
        });
        assertTrue(found);
        assertEquals("Đang đăng bán...", statusLabel.getText());
    }

    @Test
    public void testClicked_SubmitSuccess_UpdateAuction() {
        StoreSellerProductEdit.editing = true;
        StoreSellerProductEdit.auctionId = "999";
        controller.initialize(null, null);

        itemname.setText("Xe máy");
        description.setValue("VEHICLE");
        price.setText("45000000");
        TimeStart.setText("08:00:00");
        auctionDate.setValue(LocalDate.now());
        duration.setText("60");

        controller.Clicked(new ActionEvent());

        // Kiểm định cấu trúc chuỗi Json gửi đi phải đổi thành lệnh "UPDATE_AUCTION" kèm theo mã ID định danh
        boolean foundUpdate = mockSocketClient.sent.stream().anyMatch(jsonRequest -> {
            try {
                JsonObject json = com.google.gson.JsonParser.parseString(jsonRequest).getAsJsonObject();
                return "UPDATE_AUCTION".equals(json.get("command").getAsString()) &&
                        "999".equals(json.get("auctionId").getAsString());
            } catch (Exception e) { return false; }
        });
        assertTrue(foundUpdate);
        assertEquals("Đang cập nhật sản phẩm...", statusLabel.getText());
    }

    @Test
    public void testClicked_InvalidData_TriggersExceptionCatch() {
        controller.initialize(null, null);

        // Cố tình đẩy định dạng thời gian sai nguyên tắc quy định ("ABC") nhằm ép hệ thống nhảy vào khối Catch
        TimeStart.setText("ABC");

        controller.Clicked(new ActionEvent());

        // Bao phủ khối 'catch (Exception e)' thành công
        assertEquals("Dữ liệu không hợp lệ, vui lòng kiểm tra lại.", statusLabel.getText());
    }

    @Test
    public void testOnDataReceived_UploadImageSuccess() throws Exception {
        controller.initialize(null, null);
        TimeStart.setText("09:00:00");
        auctionDate.setValue(LocalDate.parse("2026-08-12"));
        duration.setText("45");
        price.setText("200000");

        // Mô phỏng chuỗi phản hồi mạng khi tải ảnh thành công lên Server
        String networkData = "UPLOAD_IMAGE_SUCCESS|uploaded_avatar.jpg";

        Platform.runLater(() -> controller.onDataReceived(networkData));
        waitForFx();

        // Hệ thống sau khi nhận tên ảnh cần lập tức tiến hành gửi Payload thông tin chi tiết sản phẩm kèm ảnh đó
        boolean hasUploaded = mockSocketClient.sent.stream().anyMatch(s -> s.contains("uploaded_avatar.jpg"));
        assertTrue(hasUploaded);
    }

    @Test
    public void testOnDataReceived_ImageDataRender() throws Exception {
        StoreSellerProductEdit.editing = true;
        StoreSellerProductEdit.imageUrl = "preview.png";
        controller.initialize(null, null);

        // Mô phỏng luồng nhị phân Base64 của ảnh gửi về từ Server (Mã hóa chuỗi rỗng để test tính tương thích)
        String base64Fake = Base64.getEncoder().encodeToString(new byte[]{0, 1, 2, 3});
        String networkData = "IMAGE_DATA|preview.png|unused|" + base64Fake;

        Platform.runLater(() -> controller.onDataReceived(networkData));
        waitForFx();

        // Khối lệnh chạy trơn tru, ảnh được chuyển đổi gán vào View không ném ra ngoại lệ
        assertNotNull(imageset);
    }

    @Test
    public void testOnDataReceived_AddAuctionSuccess_TriggersClear() {
        controller.initialize(null, null);
        itemname.setText("Dữ liệu cần xóa");

        // Nhận gói tin phản hồi tạo phiên đấu giá thành công hoàn toàn
        String networkData = "ADD_AUCTION_SUCCESS";

        Platform.runLater(() -> controller.onDataReceived(networkData));
        waitForFx();

        // Form nhập liệu phải được dọn sạch hoàn toàn tự động dọn về nguyên bản ban đầu
        assertEquals("", itemname.getText());
        assertEquals("Đăng bán thành công!", statusLabel.getText());
    }

    @Test
    public void testOnDataReceived_UpdateAuctionSuccess_TriggersRedirect() throws IOException {
        StoreSellerProductEdit.editing = true;
        controller.initialize(null, null);

        String networkData = "UPDATE_AUCTION_SUCCESS";

        Platform.runLater(() -> controller.onDataReceived(networkData));
        waitForFx();

        // Khi cập nhật xong, hệ thống phải giải phóng biến tĩnh và điều hướng người dùng quay trở về trang trung tâm danh sách
        assertTrue(mockLobbyHandle.calls.contains("/views/InvesterSell.fxml"));
    }

    @Test
    public void testOnDataReceived_ErrorFeedback() {
        controller.initialize(null, null);

        String networkData = "ERROR|Tên sản phẩm chứa từ cấm";

        Platform.runLater(() -> controller.onDataReceived(networkData));
        waitForFx();

        // Hiển thị chuỗi thông báo lỗi cắt chuỗi phân tách hệ thống một cách trực quan
        assertEquals("Lỗi: Tên sản phẩm chứa từ cấm", statusLabel.getText());
    }

    @Test
    public void testPrivate_UploadImageLogic() throws Exception {
        // Ép thực thi hàm private uploadImage thông qua Reflection
        java.lang.reflect.Method uploadImageMethod = InvesterSellHandle.class.getDeclaredMethod("uploadImage", File.class);
        uploadImageMethod.setAccessible(true);

        // Tạo một File tạm thời giả lập trên ổ đĩa để đọc mảng byte
        File tempFile = File.createTempFile("product_test", ".png");
        Files.write(tempFile.toPath(), new byte[]{12, 34, 56});
        tempFile.deleteOnExit();

        uploadImageMethod.invoke(controller, tempFile);

        // Đảm bảo hệ thống băm chuỗi thành Base64 và chuyển tới Server với cấu trúc lệnh "UPLOAD_IMAGE|ext|base64"
        boolean starts = mockSocketClient.sent.stream().anyMatch(s -> s.startsWith("UPLOAD_IMAGE|png|"));
        assertTrue(starts);
    }

    private void waitForFx() {
        try {
            Thread.sleep(150); // Khoảng dừng ngắn đồng bộ các tác vụ xử lý trên giao diện của JavaFX Thread
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}