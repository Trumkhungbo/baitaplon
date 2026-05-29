package action.com.bidding.client.controller.main;

import action.controller.main.DesignerHounerHandle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DesignerHounerHandleTest {

    @Test
    void testControllerInstantiation() {
        DesignerHounerHandle controller = new DesignerHounerHandle();
        assertNotNull(controller, "Controller DesignerHounerHandle phải được khởi tạo thành công");
    }
}
