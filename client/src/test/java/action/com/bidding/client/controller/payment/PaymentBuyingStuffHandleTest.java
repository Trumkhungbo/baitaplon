package action.com.bidding.client.controller.payment;

import action.controller.payment.PaymentBuyingStuffHandle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class PaymentBuyingStuffHandleTest {

    private PaymentBuyingStuffHandle controller;

    @Start
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/Payment_BuyingStuff.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void testPaymentLabelsAndButtonsExist(FxRobot robot) {
        assertTrue(robot.lookup("#itemNameLabel").tryQuery().isPresent(), "Item name label must exist");
        assertTrue(robot.lookup("#priceLabel").tryQuery().isPresent(), "Price label must exist");
        assertTrue(robot.lookup("THANH TOÁN NGAY").tryQuery().isPresent(), "Pay button text should be present");
    }
}
