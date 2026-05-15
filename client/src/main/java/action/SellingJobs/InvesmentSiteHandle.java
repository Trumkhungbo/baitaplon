package action.SellingJobs;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.FlowPane;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class InvesmentSiteHandle implements Initializable {
    @FXML
    private FlowPane flowPane;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        AuctionItems.currentListener = () -> { fetchAuctionsFromServer(); };
        AuctionItems.requestData();
        if (flowPane != null) {
            flowPane.getChildren().clear();
        }
    }

    public void fetchAuctionsFromServer() {
        if (flowPane != null) {
            flowPane.getChildren().clear(); // Xóa sạch danh sách cũ trước khi vẽ danh sách mới
        }

        for (List item : AuctionItems.list) {
            if (!item.isEmpty()) {
                String status = (String) item.get(3);
                // Hiển thị cả phiên đấu giá đang chạy (RUNNING) và mới mở (OPEN)
                if ("RUNNING".equals(status) ) {
                    AuctionCardItem cardItem = new AuctionCardItem(
                            (String) item.get(0),
                            (String) item.get(1),
                            (Double) item.get(2),
                            status
                    );
                    if (flowPane != null) {
                        flowPane.getChildren().add(cardItem);
                    }
                }
            }
        }
    }
}