package io.github.r4t2.nilum.fabric;

import io.github.r4t2.nilum.common.asset.AssetCache;
import io.github.r4t2.nilum.common.asset.AssetSyncSession;
import io.github.r4t2.nilum.common.asset.ClientModelStore;
import io.github.r4t2.nilum.common.model.ClientModelPlacements;
import io.github.r4t2.nilum.common.protocol.AssetManifestPacket;
import io.github.r4t2.nilum.common.protocol.HandshakeProtocol;
import io.github.r4t2.nilum.common.protocol.HelloAckPacket;
import io.github.r4t2.nilum.common.protocol.HelloPacket;
import io.github.r4t2.nilum.common.protocol.ModEntry;
import io.github.r4t2.nilum.common.protocol.ModListPacket;
import io.github.r4t2.nilum.common.protocol.ModelSpawnPacket;
import io.github.r4t2.nilum.common.protocol.TcpOfferPacket;
import io.github.r4t2.nilum.common.protocol.TcpUnavailablePacket;
import io.github.r4t2.nilum.common.tcp.NilumTcpClient;
import io.github.r4t2.nilum.common.util.SemanticVersions;
import io.github.r4t2.nilum.fabric.creativetab.NilumCreativeTabs;
import io.github.r4t2.nilum.fabric.network.NilumAssetManifestPayload;
import io.github.r4t2.nilum.fabric.network.NilumHelloAckPayload;
import io.github.r4t2.nilum.fabric.network.NilumHelloPayload;
import io.github.r4t2.nilum.fabric.network.NilumModListPayload;
import io.github.r4t2.nilum.fabric.network.NilumModListRequestPayload;
import io.github.r4t2.nilum.fabric.network.NilumModelSpawnPayload;
import io.github.r4t2.nilum.fabric.network.NilumTcpOfferPayload;
import io.github.r4t2.nilum.fabric.network.NilumTcpUnavailablePayload;
import io.github.r4t2.nilum.fabric.render.IconAtlas;
import io.github.r4t2.nilum.fabric.render.NilumIconItemModel;
import io.github.r4t2.nilum.fabric.render.NilumIconSpecialRenderer;
import io.github.r4t2.nilum.fabric.render.NilumItemDisplayRenderer;
import io.github.r4t2.nilum.fabric.render.NilumModelItemModel;
import io.github.r4t2.nilum.fabric.render.NilumModelItemSpecialRenderer;
import io.github.r4t2.nilum.fabric.render.TextureUploader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.EntityType;

import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

public final class NilumFabricClient implements ClientModInitializer {

    private static final int TCP_CONNECT_TIMEOUT_MILLIS = 5000;

    @Override
    public void onInitializeClient() {
        AssetCache assetCache = new AssetCache(FabricLoader.getInstance().getConfigDir().resolve("nilum-cache"));
        ClientModelStore modelStore = new ClientModelStore();
        ClientModelPlacements placements = new ClientModelPlacements();
        IconAtlas iconAtlas = new IconAtlas(
                FabricLoader.getInstance().getConfigDir().resolve("nilum-cache").resolve("icon_atlas_debug.png"));
        AssetSyncSession assetSync = new AssetSyncSession(assetCache, modelStore, iconAtlas::add, NilumFabricMod.LOGGER);
        TextureUploader textureUploader = new TextureUploader();

        // Deprecated as of fabric-rendering-v1 16.2.10 with no replacement yet; still works.
        //noinspection deprecation
        EntityRendererRegistry.register(EntityType.ITEM_DISPLAY,
                context -> new NilumItemDisplayRenderer(context, modelStore, placements, textureUploader));

        NilumIconSpecialRenderer iconRenderer = new NilumIconSpecialRenderer(iconAtlas);
        NilumModelItemSpecialRenderer modelRenderer = new NilumModelItemSpecialRenderer(modelStore, textureUploader);
        ModelLoadingPlugin.register(context -> context.modifyItemModelAfterBake().register(
                (original, ctx) -> new NilumModelItemModel(
                        new NilumIconItemModel(original, iconAtlas, iconRenderer), modelStore, modelRenderer)));

        NilumCreativeTabs.register(iconAtlas, modelStore);

        // Registered on both phases: a Paper server sends hello in PLAY, a Fabric-hosted
        // server sends it during configuration (see FabricServerHandshake).
        ClientConfigurationNetworking.registerGlobalReceiver(NilumHelloPayload.TYPE, (payload, context) ->
                handleHello(payload, ClientConfigurationNetworking::send));
        ClientPlayNetworking.registerGlobalReceiver(NilumHelloPayload.TYPE, (payload, context) ->
                handleHello(payload, ClientPlayNetworking::send));

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

        ClientPlayNetworking.registerGlobalReceiver(NilumModListRequestPayload.TYPE, (payload, context) -> {
            List<ModEntry> mods = FabricLoader.getInstance().getAllMods().stream()
                    .map(mod -> new ModEntry(mod.getMetadata().getId(), mod.getMetadata().getVersion().getFriendlyString()))
                    .toList();
            ClientPlayNetworking.send(new NilumModListPayload(new ModListPacket(mods).encode()));
        });
    }

    private static void handleHello(NilumHelloPayload payload, Consumer<CustomPacketPayload> sender) {
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
                HandshakeProtocol.PROTOCOL_VERSION,
                false,
                false,
                "vanilla"
        );

        sender.accept(new NilumHelloAckPayload(ack.encode()));
    }
}
