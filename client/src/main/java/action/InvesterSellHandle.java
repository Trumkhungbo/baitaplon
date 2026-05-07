package action;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.Time;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

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
    SceneSwitch sceneSwitch = new SceneSwitch();
    public void Clicked(ActionEvent actionEvent) throws IOException {
        try {
            // Ép chuỗi "14:30" thành đối tượng LocalTime
            LocalTime StarTime = LocalTime.parse(TimeStart.getText());
            // Thành công thì cho vào ItemsHolder
            Time DUration = Time.valueOf(duration.getText());
            String priced = price.getText();
            Double priceFunc =  Double.parseDouble(priced);
            ItemsHolder item = new ItemsHolder(itemname.getText(),description.getText(),priceFunc,date.getValue(),StarTime,DUration);
            ShopDataBase.danhSachSanPham.add(item);
        }
        catch (Exception e) {
            sceneSwitch.SwitchToLockPage(actionEvent,"WrongInputShow.fxml");
        }
    }
}
