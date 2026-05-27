package com.bidding.server.network;

import com.bidding.server.core.AuctionService;
import com.bidding.server.core.AuthService;
import com.bidding.server.network.command.CommandDispatcher;
import com.bidding.server.network.service.BroadcastService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Server TCP chính của hệ thống đấu giá.
 * Lắng nghe kết nối client trên port 888 (mặc định), mỗi client được xử lý bởi
 * một thread riêng từ pool (tối đa 50 threads). AuctionMonitor chạy định kỳ mỗi
 * giây để đóng các phiên hết giờ và broadcast thông báo.
 */
public class AuctionServer {

    private static final int DEFAULT_PORT = 888;
    private static final int MAX_CLIENT_THREADS = 50;

    private final int port;
    private final ExecutorService clientPool;
    private final Set<ClientHandler> connectedClients;

    private ServerSocket serverSocket;
    private volatile boolean running;
    private final AuctionService auctionService;
    private final AuthService authService;
    private final BroadcastService broadcastService;
    private final CommandDispatcher commandDispatcher;
    private final ScheduledExecutorService auctionMonitor;

    public AuctionServer() {
        this(DEFAULT_PORT);
    }
      
    /**
     * Khởi tạo server với port tùy chỉnh.
     * Tạo sẵn: thread pool cho client, AuctionService, AuthService,
     * BroadcastService, CommandDispatcher và AuctionMonitor.
     */
    public AuctionServer(int port) {
        this.port = port;
        this.clientPool = Executors.newFixedThreadPool(MAX_CLIENT_THREADS);
        this.connectedClients = ConcurrentHashMap.newKeySet();
        this.auctionService = new AuctionService();
        this.authService = new AuthService();
        this.broadcastService = new BroadcastService(this, auctionService);
        this.commandDispatcher = new CommandDispatcher(authService, auctionService, broadcastService);
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

                ClientHandler handler = new ClientHandler(clientSocket, this, commandDispatcher);
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

    /**
     * Gửi message cho tất cả client đang theo dõi một phiên đấu giá cụ thể.
     * Chỉ những client có watchingAuctionId khớp mới nhận được.
     */
    public void broadcastToAuctionRoom(String message, String auctionId) {
        for (ClientHandler client : connectedClients) {
            if (auctionId != null && auctionId.equals(client.getWatchingAuctionId())) {
                client.sendMessage(message);
            }
        }
    }

    public void broadcastToLobby(String message) {
        for (ClientHandler client : connectedClients) {
            if (client.getWatchingAuctionId() == null) {
                client.sendMessage(message);
            }
        }
    }

    public void broadcastAuctionListUpdate() {
        broadcastService.broadcastLobbyUpdate(
                auctionService.getAuctionList(false)
        );
    }

    /**
     * Khởi động bộ giám sát phiên đấu giá: mỗi 1 giây quét tất cả phiên,
     * đóng những phiên đã hết giờ và broadcast kết quả cho client.
     */
    private void startAuctionMonitor() {
        auctionMonitor.scheduleAtFixedRate(() -> {
            try {
                var messages = auctionService.closeExpiredAuctions();

                for (String message : messages) {
                    broadcastService.broadcastAuctionClosedMessage(message);
                    broadcastService.broadcastLobbyUpdate(
                            auctionService.getAuctionList(false)
                    );
                    System.out.println("[AUCTION MONITOR] " + message);
                }
            } catch (Exception e) {
                System.err.println("[AUCTION MONITOR] Error while closing expired auctions: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public int getConnectedClientCount() {
        return connectedClients.size();
    }
}
