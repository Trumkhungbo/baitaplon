package action.com.bidding.client.model;

import action.model.StoreDataInput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StoreDataInputTest {

    @AfterEach
    void tearDown() {
        // Cần reset lại biến static sau mỗi test để không ảnh hưởng test khác
        StoreDataInput.username = null;
        StoreDataInput.password = null;
    }

    @Test
    void testSetAndGetUsername() {
        String testUser = "admin123";
        StoreDataInput.username = testUser;
        assertEquals(testUser, StoreDataInput.getUsername(), "Username không khớp với dữ liệu đã gán");
    }

    @Test
    void testPasswordAssignment() {
        String testPass = "mySecretPass";
        StoreDataInput.password = testPass;
        assertEquals(testPass, StoreDataInput.password, "Password không khớp với dữ liệu đã gán");
    }

    @Test
    void testInitialValuesAreNull() {
        assertNull(StoreDataInput.getUsername(), "Mặc định username phải là null");
        assertNull(StoreDataInput.password, "Mặc định password phải là null");
    }
}