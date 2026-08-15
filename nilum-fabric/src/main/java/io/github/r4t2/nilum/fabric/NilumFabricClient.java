package io.github.r4t2.nilum.fabric;

import io.github.r4t2.nilum.common.asset.AssetCache;
import io.github.r4t2.nilum.common.asset.AssetSyncSession;
import io.github.r4t2.nilum.common.asset.ClientModelStore;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.ClientModelPlacements;
import io.github.r4t2.nilum.common.protocol.ActivateShaderPackPacket;
import io.github.r4t2.nilum.common.protocol.AssetManifestPacket;
import io.github.r4t2.nilum.common.protocol.AtlasPatchPacket;
import io.github.r4t2.nilum.common.protocol.BlockAnimationPlayPacket;
import io.github.r4t2.nilum.common.protocol.BlockAnimationStopPacket;
import io.github.r4t2.nilum.common.protocol.ChunkBlocksPacket;
import io.github.r4t2.nilum.common.protocol.EntityAnimationPlayPacket;
import io.github.r4t2.nilum.common.protocol.EntityAnimationStopPacket;
import io.github.r4t2.nilum.common.protocol.HandshakeProtocol;
import io.github.r4t2.nilum.common.protocol.HelloAckPacket;
import io.github.r4t2.nilum.common.protocol.HelloPacket;
import io.github.r4t2.nilum.common.protocol.HudFrameOverridePacket;
import io.github.r4t2.nilum.common.protocol.HudFramePacket;
import io.github.r4t2.nilum.common.protocol.HudFrameReleasePacket;
import io.github.r4t2.nilum.common.protocol.ModEntry;
import io.github.r4t2.nilum.common.protocol.ModListPacket;
import io.github.r4t2.nilum.common.protocol.ModelSpawnPacket;
import io.github.r4t2.nilum.common.protocol.RegisterClientVarPacket;
import io.github.r4t2.nilum.common.protocol.SetClientVarPacket;
import io.github.r4t2.nilum.common.protocol.SetHudTextPacket;
import io.github.r4t2.nilum.common.protocol.TcpOfferPacket;
import io.github.r4t2.nilum.common.protocol.TcpUnavailablePacket;
import io.github.r4t2.nilum.common.tcp.NilumTcpClient;
import io.github.r4t2.nilum.common.util.SemanticVersions;
import io.github.r4t2.nilum.fabric.block.ClientBlockRegistry;
import io.github.r4t2.nilum.fabric.font.ClientFontStore;
import io.github.r4t2.nilum.fabric.font.FontInstaller;
import io.github.r4t2.nilum.fabric.block.NilumBlockEntity;
import io.github.r4t2.nilum.fabric.block.NilumBlockEntityRenderer;
import io.github.r4t2.nilum.fabric.block.NilumBlockRenderer;
import io.github.r4t2.nilum.fabric.block.NilumBlockStateModel;
import io.github.r4t2.nilum.fabric.block.NilumBlocks;
import io.github.r4t2.nilum.fabric.creativetab.NilumCreativeTabs;
import io.github.r4t2.nilum.fabric.hud.ClientHudAtlasStore;
import io.github.r4t2.nilum.fabric.hud.ClientVarStore;
import io.github.r4t2.nilum.fabric.hud.HudAtlasRenderer;
import io.github.r4t2.nilum.fabric.keybind.NilumKeybinds;
import io.github.r4t2.nilum.fabric.network.NilumActivateShaderPackPayload;
import io.github.r4t2.nilum.fabric.network.NilumAssetManifestPayload;
import io.github.r4t2.nilum.fabric.network.NilumAtlasPatchPayload;
import io.github.r4t2.nilum.fabric.network.NilumBlockAnimationPlayPayload;
import io.github.r4t2.nilum.fabric.network.NilumBlockAnimationStopPayload;
import io.github.r4t2.nilum.fabric.network.NilumChunkBlocksPayload;
import io.github.r4t2.nilum.fabric.network.NilumDeactivateShaderPackPayload;
import io.github.r4t2.nilum.fabric.network.NilumEntityAnimationPlayPayload;
import io.github.r4t2.nilum.fabric.network.NilumEntityAnimationStopPayload;
import io.github.r4t2.nilum.fabric.network.NilumHelloAckPayload;
import io.github.r4t2.nilum.fabric.network.NilumHelloPayload;
import io.github.r4t2.nilum.fabric.network.NilumHudFrameOverridePayload;
import io.github.r4t2.nilum.fabric.network.NilumHudFramePayload;
import io.github.r4t2.nilum.fabric.network.NilumHudFrameReleasePayload;
import io.github.r4t2.nilum.fabric.network.NilumModListPayload;
import io.github.r4t2.nilum.fabric.network.NilumModListRequestPayload;
import io.github.r4t2.nilum.fabric.network.NilumModelSpawnPayload;
import io.github.r4t2.nilum.fabric.network.NilumRegisterClientVarPayload;
import io.github.r4t2.nilum.fabric.network.NilumSetClientVarPayload;
import io.github.r4t2.nilum.fabric.network.NilumSetHudTextPayload;
import io.github.r4t2.nilum.fabric.network.NilumTcpOfferPayload;
import io.github.r4t2.nilum.fabric.network.NilumTcpUnavailablePayload;
import io.github.r4t2.nilum.fabric.render.IconAtlas;
import io.github.r4t2.nilum.fabric.render.NilumGlintSpecialRenderer;
import io.github.r4t2.nilum.fabric.render.NilumIconItemModel;
import io.github.r4t2.nilum.fabric.render.NilumIconSpecialRenderer;
import io.github.r4t2.nilum.fabric.render.NilumIrisIntegration;
import io.github.r4t2.nilum.fabric.render.NilumItemDisplayRenderer;
import io.github.r4t2.nilum.fabric.render.NilumModelItemModel;
import io.github.r4t2.nilum.fabric.render.NilumModelItemSpecialRenderer;
import io.github.r4t2.nilum.fabric.render.ShaderCapability;
import io.github.r4t2.nilum.fabric.render.TextureUploader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class NilumFabricClient implements ClientModInitializer {

    private static final int TCP_CONNECT_TIMEOUT_MILLIS = 5000;

    // Mixins have no constructor to receive these through, so they're exposed statically here,
    // same pattern NilumFabricMod already uses for CONFIG/LOGGER.
    public static ClientModelStore MODEL_STORE;
    public static ClientModelPlacements PLACEMENTS;
    public static TextureUploader TEXTURE_UPLOADER;
    public static ClientFontStore FONT_STORE;

    @Override
    public void onInitializeClient() {
        AssetCache assetCache = new AssetCache(FabricLoader.getInstance().getConfigDir().resolve("nilum-cache"));
        ClientModelStore modelStore = new ClientModelStore();
        ClientModelPlacements placements = new ClientModelPlacements();
        MODEL_STORE = modelStore;
        PLACEMENTS = placements;
        IconAtlas iconAtlas = new IconAtlas(
                FabricLoader.getInstance().getConfigDir().resolve("nilum-cache").resolve("icon_atlas_debug.png"));
        ClientHudAtlasStore hudAtlases = new ClientHudAtlasStore();
        ClientVarStore clientVars = new ClientVarStore();

        // NilumIrisIntegration references Iris's own classes directly, so it (and anything that
        // touches it) must never be constructed/called unless Iris is actually installed; merely
        // loading the class without Iris present throws NoClassDefFoundError.
        BiConsumer<String, byte[]> shaderPackSink;
        if (FabricLoader.getInstance().isModLoaded("iris")) {
            NilumIrisIntegration irisIntegration = new NilumIrisIntegration();
            Path shaderScratchDir = FabricLoader.getInstance().getConfigDir().resolve("nilum-cache");
            shaderPackSink = (packId, data) -> irisIntegration.installPack(packId, data, shaderScratchDir);
            ClientPlayNetworking.registerGlobalReceiver(NilumActivateShaderPackPayload.TYPE, (payload, context) -> {
                ActivateShaderPackPacket packet = ActivateShaderPackPacket.decode(payload.data());
                irisIntegration.activate(packet.packId());
            });
            ClientPlayNetworking.registerGlobalReceiver(NilumDeactivateShaderPackPayload.TYPE, (payload, context) ->
                    irisIntegration.deactivate());
        } else {
            shaderPackSink = (packId, data) ->
                    NilumFabricMod.LOGGER.warn("Shader pack '" + packId + "' received but Iris isn't installed - ignoring.");
            ClientPlayNetworking.registerGlobalReceiver(NilumActivateShaderPackPayload.TYPE, (payload, context) -> { });
            ClientPlayNetworking.registerGlobalReceiver(NilumDeactivateShaderPackPayload.TYPE, (payload, context) -> { });
        }

        Path fontDirectory = FabricLoader.getInstance().getConfigDir().resolve("nilum").resolve("font");
        ClientFontStore fontStore = new ClientFontStore();
        FONT_STORE = fontStore;
        BiConsumer<String, byte[]> fontSink = (fontId, data) -> {
            FontInstaller.install(fontId, data, fontDirectory);
            fontStore.install(fontId, data);
        };

        AssetSyncSession assetSync = new AssetSyncSession(assetCache, modelStore, iconAtlas::add, hudAtlases::add,
                shaderPackSink, fontSink, NilumFabricMod.LOGGER);
        TextureUploader textureUploader = new TextureUploader();
        TEXTURE_UPLOADER = textureUploader;
        // A model reloading with new bytes under the same id (e.g. a default-retexture block
        // after a blocktextures/ edit) must drop any already-uploaded GPU textures for it, or
        // getOrUpload's cache would keep serving the old ones forever.
        modelStore.onReload(textureUploader::invalidate);

        // Deprecated as of fabric-rendering-v1 16.2.10 with no replacement yet; still works.
        //noinspection deprecation
        EntityRendererRegistry.register(EntityType.ITEM_DISPLAY,
                context -> new NilumItemDisplayRenderer(context, modelStore, placements, textureUploader));

        NilumIconSpecialRenderer iconRenderer = new NilumIconSpecialRenderer(iconAtlas);
        NilumModelItemSpecialRenderer modelRenderer = new NilumModelItemSpecialRenderer(modelStore, textureUploader);
        NilumGlintSpecialRenderer glintRenderer = new NilumGlintSpecialRenderer(iconAtlas);
        ModelLoadingPlugin.register(context -> context.modifyItemModelAfterBake().register(
                (original, ctx) -> new NilumModelItemModel(
                        new NilumIconItemModel(original, iconAtlas, iconRenderer, glintRenderer),
                        modelStore, modelRenderer, glintRenderer)));

        NilumCreativeTabs.register(iconAtlas, modelStore);

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("nilum", "hud_atlas"),
                new HudAtlasRenderer(hudAtlases, clientVars));

        NilumKeybinds.register();

        ClientBlockRegistry blockRegistry = new ClientBlockRegistry();
        ModelLoadingPlugin.register(blockContext -> blockContext.modifyBlockModelAfterBake().register(
                (original, ctx) -> new NilumBlockStateModel(original, blockRegistry)));
        ClientChunkEvents.CHUNK_UNLOAD.register((clientWorld, chunk) ->
                blockRegistry.onChunkUnload(chunk.getPos().x, chunk.getPos().z));
        NilumBlockRenderer blockRenderer = new NilumBlockRenderer(modelStore, blockRegistry, textureUploader);
        WorldRenderEvents.AFTER_ENTITIES.register(blockRenderer::render);

        // Real blocks on a Fabric/NeoForge-hosted server: rendered via a genuine
        // BlockEntityRenderer, not the wire-block overlay pass above (that stays for Paper).
        BlockEntityRenderers.register(NilumBlocks.BLOCK_ENTITY_TYPE,
                context -> new NilumBlockEntityRenderer(modelStore, blockRegistry, textureUploader));

        // Registered on both phases: a Paper server sends hello in PLAY, a Fabric-hosted
        // server sends it during configuration (see FabricServerHandshake).
        ClientConfigurationNetworking.registerGlobalReceiver(NilumHelloPayload.TYPE, (payload, context) ->
                handleHello(payload, ClientConfigurationNetworking::send));
        ClientPlayNetworking.registerGlobalReceiver(NilumHelloPayload.TYPE, (payload, context) ->
                handleHello(payload, ClientPlayNetworking::send));

        ClientPlayNetworking.registerGlobalReceiver(NilumTcpOfferPayload.TYPE, (payload, context) -> {
            TcpOfferPacket offer = TcpOfferPacket.decode(payload.data());

            // NilumTcpClient.connect blocks until success/timeout; never call it on the
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

        ClientPlayNetworking.registerGlobalReceiver(NilumHudFramePayload.TYPE, (payload, context) -> {
            HudFramePacket packet = HudFramePacket.decode(payload.data());
            hudAtlases.onHudFrame(packet.atlasId(), packet.elementId(), packet.frame());
        });

        ClientPlayNetworking.registerGlobalReceiver(NilumAtlasPatchPayload.TYPE, (payload, context) -> {
            AtlasPatchPacket packet = AtlasPatchPacket.decode(payload.data());
            hudAtlases.onAtlasPatch(packet.atlasId(), packet.elementId(), packet.frame(), packet.png());
        });

        ClientPlayNetworking.registerGlobalReceiver(NilumHudFrameOverridePayload.TYPE, (payload, context) -> {
            HudFrameOverridePacket packet = HudFrameOverridePacket.decode(payload.data());
            hudAtlases.onOverride(packet.atlasId(), packet.elementId(), packet.frame(), packet.durationTicks());
        });

        ClientPlayNetworking.registerGlobalReceiver(NilumHudFrameReleasePayload.TYPE, (payload, context) -> {
            HudFrameReleasePacket packet = HudFrameReleasePacket.decode(payload.data());
            hudAtlases.onRelease(packet.atlasId(), packet.elementId());
        });

        ClientPlayNetworking.registerGlobalReceiver(NilumRegisterClientVarPayload.TYPE, (payload, context) -> {
            RegisterClientVarPacket packet = RegisterClientVarPacket.decode(payload.data());
            clientVars.register(packet.name(), packet.initialValue());
        });

        ClientPlayNetworking.registerGlobalReceiver(NilumSetClientVarPayload.TYPE, (payload, context) -> {
            SetClientVarPacket packet = SetClientVarPacket.decode(payload.data());
            clientVars.set(packet.name(), packet.value());
        });

        ClientPlayNetworking.registerGlobalReceiver(NilumSetHudTextPayload.TYPE, (payload, context) -> {
            SetHudTextPacket packet = SetHudTextPacket.decode(payload.data());
            hudAtlases.onHudText(packet.atlasId(), packet.elementId(), packet.text());
        });

        ClientPlayNetworking.registerGlobalReceiver(NilumChunkBlocksPayload.TYPE, (payload, context) -> {
            ChunkBlocksPacket packet = ChunkBlocksPacket.decode(payload.data());
            blockRegistry.apply(packet.entries());
        });

        ClientPlayNetworking.registerGlobalReceiver(NilumEntityAnimationPlayPayload.TYPE, (payload, context) -> {
            EntityAnimationPlayPacket packet = EntityAnimationPlayPacket.decode(payload.data());
            BbModel model = modelForEntity(modelStore, placements, packet.entityId());
            if (model != null) {
                placements.animationState(packet.entityId())
                        .play(model, packet.animationName(), packet.startTimeMillis(), System.currentTimeMillis());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(NilumEntityAnimationStopPayload.TYPE, (payload, context) -> {
            EntityAnimationStopPacket packet = EntityAnimationStopPacket.decode(payload.data());
            BbModel model = modelForEntity(modelStore, placements, packet.entityId());
            if (model != null) {
                placements.animationState(packet.entityId()).stop(model, System.currentTimeMillis());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(NilumBlockAnimationPlayPayload.TYPE, (payload, context) -> {
            BlockAnimationPlayPacket packet = BlockAnimationPlayPacket.decode(payload.data());
            BlockPos pos = new BlockPos(packet.x(), packet.y(), packet.z());
            BbModel model = modelForBlock(modelStore, blockRegistry, pos);
            if (model != null) {
                blockRegistry.animationState(pos).play(model, packet.animationName(), packet.startTimeMillis(), System.currentTimeMillis());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(NilumBlockAnimationStopPayload.TYPE, (payload, context) -> {
            BlockAnimationStopPacket packet = BlockAnimationStopPacket.decode(payload.data());
            BlockPos pos = new BlockPos(packet.x(), packet.y(), packet.z());
            BbModel model = modelForBlock(modelStore, blockRegistry, pos);
            if (model != null) {
                blockRegistry.animationState(pos).stop(model, System.currentTimeMillis());
            }
        });
    }

    private static BbModel modelForEntity(ClientModelStore modelStore, ClientModelPlacements placements, UUID entityId) {
        String modelId = placements.get(entityId);
        return modelId == null ? null : modelStore.model(modelId).orElse(null);
    }

    private static BbModel modelForBlock(ClientModelStore modelStore, ClientBlockRegistry blockRegistry, BlockPos pos) {
        Optional<String> wireOverlayModelId = blockRegistry.modelIdAt(pos);
        if (wireOverlayModelId.isPresent()) {
            return modelStore.model(wireOverlayModelId.get()).orElse(null);
        }

        // A real Nilum block from a Fabric/NeoForge-hosted server isn't tracked in
        // ClientBlockRegistry at all, its model id lives on its own block entity.
        var level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(pos) instanceof NilumBlockEntity blockEntity) {
            return modelStore.model(blockEntity.definitionId()).orElse(null);
        }
        return null;
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

        ShaderCapability.Renderer shaderRenderer = ShaderCapability.detect();
        HelloAckPacket ack = new HelloAckPacket(
                "fabric",
                modVersion,
                HandshakeProtocol.PROTOCOL_VERSION,
                true,
                false,
                shaderRenderer.wireName()
        );

        sender.accept(new NilumHelloAckPayload(ack.encode()));
    }
}
