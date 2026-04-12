package com.auction.server.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import com.auction.server.service.AuthService;
import com.auction.server.service.AuctionService;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final AuctionServer server;
    private final AuthService authService;
    private final AuctionService auctionService;

    private BufferedReader reader;
    private PrintWriter writer;

    private volatile boolean connected;
    private String username;
    private String watchingAuctionId;

    public ClientHandler(Socket clientSocket, AuctionServer server) {
        this.clientSocket = clientSocket;
        this.server = server;
        this.connected = true;
        this.authService = new AuthService();
        this.auctionService = new AuctionService();
    }

    @Override
    public void run() {
        try {
            initStreams();

            sendMessage("CONNECTED|Welcome to the Auction Server");
            sendMessage("INFO|Supported commands: PING, LOGIN|user|pass, LIST_AUCTIONS, WATCH|auctionId, BID|auctionId|user|amount, QUIT");

            String clientMessage;
            while (connected && (clientMessage = reader.readLine()) != null) {
                System.out.println("Received from client: " + clientMessage);
                handleRequest(clientMessage.trim());
            }
        } catch (IOException e) {
            System.err.println("ClientHandler error: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void initStreams() throws IOException {
        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        writer = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    private void handleRequest(String request) {
        if (request.isEmpty()) {
            sendMessage("ERROR|Empty request");
            return;
        }

        String[] parts = request.split("\\|");
        String command = parts[0].toUpperCase();

        switch (command) {
            case "PING":
                sendMessage("PONG");
                break;

            case "LOGIN":
                handleLogin(parts);
                break;

            case "LIST_AUCTIONS":
                handleListAuctions();
                break;

            case "WATCH":
                handleWatchAuction(parts);
                break;

            case "BID":
                handlePlaceBid(parts);
                break;

            case "QUIT":
                sendMessage("BYE|Disconnected from server");
                connected = false;
                break;

            default:
                sendMessage("ERROR|Invalid command: " + command);
        }
    }

    private void handleLogin(String[] parts) {
        if (parts.length < 3) {
            sendMessage("ERROR|Invalid syntax. Use: LOGIN|username|password");
            return;
        }

        String inputUsername = parts[1];
        String inputPassword = parts[2];

        String response = authService.login(inputUsername, inputPassword);

        if (response.startsWith("LOGIN_SUCCESS")) {
            this.username = inputUsername;
        }

        sendMessage(response);
    }

    private void handleListAuctions() {
        sendMessage(auctionService.getAuctionList());
    }

    private void handleWatchAuction(String[] parts) {
        if (parts.length < 2) {
            sendMessage("ERROR|Invalid syntax. Use: WATCH|auctionId");
            return;
        }

        this.watchingAuctionId = parts[1];
        sendMessage("WATCHING|You are now watching auction " + watchingAuctionId);
    }

    private void handlePlaceBid(String[] parts) {
        if (parts.length < 4) {
            sendMessage("ERROR|Invalid syntax. Use: BID|auctionId|username|amount");
            return;
        }

        String auctionId = parts[1];
        String bidUsername = parts[2];
        String amountText = parts[3];

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            sendMessage("ERROR|Bid amount must be a number");
            return;
        }

        if (amount <= 0) {
            sendMessage("ERROR|Bid amount must be greater than 0");
            return;
        }

        String response = auctionService.placeBid(auctionId, bidUsername, amount);
        sendMessage(response);

        if (response.startsWith("BID_SUCCESS")) {
            server.broadcastToAuctionRoom(
                    "BID_UPDATE|auctionId=" + auctionId + "|highestBid=" + amount + "|bidder=" + bidUsername,
                    auctionId
            );
        }
    }

    public void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    public void closeConnection() {
        connected = false;

        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            System.err.println("Error while closing reader: " + e.getMessage());
        }

        if (writer != null) {
            writer.close();
        }

        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error while closing socket: " + e.getMessage());
        }

        server.removeClient(this);
    }

    public String getUsername() {
        return username;
    }

    public String getWatchingAuctionId() {
        return watchingAuctionId;
    }
}
