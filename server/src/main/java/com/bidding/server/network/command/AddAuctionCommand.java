package com.bidding.server.network.command;

import com.bidding.common.model.item.Art;
import com.bidding.common.model.item.Electronics;
import com.bidding.common.model.item.Item;
import com.bidding.common.model.item.Vehicle;
import com.bidding.server.core.AuctionService;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.network.service.BroadcastService;

public class AddAuctionCommand implements CommandHandler {

    private final AuctionService auctionService;
    private final BroadcastService broadcastService;

    public AddAuctionCommand(AuctionService auctionService, BroadcastService broadcastService) {
        this.auctionService = auctionService;
        this.broadcastService = broadcastService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (!client.isLoggedIn()) {
            client.sendMessage("ERROR|You must login first");
            return;
        }

        // Định dạng chuẩn: ADD_AUCTION|sellerUsername|itemType|itemName|startPrice|param1|param2
        if (parts.length < 4) {
            client.sendMessage("ERROR|Invalid syntax. Use: ADD_AUCTION|sellerUsername|itemName|startPrice or ADD_AUCTION|sellerUsername|itemType|itemName|startPrice|...");
            return;
        }

        String sellerUsername = parts[1];
        if (!client.getCurrentUser().equals(sellerUsername)) {
            client.sendMessage("ERROR|You can only create auctions for your own account");
            return;
        }

        try {
            if (parts.length == 4) {
                double startPrice = Double.parseDouble(parts[3]);
                String response = auctionService.addAuction(sellerUsername, parts[2], startPrice);
                client.sendMessage(response);

                if (response.startsWith("ADD_AUCTION_SUCCESS")) {
                    broadcastService.broadcastLobbyUpdate();
                }
                return;
            }

            if (parts.length < 6) {
                client.sendMessage("ERROR|Invalid syntax. Use: ADD_AUCTION|sellerUsername|itemType|itemName|startPrice|...");
                return;
            }

            String itemType = parts[2];
            String itemName = parts[3];
            double startPrice = Double.parseDouble(parts[4]);

            Item item;

            switch (itemType.toUpperCase()) {
                case "ELECTRONICS" -> {
                    if (parts.length < 7) {
                        client.sendMessage("ERROR|Electronics format invalid. Need brand and warrantyMonths");
                        return;
                    }
                    String brand = parts[5];
                    int warrantyMonths = Integer.parseInt(parts[6]);

                    item = new Electronics(
                            itemName,
                            "No description",
                            startPrice,
                            "",
                            brand,
                            warrantyMonths
                    );
                }
                case "VEHICLE" -> {
                    if (parts.length < 7) {
                        client.sendMessage("ERROR|Vehicle format invalid. Need engineType and mileage");
                        return;
                    }
                    String engineType = parts[5];
                    int mileage = Integer.parseInt(parts[6]);

                    item = new Vehicle(
                            itemName,
                            "No description",
                            startPrice,
                            "",
                            engineType,
                            mileage
                    );
                }
                case "ART" -> {
                    if (parts.length < 7) {
                        client.sendMessage("ERROR|Art format invalid. Need artist and creationYear");
                        return;
                    }
                    String artist = parts[5];
                    int creationYear = Integer.parseInt(parts[6]);

                    item = new Art(
                            itemName,
                            "No description",
                            startPrice,
                            "",
                            artist,
                            creationYear
                    );
                }
                default -> {
                    client.sendMessage("ERROR|Unsupported item type");
                    return;
                }
            }

            String response = auctionService.createAuction(sellerUsername, item);
            client.sendMessage(response);

            broadcastService.broadcastLobbyUpdate(auctionService.getAuctionList());

        } catch (NumberFormatException e) {
            client.sendMessage("ERROR|Start price, mileage, or year must be a valid number");
        } catch (Exception e) {
            client.sendMessage("ERROR|" + e.getMessage());
            e.printStackTrace();
        }
    }
}
