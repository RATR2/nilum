package io.github.r4t2.nilum.fabric;

import io.github.r4t2.nilum.common.asset.AssetCache;
import io.github.r4t2.nilum.common.asset.AssetSyncSession;
import io.github.r4t2.nilum.common.asset.ClientModelStore;
import io.github.r4t2.nilum.common.model.ClientModelPlacements;
import io.github.r4t2.nilum.common.protocol.AssetManifestPacket;
import io.github.r4t2.nilum.common.protocol.HelloAckPacket;
import io.github.r4t2.nilum.common.protocol.HelloPacket;
import io.github.r4t2.nilum.common.protocol.ModelSpawnPacket;
import io.github.r4t2.nilum.common.protocol.TcpOfferPacket;
import io.github.r4t2.nilum.common.protocol.TcpUnavailablePacket;
import io.github.r4t2.nilum.common.tcp.NilumTcpClient;
import io.github.r4t2.nilum.common.util.SemanticVersions;
import io.github.r4t2.nilum.fabric.network.NilumAssetManifestPayload;
import io.github.r4t2.nilum.fabric.network.NilumHelloAckPayload;
import io.github.r4t2.nilum.fabric.network.NilumHelloPayload;
import io.github.r4t2.nilum.fabric.network.NilumModelSpawnPayload;
import io.github.r4t2.nilum.fabric.network.NilumTcpOfferPayload;
import io.github.r4t2.nilum.fabric.network.NilumTcpUnavailablePayload;
import io.github.r4t2.nilum.fabric.render.NilumItemDisplayRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.EntityType;

import java.net.Socket;

public final class NilumFabricClient implements ClientModInitializer {

    private static final String PROTOCOL_VERSION = "1.0.0";
    private static final int TCP_CONNECT_TIMEOUT_MILLIS = 5000;

    @Override
    public void onInitializeClient() {
        AssetCache assetCache = new AssetCache(FabricLoader.getInstance().getConfigDir().resolve("nilum-cache"));
        ClientModelStore modelStore = new ClientModelStore();
        ClientModelPlacements placements = new ClientModelPlacements();
        AssetSyncSession assetSync = new AssetSyncSession(assetCache, modelStore, NilumFabricMod.LOGGER);

        // Deprecated as of fabric-rendering-v1 16.2.10 with no replacement yet; still works.
        //noinspection deprecation
        EntityRendererRegistry.register(EntityType.ITEM_DISPLAY,
                context -> new NilumItemDisplayRenderer(context, modelStore, placements));

        ClientPlayNetworking.registerGlobalReceiver(NilumHelloPayload.TYPE, (payload, context) -> {
            HelloPacket hello = HelloPacket.decode(payload.data());

            String modVersion = FabricLoader.getInstance()
                    .getModContainer("nilum")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");

            if (SemanticVersions.isNewer(modVersion, hello.serverModVersion())) {
                NilumFabricMod.LOGGER.warn("This Nilum client (" + modVersion + ") is newer than the server ("
                        + hello.serverModVersion() + ") - some features may not be available.");
            }

            HelloAckPacket ack = new HelloAckPacket(
                    "fabric",
                    modVersion,
                    PROTOCOL_VERSION,
                    false,
                    false,
                    "vanilla"
            );

            ClientPlayNetworking.send(new NilumHelloAckPayload(ack.encode()));
        });

        ClientPlayNetworking.registerGlobalReceiver(NilumTcpOfferPayload.TYPE, (payload, context) -> {
            TcpOfferPacket offer = TcpOfferPacket.decode(payload.data());

            // NilumTcpClient.connect blocks until success/timeout - never call it on the
            // network callback thread, that would stall all other packet handling.
            Thread.ofVirtual().start(() -> {
                Socket socket = NilumTcpClient.connect(offer.host(), offer.port(), offer.token(),
                        TCP_CONNECT_TIMEOUT_MILLIS);

                if (socket != null) {
                    NilumFabricMod.LOGGER.info("TCP side-channel connected to " + offer.host() + ":" + offer.port() + ".");
                    assetSync.onTcpConnected(socket);
                } else {
                    NilumFabricMod.LOGGER.warn("Couldn't reach the TCP side-channel at " + offer.host() + ":"
                            + offer.port() + ", falling back to plugin-channel transfer for this session.");
                    ClientPlayNetworking.send(new NilumTcpUnavailablePayload(new TcpUnavailablePacket().encode()));
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(NilumAssetManifestPayload.TYPE, (payload, context) ->
                assetSync.onManifest(AssetManifestPacket.decode(payload.data()).entries()));

        ClientPlayNetworking.registerGlobalReceiver(NilumModelSpawnPayload.TYPE, (payload, context) -> {
            ModelSpawnPacket spawn = ModelSpawnPacket.decode(payload.data());
            placements.put(spawn.entityId(), spawn.modelId());
        });
    }
}
