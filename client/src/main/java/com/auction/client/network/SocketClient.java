package com.auction.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClient {
    private Socket socket;
    private BufferedReader serverReader;
    private PrintWriter serverWriter;
    private ServerListener listener; // Cái chuông báo cho Giao diện

    // Interface để giao diện "lắng nghe" mạng
    public interface ServerListener {
        void onMessageReceived(String message);
    }

    public void setServerListener(ServerListener listener) {
        this.listener = listener;
    }

    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            serverWriter = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Đã kết nối tới Server!");

            // Tạo luồng ngầm chuyên túc trực nghe ngóng Server
            Thread listenerThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = serverReader.readLine()) != null) {
                        System.out.println("Mạng nhận được: " + serverMessage);
                        if (listener != null) {
                            listener.onMessageReceived(serverMessage); // Rung chuông báo giao diện
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Đã ngắt kết nối khỏi Server.");
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.start();

        } catch (IOException e) {
            System.err.println("Lỗi kết nối mạng: " + e.getMessage());
        }
    }

    // Hàm để giao diện ném lệnh (LOGIN, BID...) xuống mạng
    public void sendMessage(String message) {
        if (serverWriter != null) {
            serverWriter.println(message);
        }
    }
}