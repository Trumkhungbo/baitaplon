package com.bidding.server.repository;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.common.enums.ItemType;
import com.bidding.common.model.Auction;
import com.bidding.common.model.BidTransaction;
import com.bidding.common.model.item.Art;
import com.bidding.common.model.item.Electronics;
import com.bidding.common.model.item.Item;
import com.bidding.common.model.item.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuctionDAOTest {

    @Mock
    private ItemDAO itemDAO;

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private Statement mockStatement;

    @Mock
    private ResultSet mockResultSet;

    // Sử dụng @Spy để có thể can thiệp (stub) vào phương thức getConn() kế thừa từ BaseDAO
    @Spy
    @InjectMocks
    private AuctionDAO auctionDAO;

    private Auction sampleAuction;
    private LocalDateTime now;

    @BeforeEach
    void setUp() throws SQLException {
        now = LocalDateTime.of(2026, 5, 26, 12, 0);

        // Khởi tạo đối tượng dữ liệu mẫu
        Art artItem = new Art();
        artItem.setId(10L);
        artItem.setName("Bức tranh gốm cổ");
        artItem.setItemType(ItemType.ART);
        artItem.setArtist("Nguyễn Huy");
        artItem.setCreationYear(2000);

        sampleAuction = new Auction();
        sampleAuction.setId(1L);
        sampleAuction.setItem(artItem);
        sampleAuction.setSellerUsername("seller1");
        sampleAuction.setStartTime(now);
        sampleAuction.setEndTime(now.plusDays(1));
        sampleAuction.setStatus(AuctionStatus.OPEN);
        sampleAuction.setCurrentHighestBid(1500.0);
        sampleAuction.setHighestBidderUsername("bidder1");

        // Cấu hình mặc định: Mỗi lần gọi auctionDAO.getConn() sẽ trả về mockConnection
        lenient().doReturn(mockConnection).when(auctionDAO).getConn();
    }

    // ================== TEST TRƯỜNG HỢP: CREATE (SAVE) ==================

    @Test
    void testSave_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong(1)).thenReturn(99L); // ID tự tăng trả về từ DB

        Auction savedAuction = auctionDAO.save(sampleAuction);

        assertNotNull(savedAuction);
        assertEquals(99L, savedAuction.getId());
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void testSave_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenThrow(new SQLException("Database disk failure"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> auctionDAO.save(sampleAuction));
        assertTrue(exception.getMessage().contains("Lỗi lưu auction"));
    }

    // ================== TEST TRƯỜNG HỢP: READ (FINDBYID) ==================

    @Test
    void testFindById_Success_Art() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);

        // Giả lập map dữ liệu từ ResultSet sang Object (Loại hàng hóa: ART)
        stubResultSetForMapping(ItemType.ART.name());

        Auction result = auctionDAO.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("seller1", result.getSellerUsername());
        assertTrue(result.getItem() instanceof Art);
    }

    @Test
    void testFindById_Success_Electronics() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);

        // Giả lập map dữ liệu từ ResultSet sang Object (Loại hàng hóa: ELECTRONICS)
        stubResultSetForMapping(ItemType.ELECTRONICS.name());

        Auction result = auctionDAO.findById(1L);

        assertNotNull(result);
        assertTrue(result.getItem() instanceof Electronics);
    }

    @Test
    void testFindById_Success_Vehicle() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);

        // Giả lập map dữ liệu từ ResultSet sang Object (Loại hàng hóa: VEHICLE)
        stubResultSetForMapping(ItemType.VEHICLE.name());

        Auction result = auctionDAO.findById(1L);

        assertNotNull(result);
        assertTrue(result.getItem() instanceof Vehicle);
    }

    @Test
    void testFindById_NotFound() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // DB trống dữ liệu

        Auction result = auctionDAO.findById(404L);
        assertNull(result);
    }

    @Test
    void testFindById_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Connection lost"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> auctionDAO.findById(1L));
        assertTrue(exception.getMessage().contains("Lỗi tìm auction"));
    }

    // ================== TEST TRƯỜNG HỢP: READ ALL & BY STATUS ==================

    @Test
    void testFindAll_Success() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false); // Trả về 1 bản ghi rồi dừng

        stubResultSetForMapping(ItemType.ART.name());

        List<Auction> resultList = auctionDAO.findAll();

        assertNotNull(resultList);
        assertEquals(1, resultList.size());
    }

    @Test
    void testFindAll_ThrowsException() throws SQLException {
        when(mockConnection.createStatement()).thenThrow(new SQLException("Syntax Error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> auctionDAO.findAll());
        assertTrue(exception.getMessage().contains("Lỗi list auctions"));
    }

    @Test
    void testFindByStatus_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);

        stubResultSetForMapping(ItemType.ART.name());

        List<Auction> resultList = auctionDAO.findByStatus(AuctionStatus.OPEN);

        assertNotNull(resultList);
        assertEquals(1, resultList.size());
    }

    @Test
    void testFindByStatus_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Query Timeout"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> auctionDAO.findByStatus(AuctionStatus.FINISHED));
        assertTrue(exception.getMessage().contains("Lỗi tìm auctions by status"));
    }



    @Test
    void testUpdateStatus_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        assertDoesNotThrow(() -> auctionDAO.updateStatus(1L, AuctionStatus.FINISHED));
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void testUpdateStatus_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Lock wait timeout"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> auctionDAO.updateStatus(1L, AuctionStatus.FINISHED));
        assertTrue(exception.getMessage().contains("Lỗi cập nhật status"));
    }

    // ================== TEST TRƯỜNG HỢP: PLACE BID (ATOMIC TRANSACTION) ==================

    @Test
    void testPlaceBid_CommitSuccess() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong(1)).thenReturn(555L); // ID của giao dịch cược mới sinh

        BidTransaction transactionInput = new BidTransaction();
        transactionInput.setAuctionId(1L);
        transactionInput.setBidderUsername("user_win");
        transactionInput.setBidAmount(2000.0);
        transactionInput.setBidTime(now);

        BidTransaction result = auctionDAO.placeBid(1L, 2000.0, "user_win", transactionInput);

        assertNotNull(result);
        assertEquals(555L, result.getId());
        verify(mockConnection, times(1)).setAutoCommit(false);
        verify(mockConnection, times(1)).commit();
    }

    @Test
    void testPlaceBid_RollbackOnFailure() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        // Cố ý gây lỗi ở câu lệnh chèn giao dịch (Bước 2) để kích hoạt rollback
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenThrow(new SQLException("Constraint violation"));

        BidTransaction transactionInput = new BidTransaction();
        transactionInput.setAuctionId(1L);
        transactionInput.setBidderUsername("user_fail");
        transactionInput.setBidAmount(2000.0);
        transactionInput.setBidTime(now);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                auctionDAO.placeBid(1L, 2000.0, "user_fail", transactionInput)
        );

        assertTrue(exception.getMessage().contains("Lỗi đặt bid (đã rollback)"));
        verify(mockConnection, times(1)).rollback(); // Xác nhận lệnh rollback được gọi thành công
    }

    @Test
    void testPlaceBid_ConnectionError() throws SQLException {
        // Gây lỗi ngay từ khi mở/thiết lập connection
        doThrow(new SQLException("Network partition")).when(mockConnection).setAutoCommit(false);

        BidTransaction transactionInput = new BidTransaction();

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                auctionDAO.placeBid(1L, 2000.0, "user_fail", transactionInput)
        );

        assertTrue(exception.getMessage().contains("Lỗi kết nối khi đặt bid"));
    }

    // ================== TEST TRƯỜNG HỢP: FIND BID HISTORY ==================

    @Test
    void testFindBidHistory_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);

        // Thiết lập dữ liệu mock cho bảng bid_transactions
        when(mockResultSet.getLong("id")).thenReturn(101L);
        when(mockResultSet.getLong("auction_id")).thenReturn(1L);
        when(mockResultSet.getString("bidder_username")).thenReturn("bidder_one");
        when(mockResultSet.getDouble("bid_amount")).thenReturn(1700.0);
        when(mockResultSet.getTimestamp("bid_time")).thenReturn(Timestamp.valueOf(now));

        List<BidTransaction> result = auctionDAO.findBidHistory(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("bidder_one", result.get(0).getBidderUsername());
    }

    @Test
    void testFindBidHistory_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Table not found"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> auctionDAO.findBidHistory(1L));
        assertTrue(exception.getMessage().contains("Lỗi lấy bid history"));
    }

    // ================== HÀM PHỤ TRỢ (HELPER METHOD) GIẢ LẬP RESULTSET MAPPING ==================

    private void stubResultSetForMapping(String itemTypeStr) throws SQLException {
        when(mockResultSet.getLong("id")).thenReturn(1L);
        when(mockResultSet.getString("seller_username")).thenReturn("seller1");
        when(mockResultSet.getTimestamp("start_time")).thenReturn(Timestamp.valueOf(now));
        when(mockResultSet.getTimestamp("end_time")).thenReturn(Timestamp.valueOf(now.plusDays(1)));
        when(mockResultSet.getString("status")).thenReturn("OPEN");
        when(mockResultSet.getDouble("current_highest_bid")).thenReturn(1500.0);
        when(mockResultSet.getString("highest_bidder_username")).thenReturn("bidder1");

        when(mockResultSet.getString("i_item_type")).thenReturn(itemTypeStr);
        when(mockResultSet.getLong("i_id")).thenReturn(10L);
        when(mockResultSet.getString("i_name")).thenReturn("Mẫu vật kiểm thử");
        when(mockResultSet.getDouble("i_starting_price")).thenReturn(1000.0);
        when(mockResultSet.getString("i_image_url")).thenReturn("http://image.com/test.png");

        // Giả lập dữ liệu cho các trường thuộc lớp con đa hình tùy theo itemType
        if (ItemType.ART.name().equals(itemTypeStr)) {
            when(mockResultSet.getString("i_artist")).thenReturn("Họa sĩ ẩn danh");
            when(mockResultSet.getInt("i_creation_year")).thenReturn(1995);
        } else if (ItemType.ELECTRONICS.name().equals(itemTypeStr)) {
            when(mockResultSet.getString("i_brand")).thenReturn("Sony");
            when(mockResultSet.getInt("i_warranty_months")).thenReturn(24);
        } else if (ItemType.VEHICLE.name().equals(itemTypeStr)) {
            when(mockResultSet.getString("i_engine_type")).thenReturn("V8");
            when(mockResultSet.getInt("i_mileage")).thenReturn(15000);
        }
    }
}