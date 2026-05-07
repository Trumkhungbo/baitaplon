package action;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class RegisterHandle {
    SceneSwitch sceneswitch = new SceneSwitch();
    @FXML
    private TextField Name;
    @FXML
    private TextField PhoneNumber;
    @FXML
    private TextField Email;
    @FXML
    private TextField PersonalID;
    @FXML
    private PasswordField Password;
    @FXML
    private PasswordField ConfirmPassword;
    @FXML
    private Button Login;
    @FXML
    private Button Signup;
    public void Register(ActionEvent event) throws IOException {
        String name = Name.getText();
        String phoneNumber = PhoneNumber.getText();
        String email = Email.getText();
        String personalID = PersonalID.getText();
        String password = Password.getText();
        String confirmPassword = ConfirmPassword.getText();
        if(name.isBlank() || phoneNumber.isBlank() || email.isBlank() || personalID.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            SceneSwitch sceneswitch = new SceneSwitch();
            sceneswitch.SwitchToLockPage(event, "/views/SomeThingUnFill.fxml");
        }
        else if(!password.equals(confirmPassword)) {
            System.out.println("Passwords do not match");
        }
        else if (!email.matches("^[a-zA-Z0-9._%+-]+@gmail\\.com$")) {
            sceneswitch.SwitchToLockPage(event, "/views/WrongInputShow.fxml");
        }
        else if (!phoneNumber.matches("^[0-9]{10}$")) {
            sceneswitch.SwitchToLockPage(event, "/views/WrongInputShow.fxml");
        }
        else if (!personalID.matches("[0-9]{12}$"))
            {
                sceneswitch.SwitchToLockPage(event, "/views/WrongInputShow.fxml");
            }
        else
        {
            StoreSignUpInput.name=name;
            StoreSignUpInput.phoneNumber=phoneNumber;
            StoreSignUpInput.email=email;
            StoreSignUpInput.personalID=personalID;
            StoreSignUpInput.password=password;
            SceneSwitch sceneswitch = new SceneSwitch();
            StartScence.client.sendMessage("REGISTER|" + name + "|" + password);
            sceneswitch.SwitchToLogin(event);
            sceneswitch.SwitchToLockPage(event, "/views/RegisterPopUp.fxml");
        }

    }
    public void Login(ActionEvent event) throws IOException {
        sceneswitch.SwitchToLogin(event);
    }
}
