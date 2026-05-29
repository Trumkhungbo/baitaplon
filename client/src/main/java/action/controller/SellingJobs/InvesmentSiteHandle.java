package action.controller.SellingJobs;

import action.model.AuctionCardItem;
import action.network.SocketClient;
import action.network.SocketListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class InvesmentSiteHandle implements Initializable, SocketListener {
    @FXML private FlowPane flowPane;
    @FXML private Button filterAllButton;
    @FXML private Button filterElectronicsButton;
    @FXML private Button filterArtButton;
    @FXML private Button filterVehiclesButton;

    private final List<AuctionSummary> list = new ArrayList<>();
    private String currentFilter = "ALL";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (flowPane != null) {
            flowPane.getChildren().clear();
        }
        SocketClient.getInstance().addListener(this);
        requestAuctionList();
        updateFilterStyles();
    }

    @FXML
    public void showAll() {
        currentFilter = "ALL";
        refreshCards();
    }

    @FXML
    public void showElectronics() {
        currentFilter = "ELECTRONICS";
        refreshCards();
    }

    @FXML
    public void showArt() {
        currentFilter = "ART";
        refreshCards();
    }

    @FXML
    public void showVehicles() {
        currentFilter = "VEHICLE";
        refreshCards();
    }

    private void requestAuctionList() {
        JsonObject req = new JsonObject();
        req.addProperty("command", "LIST_AUCTIONS");
        SocketClient.getInstance().requestData(req.toString());
    }

    private void refreshCards() {
        updateFilterStyles();
        if (flowPane != null) {
            flowPane.getChildren().clear();
        }

        List<AuctionSummary> filtered = list.stream()
                .filter(this::matchesFilter)
                .sorted(Comparator.comparingInt(item -> statusPriority(item.status())))
                .toList();

        String currentGroup = null;
        for (AuctionSummary item : filtered) {
            if (!item.status().equals(currentGroup) && flowPane != null) {
                flowPane.getChildren().add(createGroupLabel(item.status()));
                currentGroup = item.status();
            }

            AuctionCardItem card = new AuctionCardItem(
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
            flowPane.getChildren().add(card);
        }
    }

    private boolean matchesFilter(AuctionSummary item) {
        if ("ALL".equals(currentFilter)) {
            return true;
        }
        return currentFilter.equalsIgnoreCase(item.itemType());
    }

    private Node createGroupLabel(String status) {
        Label label = new Label(mapStatusTitle(status));
        label.setStyle("-fx-text-fill: #FACC15; -fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 6 0 4 4;");
        label.setPrefWidth(1200);
        return label;
    }

    private int statusPriority(String status) {
        return switch (status) {
            case "RUNNING" -> 0;
            case "OPEN" -> 1;
            case "PENDING" -> 2;
            case "FINISHED", "PAID", "CANCELED" -> 3;
            default -> 4;
        };
    }

    private String mapStatusTitle(String status) {
        return switch (status) {
            case "RUNNING" -> "Đang diễn ra";
            case "OPEN" -> "Sắp bắt đầu";
            case "PENDING" -> "Đang chờ duyệt";
            case "PAID" -> "Đã thanh toán";
            case "CANCELED" -> "Đã hủy";
            case "FINISHED" -> "Đã kết thúc";
            default -> "Đang chờ duyệt";
        };
    }

    private void updateFilterStyles() {
        updateFilterButton(filterAllButton, "ALL".equals(currentFilter));
        updateFilterButton(filterElectronicsButton, "ELECTRONICS".equals(currentFilter));
        updateFilterButton(filterArtButton, "ART".equals(currentFilter));
        updateFilterButton(filterVehiclesButton, "VEHICLE".equals(currentFilter));
    }

    private void updateFilterButton(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.setStyle(active
                ? "-fx-background-color: rgba(250, 204, 21, 0.18); -fx-border-color: #FACC15; -fx-border-radius: 18; -fx-background-radius: 18; -fx-text-fill: #FACC15; -fx-font-weight: bold;"
                : "-fx-background-color: rgba(255,255,255,0.04); -fx-border-color: #333333; -fx-border-radius: 18; -fx-background-radius: 18; -fx-text-fill: #D4D4D8;");
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
                                list.add(new AuctionSummary(
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
                            list.add(new AuctionSummary(
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
                refreshCards();
            } catch (Exception e) {
                System.err.println("[InvesmentSite] Failed to parse auction list: " + data);
                e.printStackTrace();
            }
        });
    }

    private record AuctionSummary(
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
