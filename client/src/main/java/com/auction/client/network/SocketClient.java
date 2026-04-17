package com.auction.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClient {

    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 88;

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

            Thread listenerThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = serverReader.readLine()) != null) {
                        System.out.println("\nServer: " + serverMessage);
                        System.out.print("Enter command: ");
                    }
                } catch (IOException e) {
                    System.out.println("\nDisconnected from server.");
                }
            });

            listenerThread.setDaemon(true);
            listenerThread.start();

            while (true) {
                System.out.print("Enter command: ");
                String userInput = keyboardReader.readLine();

                if (userInput == null) {
                    break;
                }

                userInput = userInput.trim();
                if (userInput.isEmpty()) {
                    System.out.println("Command cannot be empty.");
                    continue;
                }

                serverWriter.println(userInput);

                if ("QUIT".equalsIgnoreCase(userInput)) {
                    break;
                }
            }

            System.out.println("Client closed.");

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}