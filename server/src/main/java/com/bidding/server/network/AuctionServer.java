package com.bidding.server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.bidding.server.core.AuctionService;

public class AuctionServer {

    private static final int DEFAULT_PORT = 888;
    private static final int MAX_CLIENT_THREADS = 50;

    private final int port;
    private final ExecutorService clientPool;
    private final Set<ClientHandler> connectedClients;

    private ServerSocket serverSocket;
    private volatile boolean running;
    private final AuctionService auctionService;
    private final ScheduledExecutorService auctionMonitor;

    public AuctionServer() {
        this(DEFAULT_PORT);
    }
      
    public AuctionServer(int port) {
        this.port = port;
        this.clientPool = Executors.newFixedThreadPool(MAX_CLIENT_THREADS);
        this.connectedClients = ConcurrentHashMap.newKeySet();
        this.auctionService = new AuctionService();
        this.auctionMonitor = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        running = true;

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("AuctionServer is running on port " + port);
            startAuctionMonitor();

            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] New client connected: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, this, auctionService);
                connectedClients.add(handler);
                clientPool.submit(handler);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error while running server: " + e.getMessage());
            }
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error while closing server socket: " + e.getMessage());
        }

        for (ClientHandler client : connectedClients) {
            client.closeConnection();
        }

        clientPool.shutdownNow();
        auctionMonitor.shutdownNow();
        System.out.println("AuctionServer has stopped.");
    }

    public void removeClient(ClientHandler clientHandler) {
        connectedClients.remove(clientHandler);
        System.out.println("A client has disconnected. Remaining clients: " + connectedClients.size());
    }

    public void broadcast(String message) {
        for (ClientHandler client : connectedClients) {
            client.sendMessage(message);
        }
    }

    public void broadcastToAuctionRoom(String message, String auctionId) {
        for (ClientHandler client : connectedClients) {
            if (auctionId != null && auctionId.equals(client.getWatchingAuctionId())) {
                client.sendMessage(message);
            }
        }
    }
    private void startAuctionMonitor() {
        auctionMonitor.scheduleAtFixedRate(() -> {
            var messages = auctionService.closeExpiredAuctions();

            for (String message : messages) {
                String auctionId = extractAuctionId(message);
                broadcastToAuctionRoom(message, auctionId);
                System.out.println("[AUCTION MONITOR] " + message);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private String extractAuctionId(String message) {
        String[] parts = message.split("\\|");

        for (String part : parts) {
            if (part.startsWith("auctionId=")) {
                return part.substring("auctionId=".length());
            }
        }

        return null;
    }

    public int getConnectedClientCount() {
        return connectedClients.size();
    }
}
