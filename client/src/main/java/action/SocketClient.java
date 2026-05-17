package action;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SocketClient {
    private Socket socket;
    private BufferedReader serverReader;
    private PrintWriter serverWriter;


    private List<SocketListener> listeners = new CopyOnWriteArrayList<>();

    private static SocketClient instance;

    private SocketClient() {}

    public static synchronized SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    public void addListener(SocketListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(SocketListener listener) {
        listeners.remove(listener);
    }

    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            serverWriter = new PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            System.out.println("Connected to Server!");

            Thread listenerThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = serverReader.readLine()) != null) {
                        System.out.println("Signal received: " + serverMessage);
                        for (SocketListener listener : listeners) {
                            listener.onDataReceived(serverMessage);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.start();

        } catch (IOException e) {
            System.err.println("Connection Error: " + e.getMessage());
        }
    }

    public void requestData(String message) {
        if (serverWriter != null) {
            serverWriter.println(message);
        }
    }
}