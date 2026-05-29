package action.com.bidding.client.controller.SellingJob; // Sửa lại package nếu bạn đã đổi tên

import action.model.StoreDataInput;
import action.controller.SellingJobs.StoreSellerProductEdit;
import action.network.SocketClient;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(ApplicationExtension.class)
public class InvesterSellHandleTest {

    private static MockedStatic<SocketClient> mockedSocket;
    private static SocketClient mockClient;

    @BeforeAll
    static void setupClass() {
        mockClient = mock(SocketClient.class);
        mockedSocket = Mockito.mockStatic(SocketClient.class);
        mockedSocket.when(SocketClient::getInstance).thenReturn(mockClient);
    }

    @AfterAll
    static void tearDownClass() {
        if (mockedSocket != null) {
            mockedSocket.close();
        }
    }

    @BeforeEach
    void resetData() {
        StoreSellerProductEdit.editing = false;
        StoreDataInput.username = "test_user_123";
    }

    @Start
    private void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/InvesterSell.fxml"));
        Parent root = loader.load();
        stage.setScene(new Scene(root));
        stage.show();
    }


    @Test
    void test1_EmptyFields_ShouldShowError(FxRobot robot) {

        robot.clickOn("#submitButton"); // Giả định id nút submit là submitButton

        Label statusLabel = robot.lookup("#statusLabel").queryAs(Label.class);
        assertEquals("Dữ liệu không hợp lệ, vui lòng kiểm tra lại.", statusLabel.getText(),
                "Phải hiện thông báo lỗi khi không điền thông tin");
    }

    @Test
    void test2_InvalidPriceFormat_ShouldShowError(FxRobot robot) {

        robot.clickOn("#itemname").write("Bình hoa cổ");
        robot.clickOn("#TimeStart").write("14:30");
        robot.clickOn("#duration").write("120");

        robot.clickOn("#price").write("Một triệu đồng");

        robot.clickOn("#submitButton");

        Label statusLabel = robot.lookup("#statusLabel").queryAs(Label.class);
        assertEquals("Dữ liệu không hợp lệ, vui lòng kiểm tra lại.", statusLabel.getText(),
                "Phải báo lỗi khi giá tiền không phải là số");
    }

    @Test
    void test3_InvalidTimeFormat_ShouldShowError(FxRobot robot) {

        robot.clickOn("#itemname").write("Bức tranh Picasso");
        robot.clickOn("#price").write("5000000"); // Giá hợp lệ
        robot.clickOn("#duration").write("60");

        robot.clickOn("#TimeStart").write("abc:xyz");

        robot.clickOn("#submitButton");

        Label statusLabel = robot.lookup("#statusLabel").queryAs(Label.class);
        assertEquals("Dữ liệu không hợp lệ, vui lòng kiểm tra lại.", statusLabel.getText(),
                "Phải báo lỗi khi giờ bắt đầu nhập sai định dạng HH:mm");
    }

    @Test
    void test4_ValidData_ShouldAttemptToSubmit(FxRobot robot) {

        robot.clickOn("#itemname").write("Vòng cổ kim cương");
        robot.clickOn("#price").write("1000000"); // 1,000,000
        robot.clickOn("#TimeStart").write("09:00");
        robot.clickOn("#duration").write("120"); // 120 phút

        robot.clickOn("#submitButton");

        Label statusLabel = robot.lookup("#statusLabel").queryAs(Label.class);
        assertEquals("Đang đăng bán...", statusLabel.getText(),
                "Label phải báo Đang đăng bán khi toàn bộ dữ liệu hợp lệ");

    }}
