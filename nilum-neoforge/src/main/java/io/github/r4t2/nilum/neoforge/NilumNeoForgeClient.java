package io.github.r4t2.nilum.neoforge;

import io.github.r4t2.nilum.common.asset.AssetCache;
import io.github.r4t2.nilum.common.asset.AssetSyncSession;
import io.github.r4t2.nilum.common.asset.ClientModelStore;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.model.ClientModelPlacements;
import io.github.r4t2.nilum.common.protocol.AssetManifestPacket;
import io.github.r4t2.nilum.common.protocol.HandshakeProtocol;
import io.github.r4t2.nilum.common.protocol.HelloAckPacket;
import io.github.r4t2.nilum.common.protocol.HelloPacket;
import io.github.r4t2.nilum.common.protocol.ModelSpawnPacket;
import io.github.r4t2.nilum.common.protocol.TcpOfferPacket;
import io.github.r4t2.nilum.common.protocol.TcpUnavailablePacket;
import io.github.r4t2.nilum.common.tcp.NilumTcpClient;
import io.github.r4t2.nilum.common.util.SemanticVersions;
import io.github.r4t2.nilum.neoforge.network.NilumAssetManifestPayload;
import io.github.r4t2.nilum.neoforge.network.NilumHelloAckPayload;
import io.github.r4t2.nilum.neoforge.network.NilumHelloPayload;
import io.github.r4t2.nilum.neoforge.network.NilumModelSpawnPayload;
import io.github.r4t2.nilum.neoforge.network.NilumTcpOfferPayload;
import io.github.r4t2.nilum.neoforge.network.NilumTcpUnavailablePayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.net.Socket;

/**
 * Client-only handling for the handshake, TCP offer, and asset manifest/model-spawn tracking.
 */
final class NilumNeoForgeClient {

    private static final int TCP_CONNECT_TIMEOUT_MILLIS = 5000;

    private NilumNeoForgeClient() {
    }

    static void register(IEventBus modEventBus, NilumLogger logger, String modVersion) {
        AssetCache assetCache = new AssetCache(FMLPaths.CONFIGDIR.get().resolve("nilum-cache"));
        ClientModelStore modelStore = new ClientModelStore();
        ClientModelPlacements placements = new ClientModelPlacements();
        AssetSyncSession assetSync = new AssetSyncSession(assetCache, modelStore, logger);

        modEventBus.addListener((RegisterClientPayloadHandlersEvent event) -> {
            event.register(NilumHelloPayload.TYPE, (payload, context) -> handleHello(payload, context, logger, modVersion));
            event.register(NilumTcpOfferPayload.TYPE, (payload, context) -> handleTcpOffer(payload, logger, assetSync));
            event.register(NilumAssetManifestPayload.TYPE, (payload, context) ->
                    assetSync.onManifest(AssetManifestPacket.decode(payload.data()).entries()));
            event.register(NilumModelSpawnPayload.TYPE, (payload, context) -> {
                ModelSpawnPacket spawn = ModelSpawnPacket.decode(payload.data());
                placements.put(spawn.entityId(), spawn.modelId());
            });
        });
    }

    private static void handleHello(NilumHelloPayload payload, IPayloadContext context, NilumLogger logger, String modVersion) {
        HelloPacket hello = HelloPacket.decode(payload.data());

        if (SemanticVersions.isNewer(modVersion, hello.serverModVersion())) {
            logger.warn("This Nilum client (" + modVersion + ") is newer than the server ("
                    + hello.serverModVersion() + ") - some features may not be available.");
        }

        HelloAckPacket ack = new HelloAckPacket(
                "neoforge",
                modVersion,
                HandshakeProtocol.PROTOCOL_VERSION,
                false,
                false,
                "vanilla"
        );

        // Sent during configuration - Minecraft.getInstance().getConnection() (used by
        // ClientPacketDistributor) is the play listener and is null at this point.
        context.reply(new NilumHelloAckPayload(ack.encode()));
    }

    private static void handleTcpOffer(NilumTcpOfferPayload payload, NilumLogger logger, AssetSyncSession assetSync) {
        TcpOfferPacket offer = TcpOfferPacket.decode(payload.data());

        // NilumTcpClient.connect blocks until success/timeout - never call it on the
        // network callback thread, that would stall all other packet handling.
        Thread.ofVirtual().start(() -> {
            Socket socket = NilumTcpClient.connect(offer.host(), offer.port(), offer.token(),
                    TCP_CONNECT_TIMEOUT_MILLIS);

            if (socket != null) {
                logger.info("TCP side-channel connected to " + offer.host() + ":" + offer.port() + ".");
                assetSync.onTcpConnected(socket);
            } else {
                logger.warn("Couldn't reach the TCP side-channel at " + offer.host() + ":" + offer.port()
                        + ", falling back to plugin-channel transfer for this session.");
                ClientPacketDistributor.sendToServer(
                        new NilumTcpUnavailablePayload(new TcpUnavailablePacket().encode()));
            }
        });
    }
}
