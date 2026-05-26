package com.bidding.server.repository;

import com.bidding.common.enums.UserRole;
import com.bidding.common.model.user.Admin;
import com.bidding.common.model.user.Bidder;
import com.bidding.common.model.user.Seller;
import com.bidding.common.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserDAOTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private Statement mockStatement;

    @Mock
    private ResultSet mockResultSet;

    @Spy
    @InjectMocks
    private UserDAO userDAO;

    private Bidder sampleBidder;
    private Seller sampleSeller;
    private Admin sampleAdmin;

    @BeforeEach
    void setUp() throws SQLException {
        // Khởi tạo các loại đối tượng User kế thừa khác nhau
        sampleBidder = new Bidder();
        sampleBidder.setUsername("bidder_test");
        sampleBidder.setPasswordHash("hash123");
        sampleBidder.setEmail("bidder@test.com");
        sampleBidder.setPhone("0912345678");
        sampleBidder.setPersonalId("123456789");
        sampleBidder.setRole(UserRole.BIDDER);
        sampleBidder.setBalance(500.0);
        sampleBidder.setCreatedAt(System.currentTimeMillis());

        sampleSeller = new Seller();
        sampleSeller.setUsername("seller_test");
        sampleSeller.setRole(UserRole.SELLER);

        sampleAdmin = new Admin();
        sampleAdmin.setUsername("admin_test");
        sampleAdmin.setRole(UserRole.ADMIN);

        // Ép BaseDAO luôn trả về Connection giả lập
        lenient().doReturn(mockConnection).when(userDAO).getConn();
    }

    // ================== TEST METHOD: SAVE ==================

    @Test
    void testSave_BidderSuccess() throws SQLException {
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong(1)).thenReturn(10L);

        User result = userDAO.save(sampleBidder);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testSave_SellerAndAdminSuccess() throws SQLException {
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        // Kiểm thử nhánh rẽ switch-case sinh chuỗi "SELLER" và "ADMIN"
        assertDoesNotThrow(() -> userDAO.save(sampleSeller));
        assertDoesNotThrow(() -> userDAO.save(sampleAdmin));
    }

    @Test
    void testSave_ThrowsExceptionOnSqlError() throws SQLException {
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenThrow(new SQLException("Duplicate entry"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userDAO.save(sampleBidder));
        assertTrue(ex.getMessage().contains("Loi luu user"));
    }

    // ================== TEST METHOD: FIND BY USERNAME ==================

    @Test
    void testFindByUsername_SuccessBidder() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        stubBaseUserResultSet("USER"); // Mặc định khớp role USER -> Bidder
        when(mockResultSet.getDouble("balance")).thenReturn(1000.0);

        User result = userDAO.findByUsername("bidder_test");

        assertNotNull(result);
        assertTrue(result instanceof Bidder);
        assertEquals(1000.0, ((Bidder) result).getBalance());
    }

    @Test
    void testFindByUsername_SuccessSeller() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        stubBaseUserResultSet("SELLER");

        User result = userDAO.findByUsername("seller_test");

        assertNotNull(result);
        assertTrue(result instanceof Seller);
    }

    @Test
    void testFindByUsername_SuccessAdmin() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        stubBaseUserResultSet("ADMIN");

        User result = userDAO.findByUsername("admin_test");

        assertNotNull(result);
        assertTrue(result instanceof Admin);
    }

    @Test
    void testFindByUsername_NotFound() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        User result = userDAO.findByUsername("ghost");
        assertNull(result);
    }

    @Test
    void testFindByUsername_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Connection failure"));

        assertThrows(RuntimeException.class, () -> userDAO.findByUsername("test"));
    }

    // ================== TEST METHOD: FIND BY ID ==================

    @Test
    void testFindById_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        stubBaseUserResultSet("USER");

        User result = userDAO.findById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testFindById_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Error"));
        assertThrows(RuntimeException.class, () -> userDAO.findById(1L));
    }

    // ================== TEST METHOD: FIND ALL ==================

    @Test
    void testFindAll_Success() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);

        stubBaseUserResultSet("USER");

        List<User> list = userDAO.findAll();
        assertEquals(1, list.size());
    }

    @Test
    void testFindAll_ThrowsException() throws SQLException {
        when(mockConnection.createStatement()).thenThrow(new SQLException("Table locked"));
        assertThrows(RuntimeException.class, () -> userDAO.findAll());
    }

    // ================== TEST METHODS: UPDATE BALANCE & GET BALANCE ==================

    @Test
    void testUpdateBalance_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        assertDoesNotThrow(() -> userDAO.updateBalance("bidder_test", 750.0));
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testUpdateBalance_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Constraint error"));
        assertThrows(RuntimeException.class, () -> userDAO.updateBalance("test", 10.0));
    }

    @Test
    void testGetBalanceByUsername_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getDouble("balance")).thenReturn(999.5);

        double balance = userDAO.getBalanceByUsername("bidder_test");
        assertEquals(999.5, balance);
    }

    @Test
    void testGetBalanceByUsername_NotFound() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        double balance = userDAO.getBalanceByUsername("ghost");
        assertEquals(0.0, balance);
    }

    @Test
    void testGetBalanceByUsername_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB crash"));
        assertThrows(RuntimeException.class, () -> userDAO.getBalanceByUsername("test"));
    }

    // ================== TEST METHOD: UPDATE PASSWORD HASH ==================

    @Test
    void testUpdatePasswordHash_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        assertDoesNotThrow(() -> userDAO.updatePasswordHash("bidder_test", "new_hash"));
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testUpdatePasswordHash_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("ReadOnly mode"));
        assertThrows(RuntimeException.class, () -> userDAO.updatePasswordHash("test", "hash"));
    }

    // ================== TEST METHOD: DELETE ==================

    @Test
    void testDelete_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1); // Xóa thành công 1 dòng

        boolean deleted = userDAO.delete(1L);
        assertTrue(deleted);
    }

    @Test
    void testDelete_Failed() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0); // Không có dòng nào bị xóa

        boolean deleted = userDAO.delete(404L);
        assertFalse(deleted);
    }

    @Test
    void testDelete_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("FK violation"));
        assertThrows(RuntimeException.class, () -> userDAO.delete(1L));
    }

    // ================== TEST METHODS: EXISTS BY USERNAME / EMAIL ==================

    @Test
    void testExistsByUsername_True() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        assertTrue(userDAO.existsByUsername("admin"));
    }

    @Test
    void testExistsByUsername_FalseAndCatchException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        assertFalse(userDAO.existsByUsername("ghost"));

        // Ép ra lỗi SQLException để kiểm thử khối catch trả về false ẩn bên trong
        reset(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Query failed"));
        assertFalse(userDAO.existsByUsername("error_user"));
    }

    @Test
    void testExistsByEmail_True() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        assertTrue(userDAO.existsByEmail("test@test.com"));
    }

    @Test
    void testExistsByEmail_FalseAndCatchException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        assertFalse(userDAO.existsByEmail("ghost@test.com"));

        // Ép ra lỗi SQLException để kiểm thử khối catch trả về false ẩn bên trong
        reset(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Query failed"));
        assertFalse(userDAO.existsByEmail("error@test.com"));
    }


    private void stubBaseUserResultSet(String roleStr) throws SQLException {
        when(mockResultSet.getLong("id")).thenReturn(1L);
        when(mockResultSet.getString("username")).thenReturn("test_user");
        when(mockResultSet.getString("password_hash")).thenReturn("hash");
        when(mockResultSet.getString("email")).thenReturn("user@bidding.com");
        when(mockResultSet.getString("phone")).thenReturn("0123");
        when(mockResultSet.getString("personal_id")).thenReturn("9999");
        when(mockResultSet.getString("role")).thenReturn(roleStr);
        when(mockResultSet.getLong("created_at")).thenReturn(111111L);
    }
}