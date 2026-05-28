package action.Authentication;

import action.Core.SceneSwitch;
import action.SocketClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({ApplicationExtension.class})
class AdminLogginHandleTest {

    private AdminLogginHandle controller;

    private StubSceneSwitch sceneSwitchMock;
    private FakeSocketClient fakeSocketClient;

    private TextField usernameField;
    private PasswordField passwordField;
    private Label label;
    private Button button;

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Toolkit đã được khởi tạo trước đó. Bỏ qua.
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        controller = new AdminLogginHandle();

        usernameField = new TextField();
        passwordField = new PasswordField();
        label = new Label();
        button = new Button();

        sceneSwitchMock = new StubSceneSwitch();
        fakeSocketClient = new FakeSocketClient();

        injectField("usernameField", usernameField);
        injectField("passwordField", passwordField);
        injectField("label", label);
        injectField("button", button);
        injectField("sceneSwitch", sceneSwitchMock);

        Platform.runLater(() -> controller.initialize(null, null));
        WaitForAsyncUtils.waitForFxEvents();
    }

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
    void testSubmittingEmptyCredentials() throws IOException {
        Platform.runLater(() -> {
            usernameField.setText("");
            passwordField.setText("");
            try {
                controller.Submitting(new ActionEvent());
            } catch (IOException e) {
                fail("Không nên văng lỗi IOException");
            }
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(sceneSwitchMock.lockCalled, "Expected SwitchToLockPage to be called");
        assertEquals("/views/SomeThingUnFill.fxml", sceneSwitchMock.lastFXML);
    }

    @Test
    void testSubmittingCredentials() throws Exception {
        Field instanceField = SocketClient.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        Object previous = instanceField.get(null);
        instanceField.set(null, fakeSocketClient);

        try {
            Platform.runLater(() -> {
                usernameField.setText("admin");
                passwordField.setText("password");
                try {
                    controller.Submitting(new ActionEvent());
                } catch (IOException e) {
                    fail("Không nên văng lỗi IOException");
                }
            });
            WaitForAsyncUtils.waitForFxEvents();

            assertTrue(fakeSocketClient.messages.stream().anyMatch(s -> s.contains("\"command\":\"LOGIN\"")));
            assertEquals("Đang đăng nhập admin...", label.getText());
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
