package com.bidding.server.network.command;

import com.bidding.common.model.item.Art;
import com.bidding.common.model.item.Electronics;
import com.bidding.common.model.item.Item;
import com.bidding.common.model.item.Vehicle;
import com.bidding.server.core.AuctionService;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.network.service.BroadcastService;

public class UpdateAuctionCommand implements CommandHandler {

    private final AuctionService auctionService;
    private final BroadcastService broadcastService;

    public UpdateAuctionCommand(AuctionService auctionService, BroadcastService broadcastService) {
        this.auctionService = auctionService;
        this.broadcastService = broadcastService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (!client.isLoggedIn()) {
            client.sendMessage("ERROR|You must login first");
            return;
        }

        if (parts.length < 11) {
            client.sendMessage("ERROR|Invalid syntax. Use: UPDATE_AUCTION|auctionId|sellerUsername|type|name|des1|des2|price|startTime|durationMinutes|description|imageUrl");
            return;
        }

        String auctionId = parts[1];
        String sellerUsername = parts[2];
        String type = parts[3].toUpperCase();

        try {
            if (!sellerUsername.equals(client.getCurrentUser())) {
                client.sendMessage("ERROR|Seller username does not match current user");
                return;
            }

            String name = parts[4];
            String des1 = parts[5];
            String des2 = parts[6];
            double price = Double.parseDouble(parts[7]);
            long startTime = Long.parseLong(parts[8]);
            long durationMinutes = Long.parseLong(parts[9]);
            String description = parts.length > 10 ? parts[10] : "";
            String imageUrl = parts.length > 11 ? parts[11] : "";

            if (price <= 0) {
                client.sendMessage("ERROR|Price must be greater than 0");
                return;
            }

            if (startTime <= 0) {
                client.sendMessage("ERROR|Start time must be greater than 0");
                return;
            }

            if (durationMinutes <= 0 || durationMinutes > 10080) {
                client.sendMessage("ERROR|Duration must be 1-10080 minutes");
                return;
            }

            Item item = switch (type) {
                case "ELECTRONICS" -> new Electronics(name, price, imageUrl, des1, Integer.parseInt(des2));
                case "ART" -> new Art(name, price, imageUrl, des1, Integer.parseInt(des2));
                case "VEHICLE" -> new Vehicle(name, price, imageUrl, des1, Integer.parseInt(des2));
                default -> null;
            };

            if (item == null) {
                client.sendMessage("ERROR|Invalid item type. Supported types: ELECTRONICS, ART, VEHICLE");
                return;
            }

            item.setDescription(description);
            if (!imageUrl.isBlank()) {
                item.setImageUrl(imageUrl);
            }

            String response = auctionService.updateSellerAuction(
                    sellerUsername,
                    auctionId,
                    item,
                    startTime,
                    durationMinutes
            );
            client.sendMessage(response);

            if (response.startsWith("UPDATE_AUCTION_SUCCESS")) {
                broadcastService.broadcastLobbyUpdate();
            }
        } catch (NumberFormatException e) {
            client.sendMessage("ERROR|Price, startTime, durationMinutes, or des2 must be a valid number");
        } catch (Exception e) {
            client.sendMessage("ERROR|" + e.getMessage());
        }
    }
}
