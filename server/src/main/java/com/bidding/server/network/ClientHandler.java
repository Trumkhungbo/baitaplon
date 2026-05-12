package com.bidding.server.network;

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

            sendMessage("{\"CONNECTED\"|\"Welcome to the Auction Server\"}");
            sendMessage("{\"INFO\"|\"Supported commands: PING, LOGIN|user|pass, LIST_AUCTIONS, GET_AUCTION_DETAIL|auctionId, ADD_AUCTION|seller|itemName|startPrice, WATCH|auctionId, BID|auctionId|user|amount, QUIT\"}");

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
        reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(
                new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    private void handleRequest(String request) {
        if (request.isEmpty()) {
            sendMessage("ERROR|Empty request");
            return;
        }

        String[] parts = null;
        String command = "";

        try {
            // Cố gắng đọc theo chuẩn JSON trước
            JsonObject json = JsonParser.parseString(request).getAsJsonObject();
            command = json.get("command").getAsString().toUpperCase();

            if (command.equals("GET_ACCOUNTINFORMATION")) {
                parts = new String[]{"GET_ACCOUNTINFORMATION", json.get("username").getAsString()};
            } else if (command.equals("ADD_MONEY")) {
                parts = new String[]{"ADD_MONEY", json.get("username").getAsString(), json.get("money").getAsString()};
            } else if (command.equals("LOGIN")) {
                parts = new String[]{
                        "LOGIN",
                        json.has("username") ? json.get("username").getAsString() : "",
                        json.has("password") ? json.get("password").getAsString() : ""
                };

            } else if (command.equals("REGISTER")) {
                parts = new String[]{
                        "REGISTER",
                        json.has("username") ? json.get("username").getAsString() : "",
                        json.has("password") ? json.get("password").getAsString() : "",
                        json.has("phone") ? json.get("phone").getAsString() : "",
                        json.has("email") ? json.get("email").getAsString() : "",
                        json.has("personalID") ? json.get("personalID").getAsString() : ""
                };

            } else if (command.equals("FORGOT_PASSWORD")) {
                parts = new String[]{
                        "FORGOT_PASSWORD",
                        json.has("username") ? json.get("username").getAsString() : "",
                        json.has("phone") ? json.get("phone").getAsString() : "",
                        json.has("personalID") ? json.get("personalID").getAsString() : ""
                };

            } else {
                parts = new String[]{command};
            }
        }  catch (Exception e) {
            //Không được về chia "|"
            parts = request.split("\\|");
            command = parts[0].toUpperCase();
        }
        // Giao cho Dispatcher xử lý
        commandDispatcher.dispatch(command, parts, this);
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

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean isAdmin() {
        return "admin".equals(currentUser);
    }
}
