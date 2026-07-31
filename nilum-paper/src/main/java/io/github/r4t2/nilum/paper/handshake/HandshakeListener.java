package io.github.r4t2.nilum.paper.handshake;

import io.github.r4t2.nilum.common.config.NilumConfigManager;
import io.github.r4t2.nilum.common.config.TcpConfig;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.protocol.AssetManifestPacket;
import io.github.r4t2.nilum.common.protocol.HandshakeProtocol;
import io.github.r4t2.nilum.common.protocol.HelloAckPacket;
import io.github.r4t2.nilum.common.protocol.HelloPacket;
import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.github.r4t2.nilum.common.protocol.TcpOfferPacket;
import io.github.r4t2.nilum.common.tcp.NilumTcpAssetServer;
import io.github.r4t2.nilum.common.tcp.NilumTcpServer;
import io.github.r4t2.nilum.common.util.SemanticVersions;
import io.github.r4t2.nilum.paper.NilumPlugin;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerConnection;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Blocks a connecting player during configuration until they complete the Nilum handshake.
 */
public final class HandshakeListener implements Listener, PluginMessageListener {

    private static final long CHANNEL_POLL_INTERVAL_MILLIS = 50L;

    private final NilumPlugin plugin;
    private final NilumLogger logger;
    private final NilumConfigManager configManager;
    private final String serverModVersion;

    private final Map<UUID, CompletableFuture<HelloAckPacket>> pendingAcks = new ConcurrentHashMap<>();
    private final Map<UUID, HelloAckPacket> acknowledged = new ConcurrentHashMap<>();
    private final Map<UUID, Socket> tcpConnections = new ConcurrentHashMap<>();

    private volatile NilumTcpServer tcpServer;
    private volatile String tcpAdvertisedHost;
    private volatile int tcpPort;

    public HandshakeListener(NilumPlugin plugin, NilumLogger logger, NilumConfigManager configManager) {
        this.plugin = plugin;
        this.logger = logger;
        this.configManager = configManager;
        this.serverModVersion = plugin.getPluginMeta().getVersion();
        applyTcpConfig();
    }

    public boolean hasClient(UUID playerId) {
        return acknowledged.containsKey(playerId);
    }

    public HelloAckPacket capabilitiesOf(UUID playerId) {
        return acknowledged.get(playerId);
    }

    /**
     * Restarts the TCP side-channel to match the current config. Leaves it
     * disabled if {@code tcp.advertised-host} is blank.
     */
    public synchronized void applyTcpConfig() {
        if (tcpServer != null) {
            tcpServer.stop();
            tcpServer = null;
        }

        String advertisedHost = configManager.get(TcpConfig.ADVERTISED_HOST);
        if (advertisedHost.isBlank()) {
            logger.info("tcp.advertised-host is not set, TCP side-channel disabled (plugin-channel-only transfer).");
            return;
        }

        String bindAddress = configManager.get(TcpConfig.BIND_ADDRESS);
        int configuredPort = configManager.get(TcpConfig.PORT);

        NilumTcpServer server = new NilumTcpServer(this::onTcpConnected);
        try {
            int boundPort = server.start(bindAddress, configuredPort);
            tcpServer = server;
            tcpAdvertisedHost = advertisedHost;
            tcpPort = boundPort;
            logger.info("TCP side-channel listening on " + bindAddress + ":" + boundPort
                    + ", advertising " + advertisedHost + ":" + boundPort + " to clients.");
        } catch (IOException e) {
            logger.error("Failed to start TCP side-channel on " + bindAddress + ":" + configuredPort
                    + ", falling back to plugin-channel-only transfer", e);
        }
    }

    public void shutdown() {
        if (tcpServer != null) {
            tcpServer.stop();
        }
        for (Socket socket : tcpConnections.values()) {
            closeQuietly(socket);
        }
    }

