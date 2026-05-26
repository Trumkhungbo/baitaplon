package com.bidding.server;

import com.bidding.server.network.AuctionServer;
import com.bidding.server.database.DatabaseInitializer;

public class ServerApplication {
    public static void main(String[] args) {
        try {
            System.out.println("[SYSTEM] Dang kiem tra va khoi tao Database...");
            DatabaseInitializer.initialize();
        } catch (Exception e) {
            System.err.println("[CRITICAL] Khong the khoi tao Database. Server se dung.");
            e.printStackTrace();
            return;
        }

        Thread serverThread = new Thread(() -> {
            int port = 888;

            if (args.length > 0) {
                try {
                    port = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid port. Using default port 888");
                }
            }

            AuctionServer server = new AuctionServer(port);
            server.start();
        }, "auction-server-main");

        serverThread.start();
    }
}
