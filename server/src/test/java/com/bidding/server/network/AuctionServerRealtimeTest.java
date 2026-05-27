package com.bidding.server.network;

import com.bidding.server.database.DatabaseInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionServerRealtimeTest {

    private AuctionServer server;
    private Thread serverThread;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (server != null) {
            server.stop();
        }
        if (serverThread != null) {
            serverThread.join(2_000);
        }
    }

    @Test
    void shouldBroadcastBidAndAuctionCloseToRoomAndLobby() throws Exception {
        int port = findFreePort();
        DatabaseInitializer.initialize();
        DatabaseInitializer.resetAuctionRuntimeData();
        String suffix = String.valueOf(System.nanoTime());
        String sellerUsername = "seller-live-" + suffix;
        String bidderUsername = "bidder-live-" + suffix;
        String watcher1Username = "watcher-1-" + suffix;
        String watcher2Username = "watcher-2-" + suffix;
        server = new AuctionServer(port);
        serverThread = new Thread(server::start, "auction-server-realtime-test");
        serverThread.setDaemon(true);
        serverThread.start();
        waitForServer(port);

        try (AsyncClient seller = new AsyncClient("127.0.0.1", port);
             AsyncClient bidder = new AsyncClient("127.0.0.1", port);
             AsyncClient watcher1 = new AsyncClient("127.0.0.1", port);
             AsyncClient watcher2 = new AsyncClient("127.0.0.1", port);
            AsyncClient lobby = new AsyncClient("127.0.0.1", port)) {

            seller.send("REGISTER|" + sellerUsername + "|pw");
            assertJsonSuccess(seller.awaitMessageContaining("\"command\":\"REGISTER_RESULT\"", 3_000));
            seller.send("LOGIN|" + sellerUsername + "|pw");
            assertJsonSuccess(seller.awaitMessageContaining("\"command\":\"LOGIN_RESULT\"", 3_000));

            bidder.send("REGISTER|" + bidderUsername + "|pw");
            assertJsonSuccess(bidder.awaitMessageContaining("\"command\":\"REGISTER_RESULT\"", 3_000));
            bidder.send("LOGIN|" + bidderUsername + "|pw");
            assertJsonSuccess(bidder.awaitMessageContaining("\"command\":\"LOGIN_RESULT\"", 3_000));

            watcher1.send("REGISTER|" + watcher1Username + "|pw");
            assertJsonSuccess(watcher1.awaitMessageContaining("\"command\":\"REGISTER_RESULT\"", 3_000));
            watcher1.send("LOGIN|" + watcher1Username + "|pw");
            assertJsonSuccess(watcher1.awaitMessageContaining("\"command\":\"LOGIN_RESULT\"", 3_000));

            watcher2.send("REGISTER|" + watcher2Username + "|pw");
            assertJsonSuccess(watcher2.awaitMessageContaining("\"command\":\"REGISTER_RESULT\"", 3_000));
            watcher2.send("LOGIN|" + watcher2Username + "|pw");
            assertJsonSuccess(watcher2.awaitMessageContaining("\"command\":\"LOGIN_RESULT\"", 3_000));

            lobby.send("LIST_AUCTIONS");
            assertTrue(lobby.awaitMessage("AUCTION_LIST|", 3_000).startsWith("AUCTION_LIST|"));

            seller.send("ADD_AUCTION|" + sellerUsername + "|Realtime Vase|5000");
            String addResponse = seller.awaitMessage("ADD_AUCTION_SUCCESS|", 3_000);
            String auctionId = extractField(addResponse, "id");
            assertTrue(lobby.awaitMessageContaining("Realtime Vase", 3_000).contains("Realtime Vase"));

            watcher1.send("WATCH|" + auctionId);
            assertTrue(watcher1.awaitMessage("WATCHING|", 3_000).startsWith("WATCHING|"));
            watcher2.send("WATCH|" + auctionId);
            assertTrue(watcher2.awaitMessage("WATCHING|", 3_000).startsWith("WATCHING|"));

            bidder.send("BID|" + auctionId + "|6500");
            assertTrue(bidder.awaitMessage("BID_RESULT|status=SUCCESS", 3_000).startsWith("BID_RESULT|status=SUCCESS"));

            String roomUpdate1 = watcher1.awaitMessage("BID_UPDATE|", 3_000);
            String roomUpdate2 = watcher2.awaitMessage("BID_UPDATE|", 3_000);
            String lobbyUpdate = lobby.awaitMessageContaining("Realtime Vase", 3_000);

            assertTrue(roomUpdate1.contains("auctionId=" + auctionId));
            assertTrue(roomUpdate1.contains("highestBid=6500"));
            assertTrue(roomUpdate1.contains("bidder=" + bidderUsername));
            assertTrue(roomUpdate2.contains("auctionId=" + auctionId));
            assertTrue(lobbyUpdate.contains(auctionId + ":Realtime Vase:6500:RUNNING"));

            seller.send("CLOSE_AUCTION|" + auctionId);
            assertTrue(seller.awaitMessage("CLOSE_AUCTION_SUCCESS|", 3_000).startsWith("CLOSE_AUCTION_SUCCESS|"));

            String closed1 = watcher1.awaitMessage("AUCTION_CLOSED|", 3_000);
            String closed2 = watcher2.awaitMessage("AUCTION_CLOSED|", 3_000);
            String lobbyClosed = lobby.awaitMessageContaining("Realtime Vase", 3_000);

            assertTrue(closed1.contains("auctionId=" + auctionId));
            assertTrue(closed1.contains("winner=" + bidderUsername));
            assertTrue(closed2.contains("auctionId=" + auctionId));
            assertTrue(lobbyClosed.contains(auctionId + ":Realtime Vase:6500:FINISHED"));
        }
    }

    private static String extractField(String message, String key) {
        for (String part : message.split("\\|")) {
            if (part.startsWith(key + "=")) {
                return part.substring((key + "=").length());
            }
        }
        throw new AssertionError("Field not found: " + key + " in " + message);
    }

    private static void assertJsonSuccess(String message) {
        assertTrue(message.contains("\"status\":\"SUCCESS\""), message);
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void waitForServer(int port) throws Exception {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            try (Socket ignored = new Socket("127.0.0.1", port)) {
                return;
            } catch (IOException ignored) {
                Thread.sleep(100);
            }
        }
        throw new AssertionError("Server did not start listening in time");
    }

    private static final class AsyncClient implements Closeable {
        private final Socket socket;
        private final PrintWriter writer;
        private final BufferedReader reader;
        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        private final List<Throwable> readerErrors = new CopyOnWriteArrayList<>();
        private final Thread readerThread;

        private AsyncClient(String host, int port) throws Exception {
            socket = new Socket(host, port);
            socket.setSoTimeout(500);
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            readerThread = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            String line = reader.readLine();
                            if (line == null) {
                                return;
                            }
                            messages.offer(line);
                        } catch (SocketTimeoutException ignored) {
                        }
                    }
                } catch (IOException e) {
                    readerErrors.add(e);
                }
            }, "async-client-reader");
            readerThread.setDaemon(true);
            readerThread.start();

            awaitMessageContaining("\"command\":\"INFO\"", 3_000);
        }

        private void send(String request) {
            writer.println(request);
        }

        private String awaitMessage(String prefix, long timeoutMs) throws Exception {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                String message = messages.poll();
                if (message == null) {
                    Thread.sleep(20);
                    continue;
                }
                if (message.startsWith(prefix)) {
                    return message;
                }
            }
            throw new AssertionError("Timed out waiting for prefix " + prefix + ", queued=" + messages + ", errors=" + readerErrors);
        }

        private String awaitMessageContaining(String token, long timeoutMs) throws Exception {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                String message = messages.poll();
                if (message == null) {
                    Thread.sleep(20);
                    continue;
                }
                if (message.contains(token)) {
                    return message;
                }
            }
            throw new AssertionError("Timed out waiting for token " + token + ", queued=" + messages + ", errors=" + readerErrors);
        }

        @Override
        public void close() throws IOException {
            readerThread.interrupt();
            writer.close();
            reader.close();
            socket.close();
        }
    }
}
