package action.SellingJobs;

import action.SocketClient;
import action.SocketListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.FlowPane;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class InvesmentWaitHandle implements Initializable, SocketListener {
    @FXML
    private FlowPane flowPane;
    private List<List<Object>> list = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (flowPane != null) {
            flowPane.getChildren().clear();
        }
        SocketClient.getInstance().addListener(this);
        JsonObject req = new JsonObject();
        req.addProperty("command", "LIST_AUCTIONS");
        SocketClient.getInstance().requestData(req.toString());
    }

    public void fetchAuctionsFromServer() {
        if (flowPane != null) {
            flowPane.getChildren().clear();
        }
        for(List<Object> item : list) {
            if(!item.isEmpty()) {
                String status = (String) item.get(3);
                // BỘ LỌC CHUYÊN BIỆT: Sảnh chờ chỉ lấy hàng OPEN
                if("OPEN".equals(status)){
                    AuctionCardItem cardItem = new AuctionCardItem(
                            (String) item.get(0),
                            (String) item.get(1),
                            (Double) item.get(2),
                            status,
                            item.size() > 4 ? (String) item.get(4) : ""
                    );
                    if (flowPane != null) {
                        flowPane.getChildren().add(cardItem);
                    }
                }
            }
        }
    }

    @Override
    public void onDataReceived(String data) {
        Platform.runLater(() -> {
            if (data == null || data.isBlank()) {
                return;
            }
            if (!data.startsWith("AUCTION_LIST|") && !data.contains("\"AUCTION_LIST_RESULT\"")) {
                return;
            }

            list.clear();
            try {
                if (data.startsWith("AUCTION_LIST|")) {
                    String dataPart = data.substring("AUCTION_LIST|".length());
                    if (!dataPart.isBlank()) {
                        for (String itemData : dataPart.split(";")) {
                            String[] attr = itemData.split(":");
                            if (attr.length >= 4) {
                                String id = attr[0];
                                String itemName = attr[1];
                                Double currentPrice = Double.parseDouble(attr[2]);
                                String status = attr[3];
                                String imageUrl = attr.length >= 5 ? attr[4] : "";
                                list.add(List.of(itemName, id, currentPrice, status, imageUrl));
                            }
                        }
                    }
                } else {
                    JsonObject res = JsonParser.parseString(data).getAsJsonObject();
                    if (res.has("command") && res.get("command").getAsString().equals("AUCTION_LIST_RESULT")) {
                        JsonArray items = res.getAsJsonArray("items");
                        for (JsonElement elem : items) {
                            JsonObject itemObj = elem.getAsJsonObject();
                            String id = itemObj.get("id").getAsString();
                            String itemName = itemObj.get("itemName").getAsString();
                            Double currentPrice = itemObj.get("currentPrice").getAsDouble();
                            String status = itemObj.get("status").getAsString();
                            String imageUrl = itemObj.has("imageUrl") ? itemObj.get("imageUrl").getAsString() : "";
                            list.add(List.of(itemName, id, currentPrice, status, imageUrl));
                        }
                    }
                }
                fetchAuctionsFromServer();
            } catch (Exception e) {
                System.err.println("[InvesmentWait] Failed to parse auction list: " + data);
                e.printStackTrace();
            }
        });
    }
}
