package com.bidding.server.network.command;

import com.bidding.common.enums.AuctionStatus;
import com.bidding.server.core.AuctionService;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.network.service.BroadcastService;

public class UpdateStatusCommand implements CommandHandler {

    private final AuctionService auctionService;
    private final BroadcastService broadcastService;

    public UpdateStatusCommand(AuctionService auctionService, BroadcastService broadcastService) {
        this.auctionService = auctionService;
        this.broadcastService = broadcastService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        if (!client.isAdmin()) {
            client.sendMessage("ERROR|Only admin can update status");
            return;
        }

        if (parts.length < 3) {
            client.sendMessage("ERROR|Invalid command format. Usage: UPDATE_STATUS|auctionId|NEW_STATUS");
            return;
        }

        try {
            AuctionStatus newStatus = AuctionStatus.valueOf(parts[2].toUpperCase());
            String response = auctionService.updateStatus(parts[1], newStatus);
            client.sendMessage(response);
            if (response.startsWith("UPDATE_STATUS_SUCCESS")) {
                broadcastService.broadcastLobbyUpdate();
            }
        } catch (IllegalArgumentException e) {
            client.sendMessage("ERROR|Invalid status: " + parts[2]);
        } catch (Exception e) {
            client.sendMessage("ERROR|Database update failed: " + e.getMessage());
        }
    }
}
