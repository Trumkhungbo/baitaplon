package action.com.bidding.client.controller.admin;

import action.controller.admin.AdminAccountPageHandle;
import action.network.SocketClient;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
class AdminAccountPageHandleTest {

    private AdminAccountPageHandle controller;

    private ServerSocket dummyServer;
    private Thread serverThread;
    private Socket clientSocketOnServerSide;
    private final List<String> receivedRequests = new ArrayList<>();

    private Label feedbackLabel;

    @SuppressWarnings("rawtypes") private TableView usersTable;
    @SuppressWarnings("rawtypes") private TableColumn sttColumn;
    @SuppressWarnings("rawtypes") private TableColumn usernameColumn;
    @SuppressWarnings("rawtypes") private TableColumn balanceColumn;
    @SuppressWarnings("rawtypes") private TableColumn emailColumn;
    @SuppressWarnings("rawtypes") private TableColumn idColumn;
    @SuppressWarnings("rawtypes") private TableColumn phoneColumn;
    @SuppressWarnings("rawtypes") private TableColumn personalIdColumn;
    @SuppressWarnings("rawtypes") private TableColumn roleColumn;
    @SuppressWarnings("rawtypes") private TableColumn actionColumn;

    @Start
    public void start(Stage stage) {
        feedbackLabel = new Label();
        usersTable = new TableView(); sttColumn = new TableColumn(); usernameColumn = new TableColumn();
        balanceColumn = new TableColumn(); emailColumn = new TableColumn(); idColumn = new TableColumn();
        phoneColumn = new TableColumn(); personalIdColumn = new TableColumn(); roleColumn = new TableColumn();
        actionColumn = new TableColumn();

        VBox root = new VBox(feedbackLabel, usersTable);
        Scene scene = new Scene(root, 600, 400);
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

        SocketClient.getInstance().connect("localhost", port);
        serverReady.await(2, TimeUnit.SECONDS);

        controller = new AdminAccountPageHandle();

        injectField("feedbackLabel", feedbackLabel); injectField("usersTable", usersTable);
        injectField("sttColumn", sttColumn); injectField("usernameColumn", usernameColumn);
        injectField("balanceColumn", balanceColumn); injectField("emailColumn", emailColumn);
        injectField("idColumn", idColumn); injectField("phoneColumn", phoneColumn);
        injectField("personalIdColumn", personalIdColumn); injectField("roleColumn", roleColumn);
        injectField("actionColumn", actionColumn);

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
        Field field = AdminAccountPageHandle.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    @Test
    void testInitializeLoadsUsers() throws InterruptedException {
        Thread.sleep(200);
        synchronized (receivedRequests) {
            boolean hasListUser = receivedRequests.stream().anyMatch(r -> r.contains("ADMIN_LIST_USERS"));
            assertTrue(hasListUser, "Phải gửi yêu cầu ADMIN_LIST_USERS");
        }
    }

    @Test
    void testOnDataReceivedUsersList() {
        String jsonResponse = "{\"command\": \"ADMIN_USERS\", \"users\": [{\"id\": 1, \"username\": \"user1\", \"balance\": 150000.0, \"email\": \"user1@mail.com\", \"phone\": \"123456789\", \"personalID\": \"CMND123\", \"role\": \"USER\"}]}";

        controller.onDataReceived(jsonResponse);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(1, usersTable.getItems().size());
        assertEquals("Tổng tài khoản: 1", feedbackLabel.getText());
    }

    @Test
    void testOnDataReceivedDeleteSuccess() throws InterruptedException {
        // Clear requests gửi từ initialize
        receivedRequests.clear();
        String jsonResponse = "{\"command\": \"ADMIN_USER_DELETE_RESULT\", \"message\": \"Xóa thành công\"}";

        controller.onDataReceived(jsonResponse);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("Xóa thành công", feedbackLabel.getText());

        Thread.sleep(200);
        synchronized (receivedRequests) {
            boolean hasListUser = receivedRequests.stream().anyMatch(r -> r.contains("ADMIN_LIST_USERS"));
            assertTrue(hasListUser, "Phải gọi lại API lấy danh sách user sau khi xóa");
        }
    }
}