package action.com.bidding.client.network;

import action.network.CentralReceiver;
import action.network.SocketListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CentralReceiverTest {

    private Socket mockSocket;
    private CentralReceiver centralReceiver;

    private List<String> receivedMessages;

    @BeforeEach
    public void setUp() throws Exception {
        receivedMessages = new ArrayList<>();

        SocketListener customListener = new SocketListener() {
            @Override
            public void onDataReceived(String data) {
                receivedMessages.add(data); // Cứ có data thì nhét vào List
            }
        };

        String simulatedServerData = "{\"command\":\"LOGIN_RESULT\",\"status\":\"SUCCESS\"}\n" +
                "{\"command\":\"NEW_MESSAGE\"}\n";
        InputStream is = new ByteArrayInputStream(simulatedServerData.getBytes());

        mockSocket = new Socket() {
            @Override
            public InputStream getInputStream() {
                return is;
            }
        };

        centralReceiver = new CentralReceiver(mockSocket);
        centralReceiver.addListener(customListener);
    }

    @Test
    public void testRunReceivesAndBroadcastsData() throws InterruptedException {
        centralReceiver.start();

        centralReceiver.join(1000);

        assertEquals(2, receivedMessages.size(), "Phải nhận được đúng 2 thông điệp");

        assertTrue(receivedMessages.contains("{\"command\":\"LOGIN_RESULT\",\"status\":\"SUCCESS\"}"));
        assertTrue(receivedMessages.contains("{\"command\":\"NEW_MESSAGE\"}"));
    }
}