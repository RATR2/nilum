package io.github.r4t2.nilum.common.tcp;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Server side of the TCP side-channel: */
public final class NilumTcpServer {

    private static final int HANDSHAKE_READ_TIMEOUT_MILLIS = 5000;
    private static final Logger LOGGER = Logger.getLogger("nilum");

    private final Map<String, UUID> pendingTokens = new ConcurrentHashMap<>();
    private final BiConsumer<UUID, Socket> onConnected;
    private final ExecutorService connectionHandlers = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "nilum-tcp-connection");
        thread.setDaemon(true);
        return thread;
    });

    private ServerSocket serverSocket;
    private Thread acceptThread;

    public NilumTcpServer(BiConsumer<UUID, Socket> onConnected) {
        this.onConnected = onConnected;
    }

    public int start(String bindAddress, int port) throws IOException {
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(bindAddress, port));
        acceptThread = new Thread(this::acceptLoop, "nilum-tcp-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        return serverSocket.getLocalPort();
    }

    public void stop() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        if (acceptThread != null) {
            acceptThread.interrupt();
        }
        connectionHandlers.shutdownNow();
    }

    /** Generates a fresh token and registers it as the expected credential for {@code playerId}. */
    public String offerConnection(UUID playerId) {
        String token = UUID.randomUUID().toString();
        pendingTokens.put(token, playerId);
        return token;
    }

    /** Withdraws any outstanding offer(s) for this player, e.g. on disconnect. */
    public void cancelOffer(UUID playerId) {
        pendingTokens.values().removeIf(playerId::equals);
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                connectionHandlers.submit(() -> handleConnection(socket));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    LOGGER.log(Level.WARNING, "Nilum TCP accept loop error", e);
                }
            }
        }
    }

    private void handleConnection(Socket socket) {
        try {
            socket.setSoTimeout(HANDSHAKE_READ_TIMEOUT_MILLIS);
            String token = new DataInputStream(socket.getInputStream()).readUTF();
            UUID playerId = pendingTokens.remove(token);
            if (playerId == null) {
                closeQuietly(socket);
                return;
            }
            socket.setSoTimeout(0);
            onConnected.accept(playerId, socket);
        } catch (IOException e) {
            closeQuietly(socket);
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
