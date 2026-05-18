package com.bidding.server.network;

import com.bidding.common.enums.UserRole;
import com.bidding.server.network.command.CommandDispatcher;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final AuctionServer server;
    private final CommandDispatcher commandDispatcher;

    private BufferedReader reader;
    private PrintWriter writer;

    private volatile boolean connected;
    private String currentUser;
    private UserRole currentRole;
    private String watchingAuctionId;

    public ClientHandler(Socket clientSocket, AuctionServer server, CommandDispatcher commandDispatcher) {
        this.clientSocket = clientSocket;
        this.server = server;
        this.commandDispatcher = commandDispatcher;
        this.connected = true;
    }

    @Override
    public void run() {
        try {
            initStreams();

            sendMessage("CONNECTED|Welcome to the Auction Server");
            sendMessage("INFO|Supported commands: PING, LOGIN|user|pass, LIST_AUCTIONS, GET_AUCTION_DETAIL|auctionId, ADD_AUCTION|seller|itemName|startPrice, WATCH|auctionId, BID|auctionId|amount, QUIT");

            String clientMessage;
            while (connected && (clientMessage = reader.readLine()) != null) {
                System.out.println("[REQUEST] " + clientMessage);
                handleRequest(clientMessage.trim());
            }
        } catch (IOException e) {
            System.err.println("ClientHandler error: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void initStreams() throws IOException {
        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    private void handleRequest(String request) {
        if (request.isEmpty()) {
            sendMessage("ERROR|Empty request");
            return;
        }

        String[] parts;
        String command;

        try {
            JsonObject json = JsonParser.parseString(request).getAsJsonObject();
            command = json.get("command").getAsString().toUpperCase();

            parts = buildJsonCommandParts(command, json);
        } catch (Exception e) {
            parts = request.split("\\|");
            command = parts[0].toUpperCase();
        }

        commandDispatcher.dispatch(command, parts, this);
    }

    private String[] buildJsonCommandParts(String command, JsonObject json) {
        return switch (command) {
            case "LOGIN" -> new String[]{
                    "LOGIN",
                    getJsonString(json, "username"),
                    getJsonString(json, "password")
            };
            case "REGISTER" -> new String[]{
                    "REGISTER",
                    getJsonString(json, "username"),
                    getJsonString(json, "password"),
                    getJsonString(json, "phone"),
                    getJsonString(json, "email"),
                    getJsonString(json, "personalID")
            };
            case "GET_ACCOUNTINFORMATION" -> new String[]{
                    "GET_ACCOUNTINFORMATION",
                    getJsonString(json, "username")
            };
            case "ADD_MONEY" -> new String[]{
                    "ADD_MONEY",
                    getJsonString(json, "username"),
                    getJsonString(json, "money")
            };
            case "FORGOT_PASSWORD" -> new String[]{
                    "FORGOT_PASSWORD",
                    getJsonString(json, "username"),
                    getJsonString(json, "phone"),
                    getJsonString(json, "personalID")
            };
            case "RESET_PASSWORD" -> new String[]{
                    "RESET_PASSWORD",
                    getJsonString(json, "username"),
                    getJsonString(json, "newPassword")
            };
            case "GET_AUCTION_DETAIL", "GET_BID_HISTORY", "GET_WINNER", "WATCH", "CLOSE_AUCTION", "APPROVE_AUCTION" -> new String[]{
                    command,
                    getJsonString(json, "auctionId")
            };
            case "BID" -> new String[]{
                    "BID",
                    getJsonString(json, "auctionId"),
                    getJsonString(json, "amount")
            };
            case "SET_AUTO_BID" -> new String[]{
                    "SET_AUTO_BID",
                    getJsonString(json, "auctionId"),
                    getJsonString(json, "maxBid"),
                    getJsonString(json, "increment")
            };
            case "UPDATE_STATUS" -> new String[]{
                    "UPDATE_STATUS",
                    getJsonString(json, "auctionId"),
                    getJsonString(json, "status")
            };
            case "ADD_AUCTION" -> buildJsonAddAuctionParts(json);
            case "UPLOAD_IMAGE" -> new String[]{
                    "UPLOAD_IMAGE",
                    getJsonString(json, "extension"),
                    getJsonString(json, "data")
            };
            case "GET_IMAGE" -> new String[]{
                    "GET_IMAGE",
                    getJsonString(json, "filename")
            };
            default -> new String[]{command};
        };
    }

    private String[] buildJsonAddAuctionParts(JsonObject json) {
        return new String[]{
                "ADD_AUCTION",
                firstJsonString(json, "sellerUsername", "seller", "username"),
                getJsonString(json, "itemType"),
                firstJsonString(json, "itemName", "name"),
                firstJsonString(json, "des1", "param1", "brand", "engineType", "artist"),
                firstJsonString(json, "des2", "param2", "warrantyMonths", "mileage", "creationYear"),
                firstJsonString(json, "price", "startPrice", "startingPrice"),
                getJsonString(json, "startTime"),
                getJsonString(json, "durationMinutes"),
                getJsonString(json, "imageUrl")
        };
    }

    private String firstJsonString(JsonObject json, String... keys) {
        for (String key : keys) {
            String value = getJsonString(json, key);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String getJsonString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? json.get(key).getAsString()
                : "";
    }

    public synchronized void sendMessage(String message) {
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

    public void disconnect() {
        connected = false;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }

    public String getWatchingAuctionId() {
        return watchingAuctionId;
    }

    public void setWatchingAuctionId(String watchingAuctionId) {
        this.watchingAuctionId = watchingAuctionId;
    }

    public UserRole getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(UserRole currentRole) {
        this.currentRole = currentRole;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        // Kiem tra role thuc su tu DB, khong dua vao ten username
        return currentRole == UserRole.ADMIN;
    }
}