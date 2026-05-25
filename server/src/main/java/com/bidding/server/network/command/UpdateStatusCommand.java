package com.bidding.server.network.command;

import com.bidding.server.core.AuctionService;
import com.bidding.common.enums.AuctionStatus;
import com.bidding.server.network.ClientHandler;
import com.google.gson.JsonObject;

public class UpdateStatusCommand implements CommandHandler {
    private final AuctionService auctionService;

    public UpdateStatusCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        JsonObject response = new JsonObject();
        response.addProperty("command", "UPDATE_STATUS_RESULT");

        if (!client.isAdmin()) {
            response.addProperty("status", "FAILED");
            response.addProperty("message", "Only admin can update status");
            client.sendMessage(response.toString());
            return;
        }

        if (parts.length < 3) {
            response.addProperty("status", "FAILED");
            response.addProperty("message", "Invalid command format.");
            client.sendMessage(response.toString());
            return;
        }

        String auctionId = parts[1];
        String statusStr = parts[2];

        try {
            AuctionStatus newStatus = AuctionStatus.valueOf(statusStr.toUpperCase());
            // Hàm updateStatus ở AuctionService sẽ được sửa để trả về JSON
            String serviceResponse = auctionService.updateStatus(auctionId, newStatus);
            client.sendMessage(serviceResponse);
        } catch (IllegalArgumentException e) {
            response.addProperty("status", "FAILED");
            response.addProperty("message", "Invalid status: " + statusStr);
            client.sendMessage(response.toString());
        } catch (Exception e) {
            response.addProperty("status", "FAILED");
            response.addProperty("message", "Status update failed: " + e.getMessage());
            client.sendMessage(response.toString());
        }
    }
}