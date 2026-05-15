package action.SellingJobs;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.FlowPane;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class InvesmentWaitHandle implements Initializable {
    @FXML
    private FlowPane flowPane;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        AuctionItems.currentListener = () -> {fetchAuctionsFromServer();};
        AuctionItems.requestData();
        if (flowPane != null) {
            flowPane.getChildren().clear();
        }
    }

    public void fetchAuctionsFromServer() {
        for(List item:AuctionItems.list) {
            if(!item.isEmpty()) {
                if(((String) item.get(6)).equals("OPEN")){
                    AuctionCardItem cardItem = new AuctionCardItem((String) item.get(0),(String) item.get(1),(String) item.get(2),(String) item.get(3),(String) item.get(4),(Double) item.get(5),(String) item.get(6));
                    if (flowPane != null) {
                        flowPane.getChildren().add(cardItem);
                    }
                }
            }
        }
    }

}