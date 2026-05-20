package action.MainUI;

import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;

public class UserHolder {
    public final String name;
    public final String balance;
    public final Button button;
    public final String gmail;
    public final String ID;
    public final String SDT;
    UserHolder(String name, String balance, String gmail, String ID, String SDT) {
        this.name = name;
        this.balance = balance;
        this.button = new Button("Xóa tài khoản");
        this.button.getStyleClass().add("btn-gold");
        this.button.setOnMouseClicked(event -> {
           // nhập code logic ở đey
            System.out.println("Xóa!");
        });
        this.gmail = gmail;
        this.ID = ID;
        this.SDT = SDT;
    }
    public Button getButton() {
        return button;
    }
    public String getBalance() {
        return balance;
    }
    public String getName() {
        return name;
    }
    public String getGmail() {
        return gmail;
    }
    public String getID() {
        return ID;
    }
    public String getSDT() {
        return SDT;
    }
}
