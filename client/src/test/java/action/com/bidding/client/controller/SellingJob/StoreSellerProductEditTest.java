package action.com.bidding.client.controller.SellingJob;

import action.controller.SellingJobs.StoreSellerProductEdit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StoreSellerProductEditTest {

    @BeforeEach
    public void setUp() {
        // Luôn clear dữ liệu trước mỗi ca test để tránh ô nhiễm trạng thái tĩnh (Static State)
        StoreSellerProductEdit.clear();
    }

    @Test
    public void testDefaultValues() {
        // Kiểm tra xem các giá trị khởi tạo mặc định ban đầu có chuẩn xác không
        assertFalse(StoreSellerProductEdit.editing);
        assertEquals("", StoreSellerProductEdit.auctionId);
        assertEquals("", StoreSellerProductEdit.itemName);
        assertEquals("ELECTRONICS", StoreSellerProductEdit.itemType);
        assertEquals("", StoreSellerProductEdit.description);
        assertEquals("", StoreSellerProductEdit.information1);
        assertEquals("", StoreSellerProductEdit.information2);
        assertEquals("", StoreSellerProductEdit.price);
        assertEquals("", StoreSellerProductEdit.date);
        assertEquals("", StoreSellerProductEdit.time);
        assertEquals("", StoreSellerProductEdit.duration);
        assertEquals("", StoreSellerProductEdit.imageUrl);
    }

    @Test
    public void testClearMethod() {
        // 1. Thay đổi toàn bộ dữ liệu tĩnh sang các giá trị khác để mô phỏng trạng thái đang chỉnh sửa
        StoreSellerProductEdit.editing = true;
        StoreSellerProductEdit.auctionId = "AUC-2026-XYZ";
        StoreSellerProductEdit.itemName = "Mô hình gundam cổ đại";
        StoreSellerProductEdit.itemType = "ART";
        StoreSellerProductEdit.description = "Hàng hiếm số lượng có hạn";
        StoreSellerProductEdit.information1 = "Chất liệu nhựa cao cấp";
        StoreSellerProductEdit.information2 = "Tỉ lệ 1:144";
        StoreSellerProductEdit.price = "2500000";
        StoreSellerProductEdit.date = "2026-05-30";
        StoreSellerProductEdit.time = "22:00:00";
        StoreSellerProductEdit.duration = "60";
        StoreSellerProductEdit.imageUrl = "gundam_render.png";

        // Xác minh dữ liệu đã thay đổi thành công
        assertTrue(StoreSellerProductEdit.editing);
        assertEquals("ART", StoreSellerProductEdit.itemType);

        // 2. Kích hoạt hàm dọn dẹp bộ nhớ tạm
        StoreSellerProductEdit.clear();

        // 3. Khẳng định (Assert) lại xem tất cả các biến đã quay về trạng thái mặc định gốc hay chưa
        assertFalse(StoreSellerProductEdit.editing);
        assertEquals("", StoreSellerProductEdit.auctionId);
        assertEquals("", StoreSellerProductEdit.itemName);
        assertEquals("ELECTRONICS", StoreSellerProductEdit.itemType); // Phải quay lại mặc định "ELECTRONICS"
        assertEquals("", StoreSellerProductEdit.description);
        assertEquals("", StoreSellerProductEdit.information1);
        assertEquals("", StoreSellerProductEdit.information2);
        assertEquals("", StoreSellerProductEdit.price);
        assertEquals("", StoreSellerProductEdit.date);
        assertEquals("", StoreSellerProductEdit.time);
        assertEquals("", StoreSellerProductEdit.duration);
        assertEquals("", StoreSellerProductEdit.imageUrl);
    }
}