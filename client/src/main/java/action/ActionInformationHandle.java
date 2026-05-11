package action;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
 
public class ActionInformationHandle {
    @FXML
    private Label personalID ;
    @FXML
    private Label personalName = new Label((String) (StoreSignUpInput.personalID));
    @FXML
    private Label name = new Label((String) (StoreSignUpInput.name));
    @FXML
    private Label email =  new Label((String) (StoreSignUpInput.email));
    @FXML
    private Label phone = new Label( (String)(StoreSignUpInput.phoneNumber));
    @FXML
    private Label password = new Label( (String)(StoreSignUpInput.password));
    @FXML
    private Label balance;

    public Label getPersonalName() {
        return personalName;
    }
    public Label getPersonalID() {
        return personalID;
    }
    public Label getName() {
        return name;
    }
    public Label getEmail() {
        return email;
    }
    public Label getPhone() {
        return phone;
    }
    public Label getPassword() {
        return password;
    }
}
