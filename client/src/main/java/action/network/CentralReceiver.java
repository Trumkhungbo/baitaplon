package action.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class CentralReceiver extends Thread {
    private Socket socket;
    private BufferedReader in;
    private List<SocketListener> listeners = new ArrayList<>();

    public CentralReceiver(Socket socket) {
        this.socket = socket;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addListener(SocketListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SocketListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void run() {
        try {
            String data;
            while ((data = in.readLine()) != null) {
                for (SocketListener listener : listeners) {
                    listener.onDataReceived(data);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
