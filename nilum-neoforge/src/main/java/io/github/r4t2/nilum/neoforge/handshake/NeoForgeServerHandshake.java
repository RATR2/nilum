package io.github.r4t2.nilum.neoforge.handshake;

import io.github.r4t2.nilum.common.config.NilumConfigManager;
import io.github.r4t2.nilum.common.config.TcpConfig;
import io.github.r4t2.nilum.common.hosting.NilumAssetHost;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.protocol.AssetManifestPacket;
import io.github.r4t2.nilum.common.protocol.HandshakeProtocol;
import io.github.r4t2.nilum.common.protocol.HelloAckPacket;
import io.github.r4t2.nilum.common.protocol.HelloPacket;
import io.github.r4t2.nilum.common.protocol.TcpOfferPacket;
import io.github.r4t2.nilum.common.tcp.NilumTcpAssetServer;
import io.github.r4t2.nilum.common.tcp.NilumTcpServer;
import io.github.r4t2.nilum.common.util.SemanticVersions;
import io.github.r4t2.nilum.neoforge.network.NilumAssetManifestPayload;
import io.github.r4t2.nilum.neoforge.network.NilumHelloAckPayload;
import io.github.r4t2.nilum.neoforge.network.NilumTcpOfferPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs only on a NeoForge dedicated server, and handles the handshake between server and client.
 */
public final class NeoForgeServerHandshake {

    private static final long TIMEOUT_TICKS = HandshakeProtocol.TIMEOUT_MILLIS / 50L;

    private final NilumConfigManager configManager;
    private final NilumLogger logger;
    private final String serverModVersion;
    private final NilumAssetHost assetHost;

    private final Map<UUID, PendingHandshake> pendingHandshakes = new ConcurrentHashMap<>();
    private final Map<UUID, HelloAckPacket> acknowledged = new ConcurrentHashMap<>();
    private final Map<UUID, Socket> tcpConnections = new ConcurrentHashMap<>();
    private volatile long currentTick = 0L;

    private volatile NilumTcpServer tcpServer;
    private volatile String tcpAdvertisedHost;
    private volatile int tcpPort;

    private record PendingHandshake(ServerConfigurationPacketListenerImpl listener, long deadlineTick) {
    }

    private NeoForgeServerHandshake(NilumConfigManager configManager, NilumLogger logger, String serverModVersion,
                                     NilumAssetHost assetHost) {
        this.configManager = configManager;
        this.logger = logger;
        this.serverModVersion = serverModVersion;
        this.assetHost = assetHost;
    }

    public static NeoForgeServerHandshake register(IEventBus modEventBus, NilumConfigManager configManager,
                                                     NilumLogger logger, String serverModVersion, NilumAssetHost assetHost) {
        NeoForgeServerHandshake handshake = new NeoForgeServerHandshake(configManager, logger, serverModVersion, assetHost);
        handshake.applyTcpConfig();

        modEventBus.addListener(RegisterConfigurationTasksEvent.class, handshake::onRegisterConfigurationTasks);

        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                handshake.onJoin(player);
            }
        });

        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event ->
                handshake.onDisconnect(event.getEntity().getUUID()));

        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event ->
                handshake.onServerTick(event.getServer()));

        return handshake;
    }

    /**
     * Restarts the TCP side-channel to match the current config. Leaves it
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

    private void onRegisterConfigurationTasks(RegisterConfigurationTasksEvent event) {
        ServerConfigurationPacketListenerImpl listener = (ServerConfigurationPacketListenerImpl) event.getListener();
        UUID playerId = listener.getOwner().id();
        pendingHandshakes.put(playerId, new PendingHandshake(listener, currentTick + TIMEOUT_TICKS));

        event.register(new NilumHandshakeTask(new HelloPacket(HandshakeProtocol.PROTOCOL_VERSION, serverModVersion)));
    }

    public void onHelloAck(NilumHelloAckPayload payload, IPayloadContext context) {
        // This handshake only ever sends nilum:hello as a configuration task (see
        // onRegisterConfigurationTasks); a play-phase ack means a Paper server's client
        // replying via play, which isn't this instance's business.
        if (!(context.listener() instanceof ServerConfigurationPacketListenerImpl listener)) {
            return;
        }
        UUID playerId = listener.getOwner().id();
        pendingHandshakes.remove(playerId);

        HelloAckPacket ack = HelloAckPacket.decode(payload.data());

        if (SemanticVersions.isOlder(ack.modVersion(), serverModVersion)) {
            logger.warn(listener.getOwner().name() + "'s Nilum version (" + ack.modVersion()
                    + ") is older than this server's (" + serverModVersion + "), kicking.");
            context.disconnect(Component.literal(HandshakeProtocol.tooOldMessage(ack.modVersion(), serverModVersion)));
            return;
        }

        acknowledged.put(playerId, ack);
        logger.info(listener.getOwner().name() + " completed the Nilum handshake (loader=" + ack.loader()
                + ", version=" + ack.modVersion() + ").");

        context.finishCurrentTask(NilumHandshakeTask.TYPE);
    }

    public void onTcpUnavailable(ServerPlayer player) {
        logger.warn(player.getName().getString() + "'s client couldn't reach the TCP side-channel, "
                + "falling back to plugin-channel transfer for this session.");
    }

    private void onTcpConnected(UUID playerId, Socket socket) {
        tcpConnections.put(playerId, socket);
        logger.info("TCP side-channel connected for " + playerId + ", serving asset requests.");
        NilumTcpAssetServer.serve(socket, assetHost::assetBytes);
    }

    private void onJoin(ServerPlayer player) {
        NilumTcpServer server = tcpServer;
        if (server != null) {
            UUID playerId = player.getUUID();
            String token = server.offerConnection(playerId);
            PacketDistributor.sendToPlayer(player, new NilumTcpOfferPayload(
                    new TcpOfferPacket(tcpAdvertisedHost, tcpPort, token).encode()));
        }

        PacketDistributor.sendToPlayer(player, new NilumAssetManifestPayload(
                new AssetManifestPacket(assetHost.manifest()).encode()));
    }

    private void onDisconnect(UUID playerId) {
        pendingHandshakes.remove(playerId);
        acknowledged.remove(playerId);
        NilumTcpServer server = tcpServer;
        if (server != null) {
            server.cancelOffer(playerId);
        }
        Socket socket = tcpConnections.remove(playerId);
        if (socket != null) {
            closeQuietly(socket);
        }
    }

    private void onServerTick(MinecraftServer server) {
        currentTick++;

        pendingHandshakes.entrySet().removeIf(entry -> {
            if (currentTick < entry.getValue().deadlineTick()) {
                return false;
            }
            ServerConfigurationPacketListenerImpl listener = entry.getValue().listener();
            logger.warn(listener.getOwner().name() + " didn't respond to the handshake in time, kicking.");
            listener.disconnect(Component.literal(HandshakeProtocol.REQUIRES_MOD_MESSAGE));
            return true;
        });
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
