package action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketClient {
    private Socket socket;
    private BufferedReader serverReader;
    private PrintWriter serverWriter;
    private ServerListener listener;

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
            System.out.println("Login to Server!");

            Thread listenerThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = serverReader.readLine()) != null) {
                        System.out.println("Signal received: " + serverMessage);
                        if (listener != null) {
                            listener.onMessageReceived(serverMessage);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.start();

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }


    public void sendMessage(String message) {
        if (serverWriter != null) {
            serverWriter.println(message);
        }
    }
}