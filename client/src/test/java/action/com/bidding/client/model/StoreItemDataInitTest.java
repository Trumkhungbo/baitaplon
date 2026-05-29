package action.com.bidding.client.model;

import action.model.StoreItemDataInit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StoreItemDataInitTest {

    @AfterEach
    void tearDown() {
        StoreItemDataInit.name = null;
        StoreItemDataInit.description = null;
        StoreItemDataInit.price = null;
        StoreItemDataInit.status = null;
        StoreItemDataInit.image = null;
        StoreItemDataInit.itemInformation1 = null;
        StoreItemDataInit.itemInformation2 = null;
        StoreItemDataInit.itemType = null;
    }

    @Test
    void testStaticFieldAssignments() {
        StoreItemDataInit.name = "Bình cổ";
        StoreItemDataInit.price = "1500000";
        StoreItemDataInit.status = "AVAILABLE";
        StoreItemDataInit.itemType = "ANTIQUE";

        assertEquals("Bình cổ", StoreItemDataInit.name);
        assertEquals("1500000", StoreItemDataInit.price);
        assertEquals("AVAILABLE", StoreItemDataInit.status);
        assertEquals("ANTIQUE", StoreItemDataInit.itemType);
        assertNull(StoreItemDataInit.description, "Các trường chưa gán phải là null");
    }
}
