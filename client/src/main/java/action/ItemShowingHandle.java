package action;
import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ItemShowingHandle {

    // Nếu bạn có gán ID cho 2 nút này trong Scene Builder thì thêm @FXML vào
    @FXML
    private Button buttonLeft;
    @FXML
    private Button buttonRight;

    @FXML
    private ImageView imageView;

    // Khởi tạo list trống để tránh NullPointer
    private List<Image> imageList = new ArrayList<>();

    // Biến lưu giữ vị trí ảnh đang hiển thị
    private int currentIndex = 0;

    /**
     * Trong JavaFX, hàm initialize() luôn tự động được gọi SAU KHI
     * file FXML đã nạp xong các giao diện. Đây là nơi chuẩn nhất để nạp dữ liệu.
     */


    @FXML
    public void LeftClicked(ActionEvent actionEvent) {
        // Nếu không phải là ảnh đầu tiên thì mới cho lùi
        if (currentIndex > 0) {
            currentIndex--; // Lùi index
            imageView.setImage(imageList.get(currentIndex)); // Đổi ảnh trên màn hình
        }
    }

    @FXML
    public void RightClicked(ActionEvent actionEvent) {
        // Nếu không phải là ảnh cuối cùng thì mới cho tiến
        if (currentIndex < imageList.size() - 1) {
            currentIndex++; // Tăng index
            imageView.setImage(imageList.get(currentIndex)); // Đổi ảnh trên màn hình
        }
    }
}