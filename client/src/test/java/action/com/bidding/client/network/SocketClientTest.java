package action.com.bidding.client.network;

import action.network.SocketClient;
import action.network.SocketListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SocketClientTest {

    private ServerSocket dummyServer;
    private int port;
    private Thread serverThread;
    private Socket clientSocketOnServerSide;

    @BeforeEach
    void setUp() throws IOException {
        dummyServer = new ServerSocket(0);
        port = dummyServer.getLocalPort();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (clientSocketOnServerSide != null && !clientSocketOnServerSide.isClosed()) {
            clientSocketOnServerSide.close();
        }
        if (dummyServer != null && !dummyServer.isClosed()) {
            dummyServer.close();
        }
        if (serverThread != null && serverThread.isAlive()) {
            serverThread.interrupt();
        }
    }

    @Test
    void testSingletonInstance() {
        SocketClient instance1 = SocketClient.getInstance();
        SocketClient instance2 = SocketClient.getInstance();
        assertNotNull(instance1, "Instance không được null");
        assertSame(instance1, instance2, "Chỉ được phép có 1 instance duy nhất (Singleton)");
    }

    @Test
    void testConnectAndReceiveData() throws InterruptedException {
        CountDownLatch receiveLatch = new CountDownLatch(1);
        String testMessage = "TEST_SIGNAL_FROM_SERVER";
        String[] receivedData = new String[1];

        serverThread = new Thread(() -> {
            try {
                clientSocketOnServerSide = dummyServer.accept();
                PrintWriter out = new PrintWriter(clientSocketOnServerSide.getOutputStream(), true);
                out.println(testMessage);
            } catch (IOException e) {
            }
        });
        serverThread.start();

        SocketClient client = SocketClient.getInstance();
        SocketListener listener = data -> {
            receivedData[0] = data;
            receiveLatch.countDown();
        };

        client.addListener(listener);
        client.connect("localhost", port);

        boolean await = receiveLatch.await(2, TimeUnit.SECONDS);

        assertTrue(await, "Không nhận được dữ liệu từ Server quá thời gian chờ (2s)");
        assertEquals(testMessage, receivedData[0], "Dữ liệu nhận được không khớp với dữ liệu Server gửi");

        client.removeListener(listener);
    }

    @Test
    void testRequestData() throws InterruptedException {
        CountDownLatch sendLatch = new CountDownLatch(1);
        String clientRequest = "CLIENT_REQUEST_DATA";
        String[] receivedOnServer = new String[1];

        serverThread = new Thread(() -> {
            try {
                clientSocketOnServerSide = dummyServer.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocketOnServerSide.getInputStream()));
                receivedOnServer[0] = in.readLine();
                sendLatch.countDown();
            } catch (IOException e) {
            }
        });
        serverThread.start();

        SocketClient client = SocketClient.getInstance();
        client.connect("localhost", port);

        Thread.sleep(200);

        client.requestData(clientRequest);

        boolean await = sendLatch.await(2, TimeUnit.SECONDS);
        assertTrue(await, "Server ảo chưa nhận được request từ Client");
        assertEquals(clientRequest, receivedOnServer[0], "Dữ liệu Client gửi bị sai lệch");
    }
}