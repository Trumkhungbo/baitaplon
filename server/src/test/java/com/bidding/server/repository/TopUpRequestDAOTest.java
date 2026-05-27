package com.bidding.server.repository;

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
public class TopUpRequestDAOTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    @Spy
    @InjectMocks
    private TopUpRequestDAO topUpRequestDAO;

    @BeforeEach
    void setUp() throws SQLException {
        // Ép phương thức getConn() kế thừa từ BaseDAO luôn trả về Connection giả lập
        lenient().doReturn(mockConnection).when(topUpRequestDAO).getConn();
    }

    // ================== TEST METHOD: CREATE ==================

    @Test
    void testCreate_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong(1)).thenReturn(456L); // Giả lập sinh mã ID tự tăng

        long generatedId = topUpRequestDAO.create("user_nap_tien", 200000.0);

        assertEquals(456L, generatedId);
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void testCreate_NoGeneratedKey() throws SQLException {
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // Không lấy được key tự tăng

        long generatedId = topUpRequestDAO.create("user_nap_tien", 50000.0);

        assertEquals(0L, generatedId);
    }

    @Test
    void testCreate_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenThrow(new SQLException("Table topup_requests does not exist"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                topUpRequestDAO.create("user_nap_tien", 100000.0)
        );
        assertTrue(ex.getMessage().contains("Loi tao yeu cau nap tien"));
    }

    // ================== TEST METHOD: FIND PENDING ==================

    @Test
    void testFindPending_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        // Giả lập ResultSet trả về 1 dòng bản ghi rồi kết thúc vòng lặp while
        when(mockResultSet.next()).thenReturn(true, false);
        stubResultSetForTopUpRequest();

        List<TopUpRequestDAO.TopUpRequest> list = topUpRequestDAO.findPending();

        assertNotNull(list);
        assertEquals(1, list.size());
        TopUpRequestDAO.TopUpRequest req = list.get(0);
        assertEquals(123L, req.id());
        assertEquals("user_nap_tien", req.username());
        assertEquals(500000.0, req.currentBalance());
        assertEquals(100000.0, req.amount());
        assertEquals("user@test.com", req.email());
        assertEquals("0987654321", req.phone());
        assertEquals("123456789", req.personalId());
        assertEquals(11111111L, req.requestedAt());
    }

    @Test
    void testFindPending_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Syntax error in JOIN"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> topUpRequestDAO.findPending());
        assertTrue(ex.getMessage().contains("Loi lay yeu cau nap tien"));
    }

    // ================== TEST METHOD: FIND PENDING BY ID ==================

    @Test
    void testFindPendingById_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        stubResultSetForTopUpRequest();

        TopUpRequestDAO.TopUpRequest result = topUpRequestDAO.findPendingById(123L);

        assertNotNull(result);
        assertEquals(123L, result.id());
        assertEquals("user_nap_tien", result.username());
    }

    @Test
    void testFindPendingById_NotFound() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // Không tìm thấy bản ghi trùng ID

        TopUpRequestDAO.TopUpRequest result = topUpRequestDAO.findPendingById(999L);

        assertNull(result);
    }

    @Test
    void testFindPendingById_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Connection closed unexpectedly"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> topUpRequestDAO.findPendingById(123L));
        assertTrue(ex.getMessage().contains("Loi tim yeu cau nap tien"));
    }

    // ================== TEST METHOD: MARK APPROVED ==================

    @Test
    void testMarkApproved_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1); // Cập nhật thành công 1 dòng

        boolean status = topUpRequestDAO.markApproved(123L);

        assertTrue(status);
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void testMarkApproved_NoRowUpdated() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0); // Không có bản ghi nào khớp (Ví dụ: ID sai hoặc không PENDING)

        boolean status = topUpRequestDAO.markApproved(999L);

        assertFalse(status);
    }

    @Test
    void testMarkApproved_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Database lock timeout"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> topUpRequestDAO.markApproved(123L));
        assertTrue(ex.getMessage().contains("Loi duyet yeu cau nap tien"));
    }



    private void stubResultSetForTopUpRequest() throws SQLException {
        when(mockResultSet.getLong("id")).thenReturn(123L);
        when(mockResultSet.getString("username")).thenReturn("user_nap_tien");
        when(mockResultSet.getDouble("balance")).thenReturn(500000.0);
        when(mockResultSet.getDouble("amount")).thenReturn(100000.0);
        when(mockResultSet.getString("email")).thenReturn("user@test.com");
        when(mockResultSet.getString("phone")).thenReturn("0987654321");
        when(mockResultSet.getString("personal_id")).thenReturn("123456789");
        when(mockResultSet.getLong("requested_at")).thenReturn(11111111L);
    }
}