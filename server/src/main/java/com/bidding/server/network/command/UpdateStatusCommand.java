package com.bidding.server.network.command;

import com.bidding.server.core.AuctionService;
import com.bidding.common.enums.AuctionStatus;
import com.bidding.server.network.ClientHandler;

public class UpdateStatusCommand implements CommandHandler {

    private final AuctionService auctionService;

    public UpdateStatusCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public void handle(String[] parts, ClientHandler client) {
        // Kiểm tra quyền Admin
        if (!client.isAdmin()) {
            client.sendMessage("ERROR|Only admin can update status");
            return;
        }

        // Kiểm tra đủ tham số lệnh: UPDATE_STATUS|auctionId|NEW_STATUS
        if (parts.length < 3) {
            client.sendMessage("ERROR|Invalid command format. Usage: UPDATE_STATUS|auctionId|NEW_STATUS");
            return;
        }

        String auctionId = parts[1];
        String statusStr = parts[2];

        try {
            AuctionStatus newStatus = AuctionStatus.valueOf(statusStr.toUpperCase());
            String response = auctionService.updateStatus(auctionId, newStatus);
            client.sendMessage(response);
        } catch (IllegalArgumentException e) {
            client.sendMessage("ERROR|Invalid status: " + statusStr);
        } catch (Exception e) {
            client.sendMessage("ERROR|Status update failed: " + e.getMessage());
        }
    }
}
