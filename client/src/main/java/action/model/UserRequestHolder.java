package action.model;

import javafx.scene.control.Button;

public class UserRequestHolder {
    public final String name;
    public final String balance;
    public final String addingMoneyAmount;
    public final Button button;
    public UserRequestHolder(String name, String balance, String addingMoneyAmount) {
        this.name = name;
        this.balance = balance;
        this.addingMoneyAmount = addingMoneyAmount;
        this.button = new Button("Đồng ý");
        this.button.getStyleClass().add("btn-gold");
        this.button.setOnMouseClicked(event -> {
            System.out.println("Xóa!");
        });

    }
    public Button getButton() {
        return button;
    }
    public String getBalance() {
        return balance;
    }
    public String getAddingMoneyAmount() {
        return addingMoneyAmount;
    }
    public String getName() {
        return name;
    }
}
