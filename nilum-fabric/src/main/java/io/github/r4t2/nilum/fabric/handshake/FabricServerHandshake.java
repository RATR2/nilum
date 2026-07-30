package io.github.r4t2.nilum.fabric.handshake;

import io.github.r4t2.nilum.common.config.NilumConfigManager;
import io.github.r4t2.nilum.common.config.TcpConfig;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.protocol.HelloAckPacket;
import io.github.r4t2.nilum.common.protocol.HelloPacket;
import io.github.r4t2.nilum.common.protocol.TcpOfferPacket;
import io.github.r4t2.nilum.common.tcp.NilumTcpServer;
import io.github.r4t2.nilum.common.util.SemanticVersions;
import io.github.r4t2.nilum.fabric.network.NilumHelloAckPayload;
import io.github.r4t2.nilum.fabric.network.NilumHelloPayload;
import io.github.r4t2.nilum.fabric.network.NilumTcpOfferPayload;
import io.github.r4t2.nilum.fabric.network.NilumTcpUnavailablePayload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * sends {@code nilum:hello} on join and kicks players who don't
 * respond with {@code nilum:hello_ack} within 60 ticks, when this instance
 * is running as a Fabric server.
 */
public final class FabricServerHandshake {

    private static final long TIMEOUT_TICKS = 60L;
    private static final String PROTOCOL_VERSION = "1.0.0";
    private static final String REQUIRES_MOD_MESSAGE =
            "This server requires Nilum. Get it at: https://github.com/RATR2/nilum";

    private final NilumConfigManager configManager;
    private final NilumLogger logger;

    private final Map<UUID, Long> pendingDeadlines = new ConcurrentHashMap<>();
    private final Map<UUID, HelloAckPacket> acknowledged = new ConcurrentHashMap<>();
    private final Map<UUID, Socket> tcpConnections = new ConcurrentHashMap<>();
    private volatile long currentTick = 0L;
    private volatile String serverModVersion;

    private volatile NilumTcpServer tcpServer;
    private volatile String tcpAdvertisedHost;
    private volatile int tcpPort;

    private FabricServerHandshake(NilumConfigManager configManager, NilumLogger logger) {
        this.configManager = configManager;
        this.logger = logger;
    }

    public static FabricServerHandshake register(NilumConfigManager configManager, NilumLogger logger) {
        FabricServerHandshake handshake = new FabricServerHandshake(configManager, logger);
        handshake.applyTcpConfig();

        ServerPlayNetworking.registerGlobalReceiver(NilumHelloAckPayload.TYPE,
                (payload, context) -> handshake.onHelloAck(context.player(), payload));

        ServerPlayNetworking.registerGlobalReceiver(NilumTcpUnavailablePayload.TYPE,
                (payload, context) -> handshake.onTcpUnavailable(context.player()));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                handshake.onJoin(handler.getPlayer()));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                handshake.onDisconnect(handler.getPlayer().getUUID()));

        ServerTickEvents.END_SERVER_TICK.register(handshake::onServerTick);

        return handshake;
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

    private String serverModVersion() {
        if (serverModVersion == null) {
            serverModVersion = FabricLoader.getInstance()
                    .getModContainer("nilum")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
        }
        return serverModVersion;
    }

    private void onJoin(ServerPlayer player) {
        ServerPlayNetworking.send(player, new NilumHelloPayload(
                new HelloPacket(PROTOCOL_VERSION, serverModVersion()).encode()));
        pendingDeadlines.put(player.getUUID(), currentTick + TIMEOUT_TICKS);
    }

    private void onDisconnect(UUID playerId) {
        pendingDeadlines.remove(playerId);
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

        pendingDeadlines.entrySet().removeIf(entry -> {
            if (currentTick < entry.getValue()) {
                return false;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                logger.warn(player.getName().getString() + " didn't respond to the handshake in time, kicking.");
                player.connection.disconnect(Component.literal(REQUIRES_MOD_MESSAGE));
            }
            return true;
        });
    }

    private void onHelloAck(ServerPlayer player, NilumHelloAckPayload payload) {
        UUID playerId = player.getUUID();
        pendingDeadlines.remove(playerId);

        HelloAckPacket ack = HelloAckPacket.decode(payload.data());

        if (SemanticVersions.isOlder(ack.modVersion(), serverModVersion())) {
            logger.warn(player.getName().getString() + "'s Nilum version (" + ack.modVersion()
                    + ") is older than this server's (" + serverModVersion() + "), kicking.");
            player.connection.disconnect(Component.literal(
                    "Your Nilum version (" + ack.modVersion() + ") is too old for this server, which requires "
                            + serverModVersion() + " or newer. Get it at: https://github.com/RATR2/nilum"));
            return;
        }

        acknowledged.put(playerId, ack);
        logger.info(player.getName().getString() + " completed the Nilum handshake (loader=" + ack.loader()
                + ", version=" + ack.modVersion() + ").");

        NilumTcpServer server = tcpServer;
        if (server != null) {
            String token = server.offerConnection(playerId);
            ServerPlayNetworking.send(player, new NilumTcpOfferPayload(
                    new TcpOfferPacket(tcpAdvertisedHost, tcpPort, token).encode()));
        }
    }

    private void onTcpUnavailable(ServerPlayer player) {
        logger.warn(player.getName().getString() + "'s client couldn't reach the TCP side-channel, "
                + "falling back to plugin-channel transfer for this session.");
    }

    private void onTcpConnected(UUID playerId, Socket socket) {
        tcpConnections.put(playerId, socket);
        logger.info("TCP side-channel connected for " + playerId
                + " (nothing streams over it yet - no asset system built).");
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
