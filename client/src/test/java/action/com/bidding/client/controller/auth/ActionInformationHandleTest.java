package action.com.bidding.client.controller.auth;

import action.controller.auth.ActionInformationHandle;
import action.model.StoreDataInput;
import action.network.SocketClient;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class ActionInformationHandleTest {

    private ActionInformationHandle controller;

    // Các thành phần giả lập Server
    private ServerSocket dummyServer;
    private Thread serverThread;
    private Socket clientSocketOnServerSide;
    private final List<String> receivedRequests = new ArrayList<>();
    private PrintWriter serverOut;

    private Label personalID;
    private Label name;
    private Label email;
    private Label phone;
    private Label password;
    private Label money;
    private TextField moneyIn;
    private Label feedbackLabel;
    private Label allStatusLabel;
    private Label pendingStatusLabel;
    private Label openStatusLabel;
    private Label runningStatusLabel;
    private Label finishedStatusLabel;

    @SuppressWarnings("rawtypes") private TableView ItemsTable;
    @SuppressWarnings("rawtypes") private TableColumn sttColumn;
    @SuppressWarnings("rawtypes") private TableColumn productNameColumn;
    @SuppressWarnings("rawtypes") private TableColumn auctionCodeColumn;
    @SuppressWarnings("rawtypes") private TableColumn roleColumn;
    @SuppressWarnings("rawtypes") private TableColumn moneyColumn;
    @SuppressWarnings("rawtypes") private TableColumn statusColumn;
    @SuppressWarnings("rawtypes") private TableColumn resultColumn;
    @SuppressWarnings("rawtypes") private TableColumn timeColumn;
    @SuppressWarnings("rawtypes") private TableColumn actionColumn;
    private VBox auctionHistoryPane;
    private javafx.scene.control.ToggleButton auctionHistoryTab;
    private javafx.scene.control.ToggleButton transactionHistoryTab;
    @SuppressWarnings("rawtypes") private TableView transactionTable;
    @SuppressWarnings("rawtypes") private TableColumn transactionSttColumn;
    @SuppressWarnings("rawtypes") private TableColumn transactionTypeColumn;
    @SuppressWarnings("rawtypes") private TableColumn transactionAmountColumn;
    @SuppressWarnings("rawtypes") private TableColumn transactionDescriptionColumn;
    @SuppressWarnings("rawtypes") private TableColumn transactionStatusColumn;
    @SuppressWarnings("rawtypes") private TableColumn transactionTimeColumn;
    @Start
    public void start(Stage stage) {
        personalID = new Label(); name = new Label(); email = new Label(); phone = new Label();
        password = new Label(); money = new Label(); moneyIn = new TextField();
        feedbackLabel = new Label(); allStatusLabel = new Label(); pendingStatusLabel = new Label();
        openStatusLabel = new Label(); runningStatusLabel = new Label(); finishedStatusLabel = new Label();

        ItemsTable = new TableView(); sttColumn = new TableColumn(); productNameColumn = new TableColumn();
        auctionCodeColumn = new TableColumn(); roleColumn = new TableColumn(); moneyColumn = new TableColumn();
        statusColumn = new TableColumn(); resultColumn = new TableColumn(); timeColumn = new TableColumn();
        actionColumn = new TableColumn();
        auctionHistoryPane = new VBox();
        auctionHistoryTab = new javafx.scene.control.ToggleButton();
        transactionHistoryTab = new javafx.scene.control.ToggleButton();
        transactionTable = new TableView();
        transactionSttColumn = new TableColumn();
        transactionTypeColumn = new TableColumn();
        transactionAmountColumn = new TableColumn();
        transactionDescriptionColumn = new TableColumn();
        transactionStatusColumn = new TableColumn();
        transactionTimeColumn = new TableColumn();

        // --- CẬP NHẬT VBox root ---
        VBox root = new VBox(personalID, name, email, phone, password, money, moneyIn, feedbackLabel,
                allStatusLabel, pendingStatusLabel, openStatusLabel, runningStatusLabel, finishedStatusLabel, ItemsTable,
                auctionHistoryPane, auctionHistoryTab, transactionHistoryTab, transactionTable);
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    @BeforeEach
    void setUp() throws Exception {
        dummyServer = new ServerSocket(0);
        int port = dummyServer.getLocalPort();
        receivedRequests.clear();

        CountDownLatch serverReady = new CountDownLatch(1);
        serverThread = new Thread(() -> {
            try {
                clientSocketOnServerSide = dummyServer.accept();
                serverOut = new PrintWriter(clientSocketOnServerSide.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocketOnServerSide.getInputStream()));
                serverReady.countDown();
                String line;
                while ((line = in.readLine()) != null) {
                    synchronized (receivedRequests) {
                        receivedRequests.add(line);
                    }
                }
            } catch (IOException ignored) {}
        });
        serverThread.start();

        // Connect Client thật tới Dummy Server
        SocketClient.getInstance().connect("localhost", port);
        serverReady.await(2, TimeUnit.SECONDS);

        controller = new ActionInformationHandle();
        StoreDataInput.username = "test_user";

        injectField("personalID", personalID); injectField("name", name); injectField("email", email);
        injectField("phone", phone); injectField("password", password); injectField("money", money);
        injectField("moneyIn", moneyIn); injectField("feedbackLabel", feedbackLabel);
        injectField("allStatusLabel", allStatusLabel); injectField("pendingStatusLabel", pendingStatusLabel);
        injectField("openStatusLabel", openStatusLabel); injectField("runningStatusLabel", runningStatusLabel);
        injectField("finishedStatusLabel", finishedStatusLabel); injectField("ItemsTable", ItemsTable);
        injectField("sttColumn", sttColumn); injectField("productNameColumn", productNameColumn);
        injectField("auctionCodeColumn", auctionCodeColumn); injectField("roleColumn", roleColumn);
        injectField("moneyColumn", moneyColumn); injectField("statusColumn", statusColumn);
        injectField("resultColumn", resultColumn); injectField("timeColumn", timeColumn);
        injectField("actionColumn", actionColumn);
        injectField("auctionHistoryPane", auctionHistoryPane);
        injectField("auctionHistoryTab", auctionHistoryTab);
        injectField("transactionHistoryTab", transactionHistoryTab);
        injectField("transactionTable", transactionTable);
        injectField("transactionSttColumn", transactionSttColumn);
        injectField("transactionTypeColumn", transactionTypeColumn);
        injectField("transactionAmountColumn", transactionAmountColumn);
        injectField("transactionDescriptionColumn", transactionDescriptionColumn);
        injectField("transactionStatusColumn", transactionStatusColumn);
        injectField("transactionTimeColumn", transactionTimeColumn);

        Platform.runLater(() -> controller.initialize(null, null));

        Platform.runLater(() -> controller.initialize(null, null));
        WaitForAsyncUtils.waitForFxEvents();
    }

    @AfterEach
    void tearDown() throws IOException {
        SocketClient.getInstance().removeListener(controller);
        if (clientSocketOnServerSide != null) clientSocketOnServerSide.close();
        if (dummyServer != null) dummyServer.close();
        if (serverThread != null) serverThread.interrupt();
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = ActionInformationHandle.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    @Test
    void testInitializeRequestsData() throws InterruptedException {
        Thread.sleep(1000);
        synchronized (receivedRequests) {
            boolean hasAccountInfo = receivedRequests.stream().anyMatch(r -> r.contains("GET_ACCOUNTINFORMATION"));
            boolean hasListAuctions = receivedRequests.stream().anyMatch(r -> r.contains("LIST_ACCOUNT_AUCTIONS"));
            assertTrue(hasAccountInfo, "Phải gửi yêu cầu GET_ACCOUNTINFORMATION");
            assertTrue(hasListAuctions, "Phải gửi yêu cầu LIST_ACCOUNT_AUCTIONS");
        }
    }

    @Test
    void testOnDataReceivedAccountInfo() throws InterruptedException {
        String jsonInfo = "{\"command\": \"ACCOUNT_INFO\", \"personalID\": \"123456789\", \"username\": \"test_user\", \"email\": \"test@test.com\", \"phone\": \"0987654321\", \"password\": \"pass\", \"balance\": \"1000000\"}";

        controller.onDataReceived(jsonInfo);

        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("123456789", personalID.getText());
        assertEquals("test_user", name.getText());
        assertEquals("test@test.com", email.getText());
        assertEquals("0987654321", phone.getText());
        assertEquals("********", password.getText());
        assertEquals("1,000,000", money.getText());
    }

    @Test
    void testOnDataReceivedAuctions() throws InterruptedException {
        String jsonAuctions = "{\"command\": \"MY_AUCTIONS_RESULT\", \"auctions\": [{\"id\": 1, \"itemName\": \"Item 1\", \"currentPrice\": 50000, \"status\": \"RUNNING\", \"startDate\": \"12/12/2026\", \"startClockTime\": \"10:00:00\", \"role\": \"SELLER\"}]}";

        controller.onDataReceived(jsonAuctions);

        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(1, ItemsTable.getItems().size());
    }
}