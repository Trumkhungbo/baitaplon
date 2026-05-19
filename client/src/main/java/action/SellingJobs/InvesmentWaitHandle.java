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

    private final List<AuctionWaitItem> list = new ArrayList<>();

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
        for (AuctionWaitItem item : list) {
            if (!"OPEN".equals(item.status())) {
                continue;
            }

            AuctionCardItem cardItem = new AuctionCardItem(
                    item.itemName(),
                    item.id(),
                    item.currentPrice(),
                    item.status(),
                    item.imageUrl(),
                    item.itemType(),
                    item.startTime(),
                    item.endTimeMillis(),
                    item.serverTimeMillis()
            );
            flowPane.getChildren().add(cardItem);
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
                            if (attr.length >= 14) {
                                list.add(new AuctionWaitItem(
                                        attr[0],
                                        attr[1],
                                        Double.parseDouble(attr[2]),
                                        attr[3],
                                        attr[4],
                                        attr[11],
                                        attr[9],
                                        Long.parseLong(attr[12]),
                                        Long.parseLong(attr[13])
                                ));
                            }
                        }
                    }
                } else {
                    JsonObject res = JsonParser.parseString(data).getAsJsonObject();
                    if (res.has("command") && res.get("command").getAsString().equals("AUCTION_LIST_RESULT")) {
                        JsonArray items = res.getAsJsonArray("items");
                        for (JsonElement elem : items) {
                            JsonObject itemObj = elem.getAsJsonObject();
                            list.add(new AuctionWaitItem(
                                    itemObj.get("id").getAsString(),
                                    itemObj.get("itemName").getAsString(),
                                    itemObj.get("currentPrice").getAsDouble(),
                                    itemObj.get("status").getAsString(),
                                    itemObj.has("imageUrl") ? itemObj.get("imageUrl").getAsString() : "",
                                    itemObj.has("itemType") ? itemObj.get("itemType").getAsString() : "",
                                    itemObj.has("startTime") ? itemObj.get("startTime").getAsString() : "--:--",
                                    itemObj.has("endTime") ? itemObj.get("endTime").getAsLong() : 0L,
                                    itemObj.has("serverTime") ? itemObj.get("serverTime").getAsLong() : System.currentTimeMillis()
                            ));
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

    private record AuctionWaitItem(
            String id,
            String itemName,
            Double currentPrice,
            String status,
            String imageUrl,
            String itemType,
            String startTime,
            long endTimeMillis,
            long serverTimeMillis
    ) {
    }
}
