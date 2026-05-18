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
    @SuppressWarnings("unused")
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

        if (parts.length < 3) {
            client.sendMessage("ERROR|Invalid syntax. Use: ADD_AUCTION|sellerUsername|type|name|des1|des2|price|startTime|durationMinutes|description|imageUrl");
            return;
        }

        String sellerUsername = parts[1];
        String type = parts[2].toUpperCase();

        try {
            if (!sellerUsername.equals(client.getCurrentUser())) {
                client.sendMessage("ERROR|Seller username does not match current user");
                return;
            }

            if (parts.length == 4) {
                double price = Double.parseDouble(parts[3]);
                if (price <= 0) {
                    client.sendMessage("ERROR|Price must be greater than 0");
                    return;
                }
                String response = auctionService.addAuction(sellerUsername, parts[2], price);
                client.sendMessage(response);
                if (response.startsWith("ADD_AUCTION_SUCCESS")) {
                    broadcastService.broadcastLobbyUpdate();
                }
                return;
            }

            if (parts.length < 9) {
                client.sendMessage("ERROR|Invalid syntax. Use: ADD_AUCTION|sellerUsername|type|name|des1|des2|price|startTime|durationMinutes|description|imageUrl");
                return;
            }

            String name = parts[3];
            String des1 = parts[4];
            String des2 = parts[5];
            double price = Double.parseDouble(parts[6]);
            long startTime = Long.parseLong(parts[7]);
            System.out.println(startTime);
            long durationMinutes = Long.parseLong(parts[8]);
            System.out.println(durationMinutes);
            String description = (parts.length > 9) ? parts[9] : "";
            String imageUrl = (parts.length > 10) ? parts[10] : "";

            if (price <= 0) {
                client.sendMessage("ERROR|Price must be greater than 0");
                return;
            }

            if (startTime <= 0) {
                client.sendMessage("ERROR|Start time must be greater than 0");
                return;
            }

            // SỬA TẠI ĐÂY: Validation khoảng duration hợp lệ (Tối đa 7 ngày = 10080 phút)
            if (durationMinutes <= 0 || durationMinutes > 10080) {
                client.sendMessage("ERROR|Duration must be 1-10080 minutes");
                return;
            }

            Item item = switch (type) {
                case "ELECTRONICS" -> new Electronics(name, price, "", des1, Integer.parseInt(des2));
                case "ART" -> new Art(name, price, "", des1, Integer.parseInt(des2));
                case "VEHICLE" -> new Vehicle(name, price, "", des1, Integer.parseInt(des2));
                default -> null;
            };

            if (item == null) {
                client.sendMessage("ERROR|Invalid item type. Supported types: ELECTRONICS, ART, VEHICLE");
                return;
            }

            // Gắn imageUrl nếu có
            item.setDescription(description);

            if (!imageUrl.isBlank()) {
                item.setImageUrl(imageUrl);
            }

            client.sendMessage(auctionService.createPendingAuction(
                    sellerUsername,
                    item,
                    startTime,
                    durationMinutes
            ));
        } catch (NumberFormatException e) {
            client.sendMessage("ERROR|Price, startTime, durationMinutes, or des2 must be a valid number");
        } catch (Exception e) {
            client.sendMessage("ERROR|" + e.getMessage());
        }
    }
}
