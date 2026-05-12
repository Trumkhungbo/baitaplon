package action.SellingJobs;

import action.Core.SceneSwitch;
import action.Core.StartScence;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.time.LocalTime;

public class InvesterSellHandle {
    @FXML
    private TextField itemname;
    @FXML
    private TextField description;
    @FXML
    private TextField price;
    @FXML
    private DatePicker date;
    @FXML
    private TextField TimeStart;

    @FXML
    private TextField duration;
    @FXML
    private ImageView imageset;
    SceneSwitch sceneSwitch = new SceneSwitch();
    @FXML
    public void Clicked(ActionEvent actionEvent) throws IOException {
        try {
            LocalTime StarTime = LocalTime.parse(TimeStart.getText());
            Time DUration = Time.valueOf(duration.getText());
            String priced = price.getText();
            Double priceFunc =  Double.parseDouble(priced);
            ItemsHolder item = new ItemsHolder(itemname.getText(),description.getText(),priceFunc,date.getValue(),StarTime,DUration);
            ShopDataBase.danhSachSanPham.add(item);

        }
        catch (Exception e) {
            sceneSwitch.SwitchToLockPage(actionEvent, "/views/WrongInputShow.fxml");
        }
    }
    @FXML
    public void AddImage(ActionEvent actionEvent) {
        // 1. Lấy Stage (cửa sổ hiện tại) từ chính sự kiện click nút bấm
        // Phải import javafx.scene.Node nếu hệ thống báo đỏ nhé
        Stage currentStage = (Stage) ((javafx.scene.Node) actionEvent.getSource()).getScene().getWindow();

        // 2. Tạo hộp thoại chọn file
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh của bạn");

        // Lọc chỉ cho phép chọn các định dạng ảnh thông dụng
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        // 3. Hiển thị hộp thoại và truyền currentStage vào thay cho primaryStage
        File selectedFile = fileChooser.showOpenDialog(currentStage);

        // 4. Nếu người dùng có chọn file (không bấm Cancel)
        if (selectedFile != null) {
            // Biến đổi đường dẫn file thành URI và load vào đối tượng Image
            Image image = new Image(selectedFile.toURI().toString());

            // Set ảnh vào ImageView (imageset) để hiện lên màn hình FXML
            imageset.setImage(image);
        }
    }
    }

