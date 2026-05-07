package com.bidding.server;

import com.bidding.server.network.AuctionServer;

public class ServerApplication {
    public static void main(String[] args) {
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
    }
}