    /** Async - blocks configuration until the handshake completes, times out, or the client disconnects. */
    @EventHandler
    public void onConfigure(AsyncPlayerConnectionConfigureEvent event) {
        PlayerConfigurationConnection connection = event.getConnection();
        UUID playerId = connection.getProfile().getId();
        long deadline = System.currentTimeMillis() + HandshakeProtocol.TIMEOUT_MILLIS;

        if (!awaitChannelRegistration(connection, deadline)) {
            connection.disconnect(Component.text(HandshakeProtocol.REQUIRES_MOD_MESSAGE));
            return;
        }

        CompletableFuture<HelloAckPacket> future = new CompletableFuture<>();
        pendingAcks.put(playerId, future);

        try {
            connection.sendPluginMessage(plugin, NilumChannels.HELLO_QUALIFIED,
                    new HelloPacket(HandshakeProtocol.PROTOCOL_VERSION, serverModVersion).encode());

            HelloAckPacket ack;
            try {
                ack = future.get(Math.max(1, deadline - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                connection.disconnect(Component.text(HandshakeProtocol.REQUIRES_MOD_MESSAGE));
                return;
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                connection.disconnect(Component.text(HandshakeProtocol.REQUIRES_MOD_MESSAGE));
                return;
            }

            if (SemanticVersions.isOlder(ack.modVersion(), serverModVersion)) {
                logger.warn(connection.getProfile().getName() + "'s Nilum version (" + ack.modVersion()
                        + ") is older than this server's (" + serverModVersion + "), kicking.");
                connection.disconnect(Component.text(HandshakeProtocol.tooOldMessage(ack.modVersion(), serverModVersion)));
                return;
            }

            acknowledged.put(playerId, ack);
            logger.info(connection.getProfile().getName() + " completed the Nilum handshake (loader=" + ack.loader()
                    + ", version=" + ack.modVersion() + ").");
        } finally {
            pendingAcks.remove(playerId);
        }
    }

    /** Paper drops outgoing plugin messages until the client's channel registration arrives, so wait for it. */
    private boolean awaitChannelRegistration(PlayerConfigurationConnection connection, long deadline) {
        while (!connection.getListeningPluginChannels().contains(NilumChannels.HELLO_QUALIFIED)) {
            if (!connection.isConnected() || System.currentTimeMillis() >= deadline) {
                return false;
            }
            try {
                Thread.sleep(CHANNEL_POLL_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    /** The player is guaranteed to have completed the handshake by now. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        NilumTcpServer server = tcpServer;
        if (server != null) {
            String token = server.offerConnection(playerId);
            player.sendPluginMessage(plugin, NilumChannels.TCP_OFFER_QUALIFIED,
                    new TcpOfferPacket(tcpAdvertisedHost, tcpPort, token).encode());
        }

        plugin.modelDisplays().sendAllTo(player);

        player.sendPluginMessage(plugin, NilumChannels.ASSET_MANIFEST_QUALIFIED,
                new AssetManifestPacket(plugin.models().manifest()).encode());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        acknowledged.remove(playerId);
        if (tcpServer != null) {
            tcpServer.cancelOffer(playerId);
        }
        Socket socket = tcpConnections.remove(playerId);
        if (socket != null) {
            closeQuietly(socket);
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        // Handled uniformly via the PlayerConnection overload below, which fires
        // for both configuring and joined players.
    }

    @Override
    public void onPluginMessageReceived(String channel, PlayerConnection connection, byte[] message) {
        if (NilumChannels.HELLO_ACK_QUALIFIED.equals(channel)) {
            onHelloAck(connection, message);
        } else if (NilumChannels.TCP_UNAVAILABLE_QUALIFIED.equals(channel)
                && connection instanceof PlayerGameConnection gameConnection) {
            onTcpUnavailable(gameConnection.getPlayer());
        }
    }

    private void onHelloAck(PlayerConnection connection, byte[] message) {
        UUID playerId = uuidOf(connection);
        if (playerId == null) {
            return;
        }

        CompletableFuture<HelloAckPacket> future = pendingAcks.get(playerId);
        if (future == null) {
            return;
        }
        future.complete(HelloAckPacket.decode(message));
    }

    private static UUID uuidOf(PlayerConnection connection) {
        if (connection instanceof PlayerConfigurationConnection configuration) {
            return configuration.getProfile().getId();
        }
        if (connection instanceof PlayerGameConnection game) {
            return game.getPlayer().getUniqueId();
        }
        return null;
    }

    private void onTcpUnavailable(Player player) {
        logger.warn(player.getName() + "'s client couldn't reach the TCP side-channel, "
                + "falling back to plugin-channel transfer for this session.");
    }

    private void onTcpConnected(UUID playerId, Socket socket) {
        tcpConnections.put(playerId, socket);
        logger.info("TCP side-channel connected for " + playerId + ", serving asset requests.");
        NilumTcpAssetServer.serve(socket, id -> plugin.models().rawBytes(id).orElse(null));
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
