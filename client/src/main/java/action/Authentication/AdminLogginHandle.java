package action.Authentication;

import action.Core.SceneSwitch;
import action.SocketClient;
import com.google.gson.JsonObject;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

public class AdminLogginHandle implements Initializable {
    private static final LocalDate INDEPENDENT_DAY = LocalDate.of(2026, 4, 30);
    private static final DateTimeFormatter ADMIN_DATE_FORMAT = DateTimeFormatter.ofPattern("d/M/yyyy");

    @FXML
    private Button button;
    @FXML
    private DatePicker datePicker;
    SceneSwitch sceneSwitch = new SceneSwitch();
    @FXML
    private Label label;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        datePicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : ADMIN_DATE_FORMAT.format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                return parseDate(text.trim());
            }
        });
    }

    public void Submitting(ActionEvent event) throws IOException {
        LocalDate currentDate = resolveAdminDate();
        if (currentDate == null) {
            sceneSwitch.SwitchToLockPage(event, "/views/SomeThingUnFill.fxml");
        } else {
            if (currentDate.equals(INDEPENDENT_DAY)) {
                JsonObject req = new JsonObject();
                req.addProperty("command", "ELEVATE");

                SocketClient.getInstance().requestData(req.toString());
                sceneSwitch.SwitchToAnyWhere(event, "/views/AdminLobby.fxml");
            } else {
                label.setText("Sai ngày xác thực. Hãy nhập 30/04/2026.");
            }
        }
    }

    private LocalDate resolveAdminDate() {
        String rawDate = datePicker.getEditor() == null ? "" : datePicker.getEditor().getText();
        if (rawDate != null && !rawDate.isBlank()) {
            return parseDate(rawDate.trim());
        }
        return datePicker.getValue();
    }

    private LocalDate parseDate(String rawDate) {
        try {
            return LocalDate.parse(rawDate, ADMIN_DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    public void ReturnLogin(ActionEvent event) throws IOException {
        sceneSwitch.SwitchToLogin(event);
    }
}
