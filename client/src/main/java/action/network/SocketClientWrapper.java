package action.network;

// This wrapper class is introduced to make SocketClient more testable by allowing its instance to be mocked.
public class SocketClientWrapper {

    // Default constructor for regular use
    public SocketClientWrapper() {
    }

    // This method can be overridden in tests to return a mock SocketClient
    public SocketClient getActualSocketClientInstance() {
        return SocketClient.getInstance();
    }

    public void addListener(SocketListener listener) {
        getActualSocketClientInstance().addListener(listener);
    }

    public void requestData(String data) {
        getActualSocketClientInstance().requestData(data);
    }

    public void removeListener(SocketListener listener) {
        getActualSocketClientInstance().removeListener(listener);
    }
}