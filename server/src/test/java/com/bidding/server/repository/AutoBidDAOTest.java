package com.bidding.server.repository;

import com.bidding.common.model.AutoBid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AutoBidDAOTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    @Spy
    @InjectMocks
    private AutoBidDAO autoBidDAO;

    private AutoBid sampleAutoBid;

    @BeforeEach
    void setUp() throws SQLException {
        // Khởi tạo đối tượng AutoBid mẫu phục vụ cho việc kiểm thử
        sampleAutoBid = new AutoBid();
        sampleAutoBid.setAuctionId(100L);
        sampleAutoBid.setBidderUsername("buyer_pro");
        sampleAutoBid.setMaxBid(5000.0);
        sampleAutoBid.setIncrement(100.0);
        sampleAutoBid.setActive(true);

        // Ràng buộc phương thức getConn() luôn trả về kết nối giả lập mockConnection
        lenient().doReturn(mockConnection).when(autoBidDAO).getConn();
    }

    // ================== TEST METHOD: UPSERT ==================

    @Test
    void testUpsert_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        assertDoesNotThrow(() -> autoBidDAO.upsert(sampleAutoBid));

        // Xác thực các tham số truyền vào PreparedStatement đúng vị trí câu lệnh SQL
        verify(mockPreparedStatement, times(1)).setLong(1, 100L);
        verify(mockPreparedStatement, times(1)).setString(2, "buyer_pro");
        verify(mockPreparedStatement, times(1)).setDouble(3, 5000.0);
        verify(mockPreparedStatement, times(1)).setDouble(4, 100.0);
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void testUpsert_ThrowsRuntimeException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Table locked by another transaction"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> autoBidDAO.upsert(sampleAutoBid));
        assertTrue(ex.getMessage().contains("Lỗi upsert auto bid"));
    }

    // ================== TEST METHOD: DISABLE ==================

    @Test
    void testDisable_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1); // Cập nhật thành công 1 bản ghi

        boolean status = autoBidDAO.disable(100L, "buyer_pro");

        assertTrue(status);
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void testDisable_NoRowUpdated() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0); // Không tìm thấy bản ghi nào phù hợp

        boolean status = autoBidDAO.disable(999L, "unknown_user");

        assertFalse(status);
    }

    @Test
    void testDisable_ThrowsRuntimeException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Connection timed out"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> autoBidDAO.disable(100L, "buyer_pro"));
        assertTrue(ex.getMessage().contains("Lỗi disable auto bid"));
    }

    // ================== TEST METHOD: FIND ACTIVE BY AUCTION ==================

    @Test
    void testFindActiveByAuction_Success_ActiveAndInactive() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        // Bản ghi 1: Trạng thái active = 1 (True). Bản ghi 2: Trạng thái active = 0 (False)
        when(mockResultSet.next()).thenReturn(true, true, false);

        when(mockResultSet.getLong("id")).thenReturn(1L, 2L);
        when(mockResultSet.getLong("auction_id")).thenReturn(100L, 100L);
        when(mockResultSet.getString("bidder_username")).thenReturn("buyer1", "buyer2");
        when(mockResultSet.getDouble("max_bid")).thenReturn(3000.0, 4000.0);
        when(mockResultSet.getDouble("increment")).thenReturn(50.0, 100.0);
        when(mockResultSet.getInt("is_active")).thenReturn(1, 0); // Trả về 1 và 0 để quét qua cả 2 nhánh (==1 và !=1)

        List<AutoBid> results = autoBidDAO.findActiveByAuction(100L);

        assertNotNull(results);
        assertEquals(2, results.size());

        assertTrue(results.get(0).isActive());  // Nhánh is_active == 1
        assertFalse(results.get(1).isActive()); // Nhánh is_active != 1
    }

    @Test
    void testFindActiveByAuction_ThrowsRuntimeException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Syntax error near WHERE"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> autoBidDAO.findActiveByAuction(100L));
        assertTrue(ex.getMessage().contains("Lỗi lấy auto bids"));
    }

    // ================== TEST METHOD: FIND ONE ==================

    @Test
    void testFindOne_Found() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true); // Tìm thấy bản ghi

        when(mockResultSet.getLong("id")).thenReturn(10L);
        when(mockResultSet.getLong("auction_id")).thenReturn(100L);
        when(mockResultSet.getString("bidder_username")).thenReturn("buyer_pro");
        when(mockResultSet.getDouble("max_bid")).thenReturn(5000.0);
        when(mockResultSet.getDouble("increment")).thenReturn(100.0);
        when(mockResultSet.getInt("is_active")).thenReturn(1);

        AutoBid result = autoBidDAO.findOne(100L, "buyer_pro");

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("buyer_pro", result.getBidderUsername());
        assertTrue(result.isActive());
    }

    @Test
    void testFindOne_NotFound() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // Không tìm thấy bản ghi phù hợp

        AutoBid result = autoBidDAO.findOne(999L, "nobody");

        assertNull(result);
    }

    @Test
    void testFindOne_ThrowsRuntimeException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Database disk failure"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> autoBidDAO.findOne(100L, "buyer_pro"));
        assertTrue(ex.getMessage().contains("Lỗi tìm auto bid"));
    }
}