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
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends {@code nilum:hello} on join and kicks players who don't respond with
 * {@code nilum:hello_ack} within the handshake timeout.
 */
public final class HandshakeListener implements Listener, PluginMessageListener {

    private static final long TIMEOUT_TICKS = HandshakeProtocol.TIMEOUT_MILLIS / 50L;

    private final NilumPlugin plugin;
    private final NilumLogger logger;
    private final NilumConfigManager configManager;
    private final String serverModVersion;

    private final Map<UUID, BukkitTask> pendingHandshakes = new ConcurrentHashMap<>();
    private final Set<UUID> helloSent = ConcurrentHashMap.newKeySet();
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

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        BukkitTask kickTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingHandshakes.remove(playerId);
            helloSent.remove(playerId);
            if (player.isOnline()) {
                logger.warn(player.getName() + " didn't respond to the handshake in time, kicking.");
                player.kick(Component.text(HandshakeProtocol.REQUIRES_MOD_MESSAGE));
            }
        }, TIMEOUT_TICKS);
        pendingHandshakes.put(playerId, kickTask);

        boolean alreadyRegistered = player.getListeningPluginChannels().contains(NilumChannels.HELLO_QUALIFIED);
        logger.debug(player.getName() + " joined; nilum:hello already registered=" + alreadyRegistered
                + "; known channels=" + player.getListeningPluginChannels());
        if (alreadyRegistered) {
            sendHello(player);
        }
    }

    /**
     * Paper drops outgoing plugin messages sent before the client's own
     * channel registration arrives, and that can still be in flight right at
     * join - wait for it instead of racing it.
     */
    @EventHandler
    public void onChannelRegister(PlayerRegisterChannelEvent event) {
        logger.debug(event.getPlayer().getName() + " registered channel " + event.getChannel()
                + "; pending=" + pendingHandshakes.containsKey(event.getPlayer().getUniqueId()));
        if (NilumChannels.HELLO_QUALIFIED.equals(event.getChannel())
                && pendingHandshakes.containsKey(event.getPlayer().getUniqueId())) {
            sendHello(event.getPlayer());
        }
    }

    private void sendHello(Player player) {
        if (helloSent.add(player.getUniqueId())) {
            logger.debug("Sending nilum:hello to " + player.getName() + ".");
            player.sendPluginMessage(plugin, NilumChannels.HELLO_QUALIFIED,
                    new HelloPacket(HandshakeProtocol.PROTOCOL_VERSION, serverModVersion).encode());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        BukkitTask pending = pendingHandshakes.remove(playerId);
        if (pending != null) {
            pending.cancel();
        }
        helloSent.remove(playerId);
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
        logger.debug("Received plugin message from " + player.getName() + " on channel " + channel + ".");
        if (NilumChannels.HELLO_ACK_QUALIFIED.equals(channel)) {
            onHelloAck(player, message);
        } else if (NilumChannels.TCP_UNAVAILABLE_QUALIFIED.equals(channel)) {
            onTcpUnavailable(player);
        }
    }

    private void onHelloAck(Player player, byte[] message) {
        UUID playerId = player.getUniqueId();
        BukkitTask pending = pendingHandshakes.remove(playerId);
        if (pending != null) {
            pending.cancel();
        }

        HelloAckPacket ack = HelloAckPacket.decode(message);

        if (SemanticVersions.isOlder(ack.modVersion(), serverModVersion)) {
            logger.warn(player.getName() + "'s Nilum version (" + ack.modVersion()
                    + ") is older than this server's (" + serverModVersion + "), kicking.");
            player.kick(Component.text(HandshakeProtocol.tooOldMessage(ack.modVersion(), serverModVersion)));
            return;
        }

        acknowledged.put(playerId, ack);
        logger.info(player.getName() + " completed the Nilum handshake (loader=" + ack.loader()
                + ", version=" + ack.modVersion() + ").");

        NilumTcpServer server = tcpServer;
        if (server != null) {
            String token = server.offerConnection(playerId);
            byte[] tcpOfferBytes = new TcpOfferPacket(tcpAdvertisedHost, tcpPort, token).encode();
            logger.debug("Sending nilum:tcp_offer to " + player.getName() + " (" + tcpOfferBytes.length + " bytes).");
            player.sendPluginMessage(plugin, NilumChannels.TCP_OFFER_QUALIFIED, tcpOfferBytes);
            logger.debug("Sent nilum:tcp_offer to " + player.getName() + ".");
        }

        logger.debug("Broadcasting model displays to " + player.getName() + ".");
        plugin.modelDisplays().sendAllTo(player);
        logger.debug("Broadcast model displays to " + player.getName() + ".");

        byte[] manifestBytes = new AssetManifestPacket(plugin.models().manifest()).encode();
        logger.debug("Sending nilum:asset_manifest to " + player.getName() + " (" + manifestBytes.length + " bytes).");
        player.sendPluginMessage(plugin, NilumChannels.ASSET_MANIFEST_QUALIFIED, manifestBytes);
        logger.debug("Sent nilum:asset_manifest to " + player.getName() + ".");
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
