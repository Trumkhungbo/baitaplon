package action.SellingJobs;

import action.Core.StartScence;
import javafx.application.Platform;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AuctionItems implements Initializable {
    public static ArrayList<List> list = new ArrayList<>();
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        fetchAuctionsFromServer();
        System.out.println("Dang lam roi tutu");
    }
    public void fetchAuctionsFromServer(){
        StartScence.client.sendMessage("LIST_AUCTIONS");
    StartScence.client.setServerListener(message -> {
        System.out.println("Nhận được từ Server: " + message);

        if (message.startsWith("AUCTION_LIST|")) {
            String dataPart = message.substring("AUCTION_LIST|".length());

            if (dataPart.isEmpty()) {
                System.out.println("Không có sản phẩm nào.");
                return;
            }

            Platform.runLater(() -> {

                String[] items = dataPart.split(";");
                for (String itemData : items) {
                    String[] attributes = itemData.split(":");
                    if (attributes.length >= 4) {
                        String id = attributes[0];
                        String itemName = attributes[1];
                        String itemType = attributes[2];
                        String itemInformation1 = attributes[3];
                        String itemInformation2 = attributes[4];
                        Double currentPrice = Double.parseDouble(attributes[5]);
                        String status = attributes[6];
                        list.add(List.of(itemName, itemType, itemInformation1, itemInformation2, currentPrice, status));
                        System.out.println("Meo");
                    }
                }});}});}}