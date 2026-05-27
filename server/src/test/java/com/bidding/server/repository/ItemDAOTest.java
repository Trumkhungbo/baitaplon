package com.bidding.server.repository;

import com.bidding.common.enums.ItemType;
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

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ItemDAOTest {

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
    private ItemDAO itemDAO;

    private Art sampleArt;
    private Electronics sampleElectronics;
    private Vehicle sampleVehicle;

    @BeforeEach
    void setUp() throws SQLException {
        // Khởi tạo các loại sản phẩm mẫu
        sampleArt = new Art();
        sampleArt.setName("Mona Lisa Replica");
        sampleArt.setDescription("Fine art print");
        sampleArt.setStartingPrice(5000.0);
        sampleArt.setItemType(ItemType.ART);
        sampleArt.setImageUrl("http://art.com/monalisa.jpg");
        sampleArt.setArtist("Leonardo");
        sampleArt.setCreationYear(1503);
        sampleArt.setCreatedAt(System.currentTimeMillis());

        sampleElectronics = new Electronics();
        sampleElectronics.setName("iPhone 15");
        sampleElectronics.setItemType(ItemType.ELECTRONICS);
        sampleElectronics.setBrand("Apple");
        sampleElectronics.setWarrantyMonths(12);

        sampleVehicle = new Vehicle();
        sampleVehicle.setName("Tesla Model 3");
        sampleVehicle.setItemType(ItemType.VEHICLE);
        sampleVehicle.setEngineType("Electric");
        sampleVehicle.setMileage(10000);

        // Mặc định kết nối giả lập được trả về
        lenient().doReturn(mockConnection).when(itemDAO).getConn();
    }

    // ================== TEST METHOD: SAVE ==================

    @Test
    void testSave_ArtSuccess() throws SQLException {
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong(1)).thenReturn(101L);

        Item result = itemDAO.save(sampleArt, "seller1");

        assertNotNull(result);
        assertEquals(101L, result.getId());
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void testSave_ElectronicsAndVehicleSuccess() throws SQLException {
        // Gom nhóm test các nhánh instanceof còn lại để tăng coverage nhanh
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // Không lấy được key

        assertDoesNotThrow(() -> itemDAO.save(sampleElectronics, "seller1"));
        assertDoesNotThrow(() -> itemDAO.save(sampleVehicle, "seller1"));
    }

    @Test
    void testSave_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> itemDAO.save(null, "seller1"));
    }

    @Test
    void testSave_ThrowsRuntimeExceptionOnSqlException() throws SQLException {
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenThrow(new SQLException("Database disk full"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> itemDAO.save(sampleArt, "seller1"));
        assertTrue(ex.getMessage().contains("Failed to save item"));
    }

    // ================== TEST METHOD: FIND BY ID ==================

    @Test
    void testFindById_SuccessArt() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        stubBaseResultSet(ItemType.ART.name());
        when(mockResultSet.getString("artist")).thenReturn("Da Vinci");
        when(mockResultSet.getInt("creation_year")).thenReturn(1503);

        Item result = itemDAO.findById(1L);

        assertNotNull(result);
        assertTrue(result instanceof Art);
        assertEquals("Da Vinci", ((Art) result).getArtist());
    }

    @Test
    void testFindById_SuccessElectronics() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        stubBaseResultSet(ItemType.ELECTRONICS.name());
        when(mockResultSet.getString("brand")).thenReturn("Apple");
        when(mockResultSet.getInt("warranty_months")).thenReturn(12);

        Item result = itemDAO.findById(2L);

        assertNotNull(result);
        assertTrue(result instanceof Electronics);
        assertEquals("Apple", ((Electronics) result).getBrand());
    }

    @Test
    void testFindById_SuccessVehicle() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        stubBaseResultSet(ItemType.VEHICLE.name());
        when(mockResultSet.getString("engine_type")).thenReturn("V8");
        when(mockResultSet.getInt("mileage")).thenReturn(5000);

        Item result = itemDAO.findById(3L);

        assertNotNull(result);
        assertTrue(result instanceof Vehicle);
        assertEquals("V8", ((Vehicle) result).getEngineType());
    }

    @Test
    void testFindById_NotFound() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        Item result = itemDAO.findById(404L);
        assertNull(result);
    }

    @Test
    void testFindById_ThrowsRuntimeException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Timeout"));

        assertThrows(RuntimeException.class, () -> itemDAO.findById(1L));
    }

    // ================== TEST MAPPING FALLBACK (LUỒNG BIÊN PHỤC HỒI DỮ LIỆU) ==================

    @Test
    void testMap_FallbackToInformationColumns() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        stubBaseResultSet(ItemType.ART.name());
        // Giả lập cột chính bị null hoặc trống, hệ thống phải đọc từ cột information1 và information2
        when(mockResultSet.getString("artist")).thenReturn(null);
        when(mockResultSet.getString("information1")).thenReturn("Fallback Artist");

        when(mockResultSet.getInt("creation_year")).thenReturn(0);
        when(mockResultSet.wasNull()).thenReturn(true); // Đánh dấu giá trị int bị null trong DB
        when(mockResultSet.getString("information2")).thenReturn("1999");

        Item result = itemDAO.findById(1L);

        assertNotNull(result);
        assertEquals("Fallback Artist", ((Art) result).getArtist());
        assertEquals(1999, ((Art) result).getCreationYear());
    }

    @Test
    void testMap_FallbackToDefaultValue() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        stubBaseResultSet(ItemType.ART.name());
        when(mockResultSet.getString("artist")).thenReturn("");
        when(mockResultSet.getString("information1")).thenReturn(" "); // Trống hoàn toàn

        when(mockResultSet.getInt("creation_year")).thenReturn(0);
        when(mockResultSet.wasNull()).thenReturn(true);
        when(mockResultSet.getString("information2")).thenReturn(""); // Không parse được số

        Item result = itemDAO.findById(1L);

        assertNotNull(result);
        assertEquals("Unknown", ((Art) result).getArtist()); // Nhận giá trị mặc định
    }

    @Test
    void testMap_ThrowsExceptionWhenTypeIsNull() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        stubBaseResultSet(null); // Loại item_type là null

        RuntimeException ex = assertThrows(RuntimeException.class, () -> itemDAO.findById(1L));
        assertEquals("Item type is null", ex.getMessage());
    }

    @Test
    void testMap_ThrowsExceptionWhenTypeIsUnknown() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);

        stubBaseResultSet("INVALID_TYPE");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> itemDAO.findById(1L));
        assertTrue(ex.getMessage().contains("Unknown item type"));
    }

    // ================== TEST METHODS: LIST & QUERIES ==================

    @Test
    void testFindAll_Success() throws SQLException {
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);

        stubBaseResultSet(ItemType.ELECTRONICS.name());

        List<Item> list = itemDAO.findAll();
        assertEquals(1, list.size());
    }

    @Test
    void testFindAll_ThrowsException() throws SQLException {
        when(mockConnection.createStatement()).thenThrow(new SQLException("Table crashed"));
        assertThrows(RuntimeException.class, () -> itemDAO.findAll());
    }

    @Test
    void testFindBySeller_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);

        stubBaseResultSet(ItemType.VEHICLE.name());

        List<Item> list = itemDAO.findBySeller("seller1");
        assertEquals(1, list.size());
    }

    @Test
    void testFindBySeller_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Error"));
        assertThrows(RuntimeException.class, () -> itemDAO.findBySeller("seller1"));
    }

    @Test
    void testFindByType_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, false);

        stubBaseResultSet(ItemType.ART.name());

        List<Item> list = itemDAO.findByType(ItemType.ART);
        assertEquals(1, list.size());
    }

    @Test
    void testFindByType_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Error"));
        assertThrows(RuntimeException.class, () -> itemDAO.findByType(ItemType.ART));
    }

    // ================== TEST METHODS: UPDATE & DELETE ==================

    @Test
    void testUpdate_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        assertDoesNotThrow(() -> itemDAO.update(1L, sampleArt, "seller1"));
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void testUpdate_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Deadlock"));
        assertThrows(RuntimeException.class, () -> itemDAO.update(1L, sampleArt, "seller1"));
    }

    @Test
    void testDeleteById_Success() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        assertDoesNotThrow(() -> itemDAO.deleteById(1L));
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    void testDeleteById_ThrowsException() throws SQLException {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("FK Constraint"));
        assertThrows(RuntimeException.class, () -> itemDAO.deleteById(1L));
    }

    // ================== TEST PUBLIC RESOLVE METHODS ==================

    @Test
    void testResolveInformationMethods() {
        assertEquals("Leonardo", itemDAO.resolveInformation1(sampleArt));
        assertEquals("1503", itemDAO.resolveInformation2(sampleArt));

        assertEquals("Apple", itemDAO.resolveInformation1(sampleElectronics));
        assertEquals("12", itemDAO.resolveInformation2(sampleElectronics));

        assertEquals("Electric", itemDAO.resolveInformation1(sampleVehicle));
        assertEquals("10000", itemDAO.resolveInformation2(sampleVehicle));

        Item genericItem = new Item() {@Override
        public String getItemDetails() {
            return "";
        }};
        genericItem.setItemType(ItemType.ART);
        assertNull(itemDAO.resolveInformation1(genericItem));
        assertNull(itemDAO.resolveInformation2(genericItem));
    }
    private void stubBaseResultSet(String typeName) throws SQLException {

        when(mockResultSet.getString("item_type"))
                .thenReturn(typeName);

        if (typeName == null || typeName.equals("INVALID_TYPE")) {
            return;
        }

        // ===== Common =====
        when(mockResultSet.getLong("id"))
                .thenReturn(1L);

        when(mockResultSet.getString("name"))
                .thenReturn("Test Product");

        when(mockResultSet.getString("description"))
                .thenReturn("Test Desc");

        when(mockResultSet.getDouble("starting_price"))
                .thenReturn(100.0);

        when(mockResultSet.getString("image_url"))
                .thenReturn("http://img.com");

        // ===== Generic fallback columns =====
        lenient().when(mockResultSet.getString("information1"))
                .thenReturn("");

        lenient().when(mockResultSet.getString("information2"))
                .thenReturn("");

        // ===== ART =====
        lenient().when(mockResultSet.getString("artist"))
                .thenReturn("Unknown");

        lenient().when(mockResultSet.getInt("creation_year"))
                .thenReturn(2000);

        // ===== ELECTRONICS =====
        lenient().when(mockResultSet.getString("brand"))
                .thenReturn("Generic");

        lenient().when(mockResultSet.getInt("warranty_months"))
                .thenReturn(12);

        // nếu map() có model / condition
        lenient().when(mockResultSet.getString("model"))
                .thenReturn("Default");

        lenient().when(mockResultSet.getString("condition"))
                .thenReturn("NEW");

        // ===== VEHICLE =====
        lenient().when(mockResultSet.getString("engine_type"))
                .thenReturn("Gasoline");

        lenient().when(mockResultSet.getInt("mileage"))
                .thenReturn(1000);

        lenient().when(mockResultSet.wasNull())
                .thenReturn(false);
    }
}