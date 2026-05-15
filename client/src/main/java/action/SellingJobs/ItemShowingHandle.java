package action.SellingJobs;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import action.Authentication.StoreItemDataInit;
import action.Core.SceneSwitch;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ItemShowingHandle implements Initializable {

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
    private Label name;
    @FXML
    private Label Type;
    @FXML
    private Label information1;
    @FXML
    private Label information2;
    @FXML
    private Label price;
    @FXML
    private Label status;
    @FXML
    private Label description;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle){
        getItem();
    }
    public void getItem(){
        name.setText(StoreItemDataInit.name);
        Type.setText(StoreItemDataInit.itemType);
        information1.setText(StoreItemDataInit.itemInformation1);
        information2.setText(StoreItemDataInit.itemInformation2);
        price.setText(StoreItemDataInit.price);
        status.setText(StoreItemDataInit.status);
        description.setText(StoreItemDataInit.description);
        String image = StoreItemDataInit.image;
        if(image!=null){
            Image image1 = new Image("/assets/rubberDuck.jfif");
        }

    }
    @FXML
    private TextField money;
    @FXML
    public void RaiseBind(ActionEvent actionEvent) {}
    @FXML
    public void ReturnToInvesment(ActionEvent actionEvent) throws IOException {
        SceneSwitch sceneSwitch = new SceneSwitch();
        sceneSwitch.SwitchToAnyWhere(actionEvent,"/views/Lobby.fxml");
        }
    @FXML
    public ImageView image;
    }



