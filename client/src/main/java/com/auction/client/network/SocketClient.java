package com.auction.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClient {

    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedReader serverReader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter serverWriter = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader keyboardReader = new BufferedReader(
                        new InputStreamReader(System.in))
        ) {
            System.out.println("Connected to AuctionServer at " + SERVER_HOST + ":" + SERVER_PORT);

            String serverMessage;

            // Read initial messages from server
            for (int i = 0; i < 2; i++) {
                serverMessage = serverReader.readLine();
                if (serverMessage != null) {
                    System.out.println("Server: " + serverMessage);
                }
            }

            while (true) {
                System.out.print("Enter command: ");
                String userInput = keyboardReader.readLine();

                if (userInput == null || userInput.trim().isEmpty()) {
                    System.out.println("Command cannot be empty.");
                    continue;
                }

                serverWriter.println(userInput);

                serverMessage = serverReader.readLine();
                if (serverMessage == null) {
                    System.out.println("Server disconnected.");
                    break;
                }

                System.out.println("Server: " + serverMessage);

                if ("QUIT".equalsIgnoreCase(userInput.trim())) {
                    break;
                }
            }

            System.out.println("Client closed.");

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}