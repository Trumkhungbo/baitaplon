package com.bidding.server;

import com.bidding.server.database.DatabaseInitializer;
import com.bidding.server.database.DatabaseManager;
import com.bidding.server.network.AuctionServer;

import action.Core.StartScence;
import javafx.application.Application;

public class ServerApplication {

    public static void main(String[] args) {
        int port = 888;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port, using default port 888");
            }
        }

        // Init database 
        try {
            DatabaseManager.getInstance();
            DatabaseInitializer.initialize();
            System.out.println("[DB] Database initialized successfully");
        } catch (Exception e) {
            System.err.println("[DB] Failed to initialize database: " + e.getMessage());
            System.exit(1);
        }

        // Server chạy trên thread riêng vì server.start() là blocking
        AuctionServer server = new AuctionServer(port);

        Thread serverThread = new Thread(server::start, "AuctionServer-Thread");
        serverThread.setDaemon(false); // false để server thoát sạch, không bị kill đột ngột
        serverThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[SERVER] Shutting down...");
            server.stop();
        }, "ShutdownHook-Thread"));

        // JavaFX chạy trên main thread
        Application.launch(StartScence.class, args);
    }
}