package com.bidding.server.core;

import com.bidding.common.enums.UserRole;
import com.bidding.common.model.user.Bidder;
import com.bidding.common.model.user.User;
import com.bidding.server.database.DatabaseInitializer;
import com.bidding.server.repository.TopUpRequestDAO;
import com.bidding.server.repository.UserDAO;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private TopUpRequestDAO topUpRequestDAO;

    @InjectMocks
    private AuthService authService;

    private MockedStatic<DatabaseInitializer> mockedDbInit;
    private MockedStatic<PasswordHasher> mockedHasher;

    @BeforeEach
    void setUp() throws Exception {
        // Mock các phương thức static để tránh phụ thuộc vào CSDL thực
        mockedDbInit = mockStatic(DatabaseInitializer.class);
        mockedHasher = mockStatic(PasswordHasher.class);

        // Inject các mock DAO vào AuthService (vì AuthService dùng 'new' thay vì Dependency Injection)
        injectMock(authService, "userDAO", userDAO);
        injectMock(authService, "topUpRequestDAO", topUpRequestDAO);
    }

    @AfterEach
    void tearDown() {
        // Đóng các mock static sau mỗi test
        if (mockedDbInit != null) mockedDbInit.close();
        if (mockedHasher != null) mockedHasher.close();
    }

    // --- CÔNG CỤ HỖ TRỢ INJECT MOCK VÀO FIELD PRIVATE ---
    private void injectMock(Object target, String fieldName, Object mock) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, mock);
    }

    // ================== TEST LOGIN ==================

    @Test
    void testLogin_Success() {
        // Arrange
        String username = "testuser";
        String password = "password123";
        String hash = "hashed_password";

        User mockUser = new Bidder();
        mockUser.setUsername(username);
        mockUser.setPasswordHash(hash);
        mockUser.setRole(UserRole.BIDDER);

        when(userDAO.findByUsername(username)).thenReturn(mockUser);
        mockedHasher.when(() -> PasswordHasher.matches(password, hash)).thenReturn(true);
        mockedHasher.when(() -> PasswordHasher.needsUpgrade(hash)).thenReturn(false);

        // Act
        String result = authService.login(username, password);
        JsonObject jsonResult = JsonParser.parseString(result).getAsJsonObject();

        // Assert
        assertEquals("LOGIN_RESULT", jsonResult.get("command").getAsString());
        assertEquals("SUCCESS", jsonResult.get("status").getAsString());
        assertEquals("Welcome " + username, jsonResult.get("message").getAsString());
        verify(userDAO, never()).updatePasswordHash(anyString(), anyString());
    }

    @Test
    void testLogin_Failure_NullParams() {
        // Act
        String result = authService.login(null, "pass");
        JsonObject jsonResult = JsonParser.parseString(result).getAsJsonObject();

        // Assert
        assertEquals("FAILED", jsonResult.get("status").getAsString());
        assertEquals("Back to refill username or password", jsonResult.get("message").getAsString());
    }

    @Test
    void testLogin_Failure_InvalidPassword() {
        // Arrange
        String username = "testuser";
        String password = "wrongpassword";
        String hash = "hashed_password";

        User mockUser = new Bidder();
        mockUser.setUsername(username);
        mockUser.setPasswordHash(hash);

        when(userDAO.findByUsername(username)).thenReturn(mockUser);
        mockedHasher.when(() -> PasswordHasher.matches(password, hash)).thenReturn(false);

        // Act
        String result = authService.login(username, password);
        JsonObject jsonResult = JsonParser.parseString(result).getAsJsonObject();

        // Assert
        assertEquals("FAILED", jsonResult.get("status").getAsString());
        assertEquals("Invalid username or password", jsonResult.get("message").getAsString());
    }

    // ================== TEST REGISTER ==================

    @Test
    void testRegister_Success() {
        // Arrange
        String username = "newuser";
        String pass = "pass";
        String phone = "123";
        String email = "newuser@local.auction";
        String id = "id123";

        when(userDAO.existsByUsername(username)).thenReturn(false);
        when(userDAO.existsByEmail(email)).thenReturn(false);
        mockedHasher.when(() -> PasswordHasher.hash(pass)).thenReturn("hash");

        // Act
        String result = authService.register(username, pass, phone, email, id);
        JsonObject jsonResult = JsonParser.parseString(result).getAsJsonObject();

        // Assert
        assertEquals("SUCCESS", jsonResult.get("status").getAsString());
        verify(userDAO, times(1)).save(any(Bidder.class));
    }

    @Test
    void testRegister_Failure_UsernameExists() {
        // Arrange
        when(userDAO.existsByUsername("existingUser")).thenReturn(true);

        // Act
        String result = authService.register("existingUser", "pass", null, null, null);
        JsonObject jsonResult = JsonParser.parseString(result).getAsJsonObject();

        // Assert
        assertEquals("FAILED", jsonResult.get("status").getAsString());
        assertEquals("Username already exists", jsonResult.get("message").getAsString());
    }

    // ================== TEST ACCOUNT INFORMATION ==================

    @Test
    void testAccountInformation_Success() {
        // Arrange
        String username = "testuser";
        User mockUser = new Bidder();
        mockUser.setUsername(username);
        mockUser.setEmail("test@email.com");
        mockUser.setPhone("123456");

        when(userDAO.findByUsername(username)).thenReturn(mockUser);
        when(userDAO.getBalanceByUsername(username)).thenReturn(150.50);

        // Act
        String result = authService.accountInformation(username);
        JsonObject jsonResult = JsonParser.parseString(result).getAsJsonObject();

        // Assert
        assertEquals("ACCOUNT_INFO", jsonResult.get("command").getAsString());
        assertEquals("150.50", jsonResult.get("balance").getAsString());
        assertEquals("test@email.com", jsonResult.get("email").getAsString());
    }

    @Test
    void testAccountInformation_NotFound() {
        // Arrange
        when(userDAO.findByUsername("ghost")).thenReturn(null);

        // Act
        String result = authService.accountInformation("ghost");
        JsonObject jsonResult = JsonParser.parseString(result).getAsJsonObject();

        // Assert
        assertEquals("ERROR", jsonResult.get("command").getAsString());
        assertEquals("User not found", jsonResult.get("message").getAsString());
    }

    // ================== TEST ADD MONEY ==================

    @Test
    void testAddMoney_Success() {
        // Arrange
        String username = "testuser";
        String money = "100.0";
        User mockUser = new Bidder();
        mockUser.setUsername(username);

        when(userDAO.findByUsername(username)).thenReturn(mockUser);
        when(topUpRequestDAO.create(username, 100.0)).thenReturn(1L);

        // Act
        String result = authService.addMoney(username, money);
        JsonObject jsonResult = JsonParser.parseString(result).getAsJsonObject();

        // Assert
        assertEquals("TOPUP_REQUEST_CREATED", jsonResult.get("command").getAsString());
        assertEquals(1L, jsonResult.get("requestId").getAsLong());
    }

    @Test
    void testAddMoney_InvalidAmount() {
        // Arrange
        String username = "testuser";
        User mockUser = new Bidder();
        when(userDAO.findByUsername(username)).thenReturn(mockUser);

        // Act
        String result = authService.addMoney(username, "-50.0"); // Tiền âm
        JsonObject jsonResult = JsonParser.parseString(result).getAsJsonObject();

        // Assert
        assertEquals("ERROR", jsonResult.get("command").getAsString());
        assertEquals("Amount must be positive", jsonResult.get("message").getAsString());
    }
}