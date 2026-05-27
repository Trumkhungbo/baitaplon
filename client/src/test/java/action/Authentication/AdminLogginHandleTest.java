package action.Authentication;

import action.Core.SceneSwitch;
import action.SocketClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.ArrayList;
import java.util.List;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({ApplicationExtension.class})
class AdminLogginHandleTest {

    private AdminLogginHandle controller;

    private StubSceneSwitch sceneSwitchMock;
    private FakeSocketClient fakeSocketClient;

    private DatePicker datePicker;
    private Label label;
    private Button button;

    @BeforeAll
    static void initJfx() {
        // Khởi tạo JFX Toolkit cho các UI components
        Platform.startup(() -> {});
    }

    @BeforeEach
    void setUp() throws Exception {
        controller = new AdminLogginHandle();

        // Khởi tạo các UI Control thật thay vì Mock để DatePicker hoạt động chuẩn
        datePicker = new DatePicker();
        label = new Label();
        button = new Button();

        // Create a lightweight SceneSwitch stub to avoid Mockito inline instrumentation
        sceneSwitchMock = new StubSceneSwitch();

        // Create a fake SocketClient instance to capture outgoing requests
        fakeSocketClient = new FakeSocketClient();

        // Sử dụng reflection để inject UI controls and sceneSwitch stub into controller
        injectField("datePicker", datePicker);
        injectField("label", label);
        injectField("button", button);
        injectField("sceneSwitch", sceneSwitchMock);

        // Chạy initialize
        Platform.runLater(() -> controller.initialize(null, null));
        WaitForAsyncUtils.waitForFxEvents();
    }

    // Named stub so tests can inspect call flags without Mockito bytecode instrumentation
    private static class StubSceneSwitch extends SceneSwitch {
        public boolean lockCalled = false;
        public boolean anyWhereCalled = false;
        public boolean loginCalled = false;
        public ActionEvent lastEvent = null;
        public String lastFXML = null;

        @Override
        public void SwitchToLockPage(ActionEvent event, String FXML) {
            lockCalled = true;
            lastEvent = event;
            lastFXML = FXML;
        }

        @Override
        public void SwitchToAnyWhere(ActionEvent event, String FXML) {
            anyWhereCalled = true;
            lastEvent = event;
            lastFXML = FXML;
        }

        @Override
        public void SwitchToLogin(ActionEvent event) {
            loginCalled = true;
            lastEvent = event;
        }
    }

    // Simple fake SocketClient that records outgoing messages
    private static class FakeSocketClient extends SocketClient {
        public final List<String> messages = new ArrayList<>();

        @Override
        public void requestData(String message) {
            messages.add(message);
        }
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field field = AdminLogginHandle.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(controller, value);
    }

    @Test
    void testSubmittingEmptyDate() throws IOException {
        Platform.runLater(() -> {
            datePicker.setValue(null);
            try {
                controller.Submitting(new ActionEvent());
            } catch (IOException e) {
                fail("Không nên văng lỗi IOException");
            }
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Nếu date trống, phải gọi cảnh báo SomeThingUnFill
        assertTrue(sceneSwitchMock.lockCalled, "Expected SwitchToLockPage to be called");
        assertEquals("/views/SomeThingUnFill.fxml", sceneSwitchMock.lastFXML);
    }

    @Test
    void testSubmittingWrongDate() throws IOException {
        Platform.runLater(() -> {
            datePicker.setValue(LocalDate.of(2025, 1, 1));
            try {
                controller.Submitting(new ActionEvent());
            } catch (IOException e) {
                fail("Không nên văng lỗi IOException");
            }
        });
        WaitForAsyncUtils.waitForFxEvents();

        // Ngày sai thì không switch scene, label báo lỗi
        assertFalse(sceneSwitchMock.lockCalled);
        assertFalse(sceneSwitchMock.anyWhereCalled);
        assertEquals("Sai ngày xác thực. Hãy nhập 30/04/2026.", label.getText());
    }

    @Test
    void testSubmittingCorrectDate() throws Exception {
        // Inject our fake SocketClient into the singleton via reflection
        Field instanceField = SocketClient.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        Object previous = instanceField.get(null);
        instanceField.set(null, fakeSocketClient);

        try {
            Platform.runLater(() -> {
                // Ngày độc lập 30/04/2026 là ngày đúng trong AdminLogginHandle
                datePicker.setValue(LocalDate.of(2026, 4, 30));
                try {
                    controller.Submitting(new ActionEvent());
                } catch (IOException e) {
                    fail("Không nên văng lỗi IOException");
                }
            });
            WaitForAsyncUtils.waitForFxEvents();

            // Sẽ gửi yêu cầu qua socket và đổi cảnh sang Lobby
            assertTrue(fakeSocketClient.messages.stream().anyMatch(s -> s.contains("\"command\":\"ELEVATE\"")));
            // Scene switch should have been called to AdminLobby
            assertTrue(sceneSwitchMock.anyWhereCalled, "Expected SwitchToAnyWhere to be called");
            assertEquals("/views/AdminLobby.fxml", sceneSwitchMock.lastFXML);
        } finally {
            instanceField.set(null, previous);
        }
    }

    @Test
    void testReturnLogin() throws IOException {
        ActionEvent event = new ActionEvent();
        controller.ReturnLogin(event);
        assertTrue(sceneSwitchMock.loginCalled, "Expected SwitchToLogin to be called");
        assertEquals(event, sceneSwitchMock.lastEvent);
    }
}
