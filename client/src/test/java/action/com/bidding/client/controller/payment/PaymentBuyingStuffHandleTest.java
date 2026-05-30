package action.com.bidding.client.controller.payment;

import action.controller.payment.PaymentBuyingStuffHandle;
import action.model.StoreDataInput;
import action.model.StoreItemDataInit;
import action.network.SocketClient;
import action.network.SocketClientWrapper; // Import the new wrapper
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class PaymentBuyingStuffHandleTest {

    private PaymentBuyingStuffHandle controller;

    // UI Elements (will be injected by FXML loader)
    private Label paymentStatusLabel;
    private Label feedbackLabel;
    private Label itemNameLabel;
    private Label auctionCodeLabel;
    private Label sellerLabel;
    private Label auctionStatusLabel;
    private Label winnerLabel;
    private Label priceLabel;
    private Label balanceLabel;
    private Label balanceAfterLabel;
    private Button payButton;

    // Mocks
    @Mock
    private SocketClientWrapper mockSocketClientWrapper;
    @Mock
    private SocketClient mockSocketClient; // The actual SocketClient instance mocked within the wrapper

    // Use Mockito's MockedStatic for static methods if absolutely necessary and not causing issues
    // For StoreItemDataInit and StoreDataInput, we set public static fields directly.
    // private MockedStatic<StoreItemDataInit> mockedStaticStoreItemDataInit;
    // private MockedStatic<StoreDataInput> mockedStaticStoreDataInput;


    @Start
    public void start(Stage stage) throws Exception {
        // Initialize mocks - this usually happens with @MockitoExtension or similar
        // For @Start, we manually init if not using @ExtendWith(MockitoExtension.class)
        MockitoAnnotations.initMocks(this); // This line requires an import for MockitoAnnotations

        // Setup the mock wrapper to return our mocked SocketClient
        // This is crucial: when paymentBuyingStuffHandle calls socketClientWrapper.getActualSocketClientInstance(),
        // it should receive our mockSocketClient.
        doReturn(mockSocketClient).when(mockSocketClientWrapper).getActualSocketClientInstance();

        // Set static fields directly (since they are public)
        StoreItemDataInit.name = "Test Item";
        StoreItemDataInit.description = "12345"; // Auction Code
        StoreDataInput.username = "testUser";

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Payment_BuyingStuff.fxml"));
        // Temporarily, we will set a custom controller factory to inject our mock wrapper
        loader.setControllerFactory(c -> {
            try {
                PaymentBuyingStuffHandle instance = (PaymentBuyingStuffHandle) c.getDeclaredConstructor().newInstance();
                // Use reflection to set the private final socketClientWrapper field
                java.lang.reflect.Field field = PaymentBuyingStuffHandle.class.getDeclaredField("socketClientWrapper");
                field.setAccessible(true);
                field.set(instance, mockSocketClientWrapper);
                return instance;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Parent root = loader.load();
        controller = loader.getController();

        // Get UI elements from the loaded FXML
        paymentStatusLabel = (Label) root.lookup("#paymentStatusLabel");
        feedbackLabel = (Label) root.lookup("#feedbackLabel");
        itemNameLabel = (Label) root.lookup("#itemNameLabel");
        auctionCodeLabel = (Label) root.lookup("#auctionCodeLabel");
        sellerLabel = (Label) root.lookup("#sellerLabel");
        auctionStatusLabel = (Label) root.lookup("#auctionStatusLabel");
        winnerLabel = (Label) root.lookup("#winnerLabel");
        priceLabel = (Label) root.lookup("#priceLabel");
        balanceLabel = (Label) root.lookup("#balanceLabel");
        balanceAfterLabel = (Label) root.lookup("#balanceAfterLabel");
        payButton = (Button) root.lookup("#payButton");


        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @BeforeEach
    public void setup() {
        // Reset mocks before each test
        Mockito.reset(mockSocketClientWrapper, mockSocketClient);

        // Re-setup mock behavior for the wrapper
        doReturn(mockSocketClient).when(mockSocketClientWrapper).getActualSocketClientInstance();

        // Reset static fields for each test
        StoreItemDataInit.name = "Test Item";
        StoreItemDataInit.description = "12345";
        StoreDataInput.username = "testUser";

        // Ensure JavaFX platform is ready for UI updates
        Platform.runLater(() -> {
            // Any UI element setup or state reset that needs to happen on JavaFX thread
        });
    }

    @AfterEach
    public void tearDownEach() {
        // No static mocks to close here, as we removed them.
    }

    @Test
    public void testPaymentLabelsAndButtonsExist(FxRobot robot) {
        assertNotNull(itemNameLabel, "Item name label must exist");
        assertNotNull(priceLabel, "Price label must exist");
        assertNotNull(payButton, "Pay button must exist");
        assertEquals("Test Item", itemNameLabel.getText());
        assertEquals("#AU12345", auctionCodeLabel.getText());
    }

    @Test
    public void testInitialize_makesCorrectRequests() throws InterruptedException {
        // Chúng ta chủ động reset trí nhớ của mock ở đây để bắt đầu đếm lại từ đầu
        Mockito.reset(mockSocketClientWrapper);

        // Gọi lại hàm initialize() một cách thủ công để xem nó làm gì
        controller.initialize(null, null);

        // Giờ thì ta kiểm tra xem mock có ghi nhận đúng các request không
        verify(mockSocketClientWrapper, times(1)).addListener(controller);
        verify(mockSocketClientWrapper, times(1)).requestData("GET_AUCTION_DETAIL|12345");
        verify(mockSocketClientWrapper, times(1)).requestData("GET_WINNER|12345");

        JsonObject expectedAccountReq = new JsonObject();
        expectedAccountReq.addProperty("command", "GET_ACCOUNTINFORMATION");
        expectedAccountReq.addProperty("username", "testUser");
        verify(mockSocketClientWrapper, times(1)).requestData(expectedAccountReq.toString());
    }
    @Test
    public void testPayNow_sendsCorrectRequest(FxRobot robot) throws InterruptedException {
        // 1. BƯỚC CHUẨN BỊ: Làm cho nút Pay sáng lên (Enable)
        StoreDataInput.username = "testUser";
        controller.onDataReceived("AUCTION_DETAIL|currentPrice=500.0|status=FINISHED|seller=seller123|itemName=New Item|id=12345");
        controller.onDataReceived("WINNER_INFO|winner=testUser");
        controller.onDataReceived("{\"command\":\"ACCOUNT_INFO\",\"balance\":\"1000.0\"}");

        // Chờ JavaFX cập nhật giao diện (Bật nút)
        CountDownLatch waitLatch = new CountDownLatch(1);
        Platform.runLater(waitLatch::countDown);
        assertTrue(waitLatch.await(5, TimeUnit.SECONDS), "Timeout waiting for JavaFX thread");

        // Chắc chắn nút đã bật trước khi bấm
        assertFalse(payButton.isDisabled(), "Button MUST be enabled before clicking");
        Mockito.clearInvocations(mockSocketClientWrapper);
        // 2. BƯỚC HÀNH ĐỘNG: Cho robot bấm nút
        robot.clickOn(payButton); // Simulate button click

        // 3. BƯỚC KIỂM TRA: Xem đã gửi đúng request chưa
        verify(mockSocketClientWrapper, times(1)).requestData("PAY_AUCTION|12345");
    }

    @Test
    public void testGoBack_removesListenerAndNavigates(FxRobot robot) throws IOException {
        controller.goBack(null); // Simulate goBack
        verify(mockSocketClientWrapper, times(1)).removeListener(controller);    }

    @Test
    public void testOnDataReceived_nullOrBlankData() throws InterruptedException {
        controller.onDataReceived(null);
        controller.onDataReceived("");
        verify(mockSocketClientWrapper, never()).requestData(anyString());    }

    @Test
    public void testOnDataReceived_auctionDetailUpdate() throws InterruptedException {
        String data = "AUCTION_DETAIL|currentPrice=1000.0|status=FINISHED|seller=seller123|itemName=New Item|id=67890";
        controller.onDataReceived(data);
        CountDownLatch waitLatch = new CountDownLatch(1);
        Platform.runLater(waitLatch::countDown);
        assertTrue(waitLatch.await(5, TimeUnit.SECONDS));

        assertEquals("New Item", itemNameLabel.getText());
        assertEquals("#AU67890", auctionCodeLabel.getText());
        assertEquals("Người bán: seller123", sellerLabel.getText());
        assertEquals("Trạng thái phiên: FINISHED", auctionStatusLabel.getText());
        assertEquals("1,000 VND", priceLabel.getText());
        // balanceAfterLabel depends on currentBalance, which is 0.0 initially, so it should be -1,000 VND
        assertEquals("-1,000 VND", balanceAfterLabel.getText());
        // Verify refreshPayState was called (implicitly by checking button state)
        assertTrue(payButton.isDisabled()); // Initially disabled as currentBalance is 0
    }

    @Test
    public void testOnDataReceived_winnerInfoUpdate() throws InterruptedException {
        String data = "WINNER_INFO|winner=testUser";
        controller.onDataReceived(data);
        CountDownLatch waitLatch = new CountDownLatch(1);
        Platform.runLater(waitLatch::countDown);
        assertTrue(waitLatch.await(5, TimeUnit.SECONDS));

        assertEquals("Người thắng: testUser", winnerLabel.getText());
        // refreshPayState should be called, but button still disabled if other conditions not met
        assertTrue(payButton.isDisabled());
    }

    @Test
    public void testOnDataReceived_accountInfoUpdate() throws InterruptedException {
        String data = "{\"command\":\"ACCOUNT_INFO\",\"balance\":\"5000.0\"}";
        controller.onDataReceived(data);
        CountDownLatch waitLatch = new CountDownLatch(1);
        Platform.runLater(waitLatch::countDown);
        assertTrue(waitLatch.await(5, TimeUnit.SECONDS));
        assertEquals("5,000 VND", balanceLabel.getText());
        assertEquals("5,000 VND", balanceAfterLabel.getText());
        assertTrue(payButton.isDisabled());
    }

    @Test
    public void testOnDataReceived_payAuctionResult_success() throws InterruptedException {
        // Đảm bảo username đúng để nó hiểu mình là người mua
        StoreDataInput.username = "testUser";

        // BƯỚC 1: Đẩy HÀNG LOẠT dữ liệu giả lập vào trực tiếp (Không bọc runLater)
        // Cấp dữ liệu phiên đấu giá
        controller.onDataReceived("AUCTION_DETAIL|currentPrice=500.0|status=FINISHED|seller=seller123|itemName=New Item|id=12345");
        // Cấp dữ liệu người thắng
        controller.onDataReceived("WINNER_INFO|winner=testUser");
        // Cấp số dư ban đầu
        controller.onDataReceived("{\"command\":\"ACCOUNT_INFO\",\"balance\":\"1000.0\"}");
        // Nhận được thông báo thanh toán thành công
        String successData = "PAY_AUCTION_RESULT|status=SUCCESS|newStatus=PAID|buyerBalance=500.0|message=Payment successful!";
        controller.onDataReceived(successData);

        // BƯỚC 2: CHỜ JAVAFX CẬP NHẬT TẤT CẢ
        // Tung ra 1 cái chốt duy nhất để đợi toàn bộ khối dữ liệu kia được xử lý lên UI
        CountDownLatch waitLatch = new CountDownLatch(1);
        Platform.runLater(waitLatch::countDown);
        assertTrue(waitLatch.await(5, TimeUnit.SECONDS), "Timeout waiting for JavaFX thread");

        // BƯỚC 3: KIỂM TRA
        assertEquals("ĐÃ THANH TOÁN", paymentStatusLabel.getText());
        assertEquals("500 VND", balanceLabel.getText());
        assertEquals("500 VND", balanceAfterLabel.getText()); // balanceAfterLabel shows currentBalance if paid
        assertEquals("Payment successful!", feedbackLabel.getText());
        assertTrue(payButton.isDisabled(), "Nút phải mờ đi sau khi đã thanh toán");
    }

    @Test
    public void testOnDataReceived_payAuctionResult_failed() throws InterruptedException {
        String data = "PAY_AUCTION_RESULT|status=FAILED|message=Insufficient funds!";

        controller.onDataReceived(data);;
        CountDownLatch waitLatch = new CountDownLatch(1);
        Platform.runLater(waitLatch::countDown);
        assertTrue(waitLatch.await(5, TimeUnit.SECONDS));


        assertEquals("Insufficient funds!", feedbackLabel.getText());
        // Status should remain "CHỜ THANH TOÁN" or whatever it was before
        assertEquals("CHỜ THANH TOÁN", paymentStatusLabel.getText());
        assertTrue(payButton.isDisabled()); // Still disabled if failed
    }

    @Test
    public void testOnDataReceived_payAuctionResult_canceled() throws InterruptedException {
        String data = "PAY_AUCTION_RESULT|status=FAILED|newStatus=CANCELED|message=Auction canceled!";
        controller.onDataReceived(data);;
        CountDownLatch waitLatch = new CountDownLatch(1);
        Platform.runLater(waitLatch::countDown);
        assertTrue(waitLatch.await(5, TimeUnit.SECONDS));

        assertEquals("Auction canceled!", feedbackLabel.getText());
        assertEquals("ĐÃ HỦY", paymentStatusLabel.getText());
        assertTrue(payButton.isDisabled());
    }

    @Test
    public void testOnDataReceived_errorHandling() throws InterruptedException {
        String data = "ERROR|Something went wrong!";
        controller.onDataReceived(data);;
        CountDownLatch waitLatch = new CountDownLatch(1);
        Platform.runLater(waitLatch::countDown);
        assertTrue(waitLatch.await(5, TimeUnit.SECONDS));

        assertEquals("Something went wrong!", feedbackLabel.getText());
    }

    @Test
    public void testRefreshPayState_payButtonEnabledScenario() throws InterruptedException {
        StoreDataInput.username = "testUser";

        controller.onDataReceived("AUCTION_DETAIL|currentPrice=500.0|status=FINISHED|seller=seller123|itemName=New Item|id=12345");
        controller.onDataReceived("WINNER_INFO|winner=testUser");
        controller.onDataReceived("{\"command\":\"ACCOUNT_INFO\",\"balance\":\"1000.0\"}");

        CountDownLatch waitLatch = new CountDownLatch(1);
        Platform.runLater(waitLatch::countDown); // Lệnh này xếp hàng sau cùng, đảm bảo UI cập nhật xong mới chạy
        assertTrue(waitLatch.await(5, TimeUnit.SECONDS), "Timeout waiting for JavaFX thread");

        assertFalse(payButton.isDisabled(), "Pay button should be enabled when user is winner, auction finished, and balance sufficient.");
        assertEquals("CHỜ THANH TOÁN", paymentStatusLabel.getText());
    }

    @Test
    public void testRefreshPayState_payButtonDisabled_notWinner() throws InterruptedException {
        StoreDataInput.username = "anotherUser"; // Change winner
        controller.onDataReceived("AUCTION_DETAIL|currentPrice=500.0|status=FINISHED|seller=seller123|itemName=New Item|id=12345");
        controller.onDataReceived("WINNER_INFO|winner=testUser");
        controller.onDataReceived("{\"command\":\"ACCOUNT_INFO\",\"balance\":\"1000.0\"}");
        CountDownLatch waitLatch = new CountDownLatch(1);
        Platform.runLater(waitLatch::countDown);
        assertTrue(waitLatch.await(5, TimeUnit.SECONDS));
        assertTrue(payButton.isDisabled(), "Pay button should be disabled when user is not the winner.");
        assertEquals("Chỉ bidder thắng mới được thanh toán.", feedbackLabel.getText());
    }

    @Test
    public void testRefreshPayState_payButtonDisabled_auctionNotFinished() throws InterruptedException {
        controller.onDataReceived("AUCTION_DETAIL|currentPrice=500.0|status=RUNNING|seller=seller123|itemName=New Item|id=12345"); // Status RUNNING
        controller.onDataReceived("WINNER_INFO|winner=testUser");
        controller.onDataReceived("{\"command\":\"ACCOUNT_INFO\",\"balance\":\"1000.0\"}");
        CountDownLatch waitLatch = new CountDownLatch(1);
        Platform.runLater(waitLatch::countDown);
        assertTrue(waitLatch.await(5, TimeUnit.SECONDS));

        assertTrue(payButton.isDisabled(), "Pay button should be disabled when auction is not FINISHED.");
    }

    @Test
    public void testRefreshPayState_payButtonDisabled_insufficientBalance() throws InterruptedException {
        CountDownLatch latch1 = new CountDownLatch(1);
        controller.onDataReceived("AUCTION_DETAIL|currentPrice=1500.0|status=FINISHED|seller=seller123|itemName=New Item|id=12345"); // Higher price
        controller.onDataReceived("WINNER_INFO|winner=testUser");
        controller.onDataReceived("{\"command\":\"ACCOUNT_INFO\",\"balance\":\"1000.0\"}");
        CountDownLatch waitLatch = new CountDownLatch(1);
        Platform.runLater(waitLatch::countDown);
        assertTrue(waitLatch.await(5, TimeUnit.SECONDS));
        assertTrue(payButton.isDisabled(), "Pay button should be disabled when balance is insufficient.");
        assertEquals("Số dư hiện tại không đủ để thanh toán.", feedbackLabel.getText());
    }

    @Test
    public void testRefreshPayState_paidStatus() throws InterruptedException {
        controller.onDataReceived("AUCTION_DETAIL|currentPrice=500.0|status=PAID|seller=seller123|itemName=New Item|id=12345");
        controller.onDataReceived("WINNER_INFO|winner=testUser");
        controller.onDataReceived("{\"command\":\"ACCOUNT_INFO\",\"balance\":\"1000.0\"}");
        CountDownLatch waitLatch = new CountDownLatch(1);
        Platform.runLater(waitLatch::countDown);
        assertTrue(waitLatch.await(5, TimeUnit.SECONDS), "Timeout waiting for JavaFX thread");
        assertEquals("ĐÃ THANH TOÁN", paymentStatusLabel.getText());
        assertTrue(payButton.isDisabled());
    }

    @Test
    public void testRefreshPayState_canceledStatus() throws InterruptedException {
        controller.onDataReceived("AUCTION_DETAIL|currentPrice=500.0|status=CANCELED|seller=seller123|itemName=New Item|id=12345");
        controller.onDataReceived("WINNER_INFO|winner=testUser");
        controller.onDataReceived("{\"command\":\"ACCOUNT_INFO\",\"balance\":\"1000.0\"}");
        CountDownLatch waitLatch = new CountDownLatch(1);
        Platform.runLater(waitLatch::countDown);
        assertTrue(waitLatch.await(5, TimeUnit.SECONDS), "Timeout waiting for JavaFX thread");
        assertEquals("ĐÃ HỦY", paymentStatusLabel.getText());
        assertTrue(payButton.isDisabled());
    }

    @Test
    public void testParseDouble_validInput() {
        assertEquals(123.45, callParseDouble(controller, "123.45"), 0.001);
    }

    @Test
    public void testParseDouble_invalidInput() {
        assertEquals(0.0, callParseDouble(controller, "abc"));
        assertEquals(0.0, callParseDouble(controller, ""));
        assertEquals(0.0, callParseDouble(controller, null));
    }

    @Test
    public void testFormatMoney() {
        assertEquals("1,000", callFormatMoney(controller, 1000.0));
        assertEquals("1,234,567", callFormatMoney(controller, 1234567.0));
        assertEquals("1,234", callFormatMoney(controller, 1234.5));
        assertEquals("0", callFormatMoney(controller, 0.0));
        assertEquals("-1,000", callFormatMoney(controller, -1000.0));
    }

    @Test
    public void testSetFeedback() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            callSetFeedback(controller, "Test Feedback Message");
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("Test Feedback Message", feedbackLabel.getText());

        CountDownLatch latch2 = new CountDownLatch(1);
        Platform.runLater(() -> {
            callSetFeedback(controller, null);
            latch2.countDown();
        });
        assertTrue(latch2.await(5, TimeUnit.SECONDS));
        assertEquals("", feedbackLabel.getText());
    }

    // Helper methods for accessing private methods via reflection
    private double callParseDouble(PaymentBuyingStuffHandle controllerInstance, String raw) {
        try {
            java.lang.reflect.Method method = PaymentBuyingStuffHandle.class.getDeclaredMethod("parseDouble", String.class);
            method.setAccessible(true);
            return (double) method.invoke(controllerInstance, raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String callFormatMoney(PaymentBuyingStuffHandle controllerInstance, double value) {
        try {
            java.lang.reflect.Method method = PaymentBuyingStuffHandle.class.getDeclaredMethod("formatMoney", double.class);
            method.setAccessible(true);
            return (String) method.invoke(controllerInstance, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void callSetFeedback(PaymentBuyingStuffHandle controllerInstance, String value) {
        try {
            java.lang.reflect.Method method = PaymentBuyingStuffHandle.class.getDeclaredMethod("setFeedback", String.class);
            method.setAccessible(true);
            method.invoke(controllerInstance, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}