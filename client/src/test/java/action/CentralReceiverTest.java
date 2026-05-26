package action;

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

    // Tạo 1 list để hứng dữ liệu thay cho Mockito
    private List<String> receivedMessages;

    @BeforeEach
    public void setUp() throws Exception {
        receivedMessages = new ArrayList<>();

        // 1. Tạo 1 Listener "bằng tay" (Stub) thay vì dùng Mockito
        SocketListener customListener = new SocketListener() {
            @Override
            public void onDataReceived(String data) {
                receivedMessages.add(data); // Cứ có data thì nhét vào List
            }
        };

        // 2. Giả lập luồng dữ liệu mà Server sẽ gửi về (2 dòng lệnh JSON)
        String simulatedServerData = "{\"command\":\"LOGIN_RESULT\",\"status\":\"SUCCESS\"}\n" +
                "{\"command\":\"NEW_MESSAGE\"}\n";
        InputStream is = new ByteArrayInputStream(simulatedServerData.getBytes());

        // 3. TẠO FAKE SOCKET (Lớp ẩn danh)
        mockSocket = new Socket() {
            @Override
            public InputStream getInputStream() {
                return is;
            }
        };

        // 4. Khởi tạo đối tượng cần test
        centralReceiver = new CentralReceiver(mockSocket);
        centralReceiver.addListener(customListener);
    }

    @Test
    public void testRunReceivesAndBroadcastsData() throws InterruptedException {
        // Chạy Thread Receiver
        centralReceiver.start();

        // Đợi Thread đọc xong dữ liệu (tối đa 1 giây)
        centralReceiver.join(1000);

        // Kiểm tra xem List có hứng được đúng 2 message không
        assertEquals(2, receivedMessages.size(), "Phải nhận được đúng 2 thông điệp");

        // Kiểm tra nội dung từng message
        assertTrue(receivedMessages.contains("{\"command\":\"LOGIN_RESULT\",\"status\":\"SUCCESS\"}"));
        assertTrue(receivedMessages.contains("{\"command\":\"NEW_MESSAGE\"}"));
    }
}