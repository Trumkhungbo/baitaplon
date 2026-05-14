package com.bidding.server.network.command;

import com.bidding.server.core.AuctionService;
import com.bidding.server.core.AuctionStatus;
import com.bidding.server.core.Auction;
import com.bidding.server.network.ClientHandler;
import com.bidding.server.repository.AuctionDAO;
import com.bidding.server.repository.ItemDAO;

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

        Auction auction = auctionService.findAuctionById(auctionId);
        if (auction == null) {
            client.sendMessage("ERROR|Auction not found");
            return;
        }

        try {
            AuctionStatus newStatus = AuctionStatus.valueOf(statusStr.toUpperCase());
            // Cập nhật trạng thái trên RAM
            auction.setStatus(newStatus);

            // LƯU Ý: Phải gọi DAO để cập nhật database
            ItemDAO itemDAO = new ItemDAO();
            AuctionDAO auctionDAO = new AuctionDAO(itemDAO);
            // id trong Database là kiểu long
            com.bidding.common.enums.AuctionStatus newStatusDAO = com.bidding.common.enums.AuctionStatus.valueOf(statusStr.toUpperCase());
            auctionDAO.updateStatus(Long.parseLong(auctionId),newStatusDAO);

            client.sendMessage("UPDATE_STATUS_SUCCESS|Auction " + auctionId + " is now " + newStatus.name());
        } catch (IllegalArgumentException e) {
            client.sendMessage("ERROR|Invalid status: " + statusStr);
        } catch (Exception e) {
            client.sendMessage("ERROR|Database update failed: " + e.getMessage());
        }
    }
}
