package action.SellingJobs;

import action.Authentication.StoreDataInput;
import action.Core.SceneSwitch;
import action.Core.StartScence;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ResourceBundle;

public class InvesterSellHandle implements Initializable {
    @Override
    public void initialize(URL location, ResourceBundle resource){
        setDescription(description);
    }
    @FXML
    private TextField itemname;
    @FXML
    private ChoiceBox<String> description;
    @FXML
    private TextField description1;
    @FXML
    private TextField description2;
    @FXML
    private TextField price;
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
            int minutesInput = Integer.parseInt(duration.getText());
            int hours = minutesInput / 60;
            int minutes = minutesInput % 60;
            String timeString = String.format("%02d:%02d:00", hours, minutes);
            Time DUration = Time.valueOf(timeString);
            String priced = price.getText();
            Long priceFunc = Long.parseLong(priced);
            //ItemsHolder item = new ItemsHolder("",itemname.getText(), priceFunc, StarTime, DUration);
            //ShopDataBase.danhSachSanPham.add(item);
            // Định dạng chuẩn: ADD_AUCTION|sellerUsername|itemType|itemName|startPrice|param1|param2

            String message = "ADD_AUCTION|"+StoreDataInput.getUsername()+"|"+description.getValue()+"|"+itemname.getText()+"|"+description1.getText()+"|"+description2.getText()+"|" + priceFunc +"|"+StarTime+"|"+DUration;
            StartScence.client.sendMessage(message);
            System.out.println(message);


            System.out.println("Đăng bán thành công!");
        }
        catch (Exception e) {
            sceneSwitch.SwitchToLockPage(actionEvent, "/views/WrongInputShow.fxml");
            e.printStackTrace();
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

    public void setDescription(ChoiceBox<String> description) {
        this.description = description;
        description.getItems().addAll("Thông Tin","ELECTRONICS","ART","VEHICLE");
        description.setValue(description.getItems().get(0));
    }
}

