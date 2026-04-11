package com.auction.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClient {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean isRunning;

    // 1. Tạo "Chiếc loa" báo cáo cho Giao diện
    public interface ServerListener {
        void onLoginResult(boolean isSuccess, String message);
    }
    private ServerListener listener;

    public void setServerListener(ServerListener listener) {
        this.listener = listener;
    }

    // 2. Khởi tạo kết nối và bật luồng ngầm
    public void connect() {
        try {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            isRunning = true;
            System.out.println("[CLIENT] Đã kết nối với Server.");

            new Thread(this::listenToServer).start(); // Luồng ngầm nghe ngóng
        } catch (IOException e) {
            System.err.println("[CLIENT] Không thể kết nối Server.");
        }
    }

    // 3. Hàm cho giao diện gọi để đẩy dữ liệu đi
    public void sendLogin(String username, String password) {
        if (writer != null) {
            writer.println("LOGIN|" + username + "|" + password);
        }
    }

    // 4. Luồng ngầm: Bóc tách chuỗi nhận về
    private void listenToServer() {
        try {
            String response;
            while (isRunning && (response = reader.readLine()) != null) {
                if (listener != null) {
                    String[] parts = response.split("\\|");
                    if (parts[0].equals("LOGIN_SUCCESS")) {
                        listener.onLoginResult(true, parts[1]);
                    } else if (parts[0].equals("LOGIN_FAILED")) {
                        listener.onLoginResult(false, parts[1]);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Mất kết nối.");
        }
    }
}