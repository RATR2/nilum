package io.github.r4t2.nilum.neoforge;

import io.github.r4t2.nilum.common.asset.AssetCache;
import io.github.r4t2.nilum.common.asset.AssetSyncSession;
import io.github.r4t2.nilum.common.asset.ClientModelStore;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.ClientHeldItemAnimationStates;
import io.github.r4t2.nilum.common.model.ClientModelPlacements;
import io.github.r4t2.nilum.common.protocol.ActivateShaderPackPacket;
import io.github.r4t2.nilum.common.protocol.AssetManifestEntry;
import io.github.r4t2.nilum.common.protocol.AssetManifestPacket;
import io.github.r4t2.nilum.common.protocol.ItemDefinedAssetsPacket;
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
import io.github.r4t2.nilum.common.protocol.ItemAnimationPlayPacket;
import io.github.r4t2.nilum.common.protocol.ItemAnimationStopPacket;
import io.github.r4t2.nilum.common.protocol.ModEntry;
import io.github.r4t2.nilum.common.protocol.ModListPacket;
import io.github.r4t2.nilum.common.protocol.ModelSpawnPacket;
import io.github.r4t2.nilum.common.protocol.RegisterClientVarPacket;
import io.github.r4t2.nilum.common.protocol.SetClientVarPacket;
import io.github.r4t2.nilum.common.protocol.OpenUiPacket;
import io.github.r4t2.nilum.common.protocol.SetHudAtlasVisibilityPacket;
import io.github.r4t2.nilum.common.protocol.SetHudElementVisibilityPacket;
import io.github.r4t2.nilum.common.protocol.SetHudTextPacket;
import io.github.r4t2.nilum.common.protocol.TcpOfferPacket;
import io.github.r4t2.nilum.common.protocol.TcpUnavailablePacket;
import io.github.r4t2.nilum.common.tcp.NilumTcpClient;
import io.github.r4t2.nilum.common.util.SemanticVersions;
import io.github.r4t2.nilum.common.util.ServerCacheId;
import io.github.r4t2.nilum.neoforge.block.ClientBlockRegistry;
import io.github.r4t2.nilum.neoforge.block.NilumBlockEntity;
import io.github.r4t2.nilum.neoforge.block.NilumBlockEntityRenderer;
import io.github.r4t2.nilum.neoforge.block.NilumBlockRenderer;
import io.github.r4t2.nilum.neoforge.block.NilumBlockStateModel;
import io.github.r4t2.nilum.neoforge.block.NilumBlocks;
import io.github.r4t2.nilum.neoforge.creativetab.NilumCreativeTabs;
import io.github.r4t2.nilum.neoforge.font.ClientFontStore;
import io.github.r4t2.nilum.neoforge.font.FontInstaller;
import io.github.r4t2.nilum.neoforge.hud.ClientHudAtlasStore;
import io.github.r4t2.nilum.neoforge.network.NilumOpenUiPayload;
import io.github.r4t2.nilum.neoforge.network.NilumSetHudAtlasVisibilityPayload;
import io.github.r4t2.nilum.neoforge.network.NilumSetHudElementVisibilityPayload;
import io.github.r4t2.nilum.neoforge.ui.ClientCustomUiStore;
import io.github.r4t2.nilum.neoforge.ui.NilumCustomUiScreen;
import io.github.r4t2.nilum.neoforge.hud.ClientVarStore;
import io.github.r4t2.nilum.neoforge.hud.HudAtlasRenderer;
import io.github.r4t2.nilum.neoforge.keybind.NilumKeybinds;
import io.github.r4t2.nilum.neoforge.network.NilumActivateShaderPackPayload;
import io.github.r4t2.nilum.neoforge.network.NilumAssetManifestPayload;
import io.github.r4t2.nilum.neoforge.network.NilumItemDefinedAssetsPayload;
import io.github.r4t2.nilum.neoforge.network.NilumAtlasPatchPayload;
import io.github.r4t2.nilum.neoforge.network.NilumBlockAnimationPlayPayload;
import io.github.r4t2.nilum.neoforge.network.NilumBlockAnimationStopPayload;
import io.github.r4t2.nilum.neoforge.network.NilumChunkBlocksPayload;
import io.github.r4t2.nilum.neoforge.network.NilumDeactivateShaderPackPayload;
import io.github.r4t2.nilum.neoforge.network.NilumEntityAnimationPlayPayload;
import io.github.r4t2.nilum.neoforge.network.NilumEntityAnimationStopPayload;
import io.github.r4t2.nilum.neoforge.network.NilumHelloAckPayload;
import io.github.r4t2.nilum.neoforge.network.NilumHelloPayload;
import io.github.r4t2.nilum.neoforge.network.NilumHudFrameOverridePayload;
import io.github.r4t2.nilum.neoforge.network.NilumHudFramePayload;
import io.github.r4t2.nilum.neoforge.network.NilumHudFrameReleasePayload;
import io.github.r4t2.nilum.neoforge.network.NilumItemAnimationPlayPayload;
import io.github.r4t2.nilum.neoforge.network.NilumItemAnimationStopPayload;
import io.github.r4t2.nilum.neoforge.network.NilumModListPayload;
import io.github.r4t2.nilum.neoforge.network.NilumModListRequestPayload;
import io.github.r4t2.nilum.neoforge.network.NilumModelSpawnPayload;
import io.github.r4t2.nilum.neoforge.network.NilumRegisterClientVarPayload;
import io.github.r4t2.nilum.neoforge.network.NilumSetClientVarPayload;
import io.github.r4t2.nilum.neoforge.network.NilumSetHudTextPayload;
import io.github.r4t2.nilum.neoforge.network.NilumTcpOfferPayload;
import io.github.r4t2.nilum.neoforge.network.NilumTcpUnavailablePayload;
import io.github.r4t2.nilum.neoforge.render.IconAtlas;
import io.github.r4t2.nilum.neoforge.render.NilumGlintSpecialRenderer;
import io.github.r4t2.nilum.neoforge.render.NilumIconItemModel;
import io.github.r4t2.nilum.neoforge.render.NilumIconSpecialRenderer;
import io.github.r4t2.nilum.neoforge.render.NilumIrisIntegration;
import io.github.r4t2.nilum.neoforge.render.NilumItemDisplayRenderer;
import io.github.r4t2.nilum.neoforge.render.NilumModelItemModel;
import io.github.r4t2.nilum.neoforge.render.NilumModelItemSpecialRenderer;
import io.github.r4t2.nilum.neoforge.render.ShaderCapability;
import io.github.r4t2.nilum.neoforge.render.TextureUploader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * Client-only handling for the handshake, TCP offer, and asset manifest/model-spawn tracking.
 */
final class NilumNeoForgeClient {

    private static final int TCP_CONNECT_TIMEOUT_MILLIS = 5000;

    private NilumNeoForgeClient() {
    }

    static void register(IEventBus modEventBus, NilumLogger logger, String modVersion) {
        Path assetCacheRoot = FMLPaths.CONFIGDIR.get().resolve("nilum-cache");
        AssetCache assetCache = new AssetCache(assetCacheRoot);
        ClientModelStore modelStore = new ClientModelStore();
        ClientModelPlacements placements = new ClientModelPlacements();
        ClientHeldItemAnimationStates heldItemAnimations = new ClientHeldItemAnimationStates();
        IconAtlas iconAtlas = new IconAtlas(
                FMLPaths.CONFIGDIR.get().resolve("nilum-cache").resolve("icon_atlas_debug.png"), logger);
        ClientHudAtlasStore hudAtlases = new ClientHudAtlasStore(logger);
        ClientVarStore clientVars = new ClientVarStore();
        ClientCustomUiStore customUiStore = new ClientCustomUiStore(logger);

        if (ModList.get().isLoaded("oculus") && !ModList.get().isLoaded("iris")) {
            logger.warn("Oculus is installed without Iris. Nilum's shader/glint features integrate with Iris "
                    + "directly and won't work through Oculus; installing Iris instead is recommended.");
        }

        // NilumIrisIntegration references Iris's own classes directly, so it (and anything that
        // touches it) must never be constructed/called unless Iris is actually installed; merely
        // loading the class without Iris present throws NoClassDefFoundError.
        BiConsumer<String, byte[]> shaderPackSink;
        if (ModList.get().isLoaded("iris")) {
            NilumIrisIntegration irisIntegration = new NilumIrisIntegration();
            Path shaderScratchDir = FMLPaths.CONFIGDIR.get().resolve("nilum-cache");
            shaderPackSink = (packId, data) -> irisIntegration.installPack(packId, data, shaderScratchDir);
            modEventBus.addListener((RegisterClientPayloadHandlersEvent event) -> {
                event.register(NilumActivateShaderPackPayload.TYPE, (payload, context) -> {
                    ActivateShaderPackPacket packet = ActivateShaderPackPacket.decode(payload.data());
                    irisIntegration.activate(packet.packId());
                });
                event.register(NilumDeactivateShaderPackPayload.TYPE, (payload, context) -> irisIntegration.deactivate());
            });
        } else {
            shaderPackSink = (packId, data) -> logger.warn("Shader pack '" + packId + "' received but Iris isn't installed, ignoring.");
            modEventBus.addListener((RegisterClientPayloadHandlersEvent event) -> {
                event.register(NilumActivateShaderPackPayload.TYPE, (payload, context) -> { });
                event.register(NilumDeactivateShaderPackPayload.TYPE, (payload, context) -> { });
            });
        }

        Path fontDirectory = FMLPaths.CONFIGDIR.get().resolve("nilum").resolve("font");
        ClientFontStore fontStore = new ClientFontStore(logger);
        BiConsumer<String, byte[]> fontSink = (fontId, data) -> {
            FontInstaller.install(fontId, data, fontDirectory);
            fontStore.install(fontId, data);
        };

        AssetSyncSession assetSync = new AssetSyncSession(assetCache, modelStore, iconAtlas::add, hudAtlases::add,
                shaderPackSink, fontSink, customUiStore::add, logger,
                runnable -> Minecraft.getInstance().execute(runnable));
        TextureUploader textureUploader = new TextureUploader();
        // A model reloading with new bytes under the same id (e.g. a default-retexture block
        // after a blocktextures/ edit) must drop any already-uploaded GPU textures for it, or
        // getOrUpload's cache would keep serving the old ones forever.
        modelStore.onReload(textureUploader::invalidate);

        NilumIconSpecialRenderer iconRenderer = new NilumIconSpecialRenderer(iconAtlas);
        NilumModelItemSpecialRenderer modelRenderer = new NilumModelItemSpecialRenderer(modelStore, textureUploader);
        NilumGlintSpecialRenderer glintRenderer = new NilumGlintSpecialRenderer(iconAtlas);

        // Real blocks on a NeoForge-hosted server (Tier-3): tracked on their own NilumBlockEntity,
        // rendered via a genuine BlockEntityRenderer. Wire-block overlay blocks from a Paper server
        // (Tier-1, no registry access there so it proxies a real vanilla material) are tracked here
        // instead and rendered by NilumBlockRenderer/suppressed by NilumBlockStateModel below;
        // both loaders' clients must support connecting to Paper, so this path stays even though a
        // NeoForge-hosted server itself never needs it.
        ClientBlockRegistry blockRegistry = new ClientBlockRegistry();
        NilumBlockRenderer blockRenderer = new NilumBlockRenderer(modelStore, blockRegistry, textureUploader);

        modEventBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(EntityType.ITEM_DISPLAY,
                    context -> new NilumItemDisplayRenderer(context, modelStore, placements, textureUploader));
            event.registerBlockEntityRenderer(NilumBlocks.BLOCK_ENTITY_TYPE.get(),
                    context -> new NilumBlockEntityRenderer(modelStore, textureUploader, blockRegistry));
        });

        modEventBus.addListener((ModelEvent.ModifyBakingResult event) -> {
            Map<Identifier, ItemModel> models = event.getBakingResult().itemStackModels();
            models.replaceAll((id, original) -> new NilumModelItemModel(
                    new NilumIconItemModel(original, iconAtlas, iconRenderer, glintRenderer),
                    modelStore, modelRenderer, glintRenderer, heldItemAnimations));

            event.getBakingResult().blockStateModels().replaceAll((state, original) ->
                    new NilumBlockStateModel(original, blockRegistry));
        });

        NilumCreativeTabs.register(modEventBus, iconAtlas, modelStore);

        modEventBus.addListener((RegisterGuiLayersEvent event) ->
                event.registerAboveAll(Identifier.fromNamespaceAndPath("nilum", "hud_atlas"),
                        new HudAtlasRenderer(hudAtlases, clientVars, fontStore, logger)));

        NilumKeybinds.register(modEventBus);

        NeoForge.EVENT_BUS.addListener((ChunkEvent.Unload event) ->
                blockRegistry.onChunkUnload(event.getChunk().getPos().x, event.getChunk().getPos().z));
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent.AfterEntities event) -> blockRenderer.render(event));

        modEventBus.addListener((RegisterClientPayloadHandlersEvent event) -> {
            event.register(NilumHelloPayload.TYPE, (payload, context) ->
                    handleHello(payload, context, logger, modVersion, assetCache, assetCacheRoot));
            event.register(NilumTcpOfferPayload.TYPE, (payload, context) -> handleTcpOffer(payload, logger, assetSync));
            event.register(NilumAssetManifestPayload.TYPE, (payload, context) -> {
                List<AssetManifestEntry> entries = AssetManifestPacket.decode(payload.data()).entries();
                logger.info("Received asset manifest from the server: " + entries.size() + " entries.");
                assetSync.onManifest(entries);
            });
            event.register(NilumItemDefinedAssetsPayload.TYPE, (payload, context) -> {
                ItemDefinedAssetsPacket packet = ItemDefinedAssetsPacket.decode(payload.data());
                NilumCreativeTabs.setItemDefinedAssets(packet.models(), packet.icons());
            });
            event.register(NilumModelSpawnPayload.TYPE, (payload, context) -> {
                ModelSpawnPacket spawn = ModelSpawnPacket.decode(payload.data());
                placements.put(spawn.entityId(), spawn.modelId());
            });
            event.register(NilumModListRequestPayload.TYPE, (payload, context) -> {
                List<ModEntry> mods = ModList.get().getMods().stream()
                        .map(info -> new ModEntry(info.getModId(), info.getVersion().toString()))
                        .toList();
                context.reply(new NilumModListPayload(new ModListPacket(mods).encode()));
            });
            event.register(NilumHudFramePayload.TYPE, (payload, context) -> {
                HudFramePacket packet = HudFramePacket.decode(payload.data());
                hudAtlases.onHudFrame(packet.atlasId(), packet.elementId(), packet.frame());
            });
            event.register(NilumAtlasPatchPayload.TYPE, (payload, context) -> {
                AtlasPatchPacket packet = AtlasPatchPacket.decode(payload.data());
                hudAtlases.onAtlasPatch(packet.atlasId(), packet.elementId(), packet.frame(), packet.png());
            });
            event.register(NilumHudFrameOverridePayload.TYPE, (payload, context) -> {
                HudFrameOverridePacket packet = HudFrameOverridePacket.decode(payload.data());
                hudAtlases.onOverride(packet.atlasId(), packet.elementId(), packet.frame(), packet.durationTicks());
            });
            event.register(NilumHudFrameReleasePayload.TYPE, (payload, context) -> {
                HudFrameReleasePacket packet = HudFrameReleasePacket.decode(payload.data());
                hudAtlases.onRelease(packet.atlasId(), packet.elementId());
            });
            event.register(NilumRegisterClientVarPayload.TYPE, (payload, context) -> {
                RegisterClientVarPacket packet = RegisterClientVarPacket.decode(payload.data());
                clientVars.register(packet.name(), packet.initialValue());
            });
            event.register(NilumSetClientVarPayload.TYPE, (payload, context) -> {
                SetClientVarPacket packet = SetClientVarPacket.decode(payload.data());
                clientVars.set(packet.name(), packet.value());
            });
            event.register(NilumSetHudTextPayload.TYPE, (payload, context) -> {
                SetHudTextPacket packet = SetHudTextPacket.decode(payload.data());
                hudAtlases.onHudText(packet.atlasId(), packet.elementId(), packet.text());
            });
            event.register(NilumOpenUiPayload.TYPE, (payload, context) -> {
                OpenUiPacket packet = OpenUiPacket.decode(payload.data());
                customUiStore.get(packet.uiId()).ifPresentOrElse(
                        ui -> Minecraft.getInstance().setScreen(new NilumCustomUiScreen(packet.uiId(), ui, logger, fontStore)),
                        () -> logger.warn("Server opened custom UI '" + packet.uiId()
                                + "' but it isn't cached on this client yet."));
            });
            event.register(NilumSetHudAtlasVisibilityPayload.TYPE, (payload, context) -> {
                SetHudAtlasVisibilityPacket packet = SetHudAtlasVisibilityPacket.decode(payload.data());
                hudAtlases.setAtlasVisible(packet.atlasId(), packet.visible());
            });
            event.register(NilumSetHudElementVisibilityPayload.TYPE, (payload, context) -> {
                SetHudElementVisibilityPacket packet = SetHudElementVisibilityPacket.decode(payload.data());
                hudAtlases.setElementVisible(packet.atlasId(), packet.elementId(), packet.visible());
            });
            event.register(NilumEntityAnimationPlayPayload.TYPE, (payload, context) -> {
                EntityAnimationPlayPacket packet = EntityAnimationPlayPacket.decode(payload.data());
                BbModel model = modelForEntity(modelStore, placements, packet.entityId());
                if (model != null) {
                    placements.animationState(packet.entityId())
                            .play(model, packet.animationName(), packet.startTimeMillis(), System.currentTimeMillis());
                }
            });
            event.register(NilumEntityAnimationStopPayload.TYPE, (payload, context) -> {
                EntityAnimationStopPacket packet = EntityAnimationStopPacket.decode(payload.data());
                BbModel model = modelForEntity(modelStore, placements, packet.entityId());
                if (model != null) {
                    placements.animationState(packet.entityId()).stop(model, System.currentTimeMillis());
                }
            });
            event.register(NilumBlockAnimationPlayPayload.TYPE, (payload, context) -> {
                BlockAnimationPlayPacket packet = BlockAnimationPlayPacket.decode(payload.data());
                BlockPos pos = new BlockPos(packet.x(), packet.y(), packet.z());
                BbModel model = modelForBlock(modelStore, blockRegistry, pos);
                if (model != null) {
                    blockRegistry.animationState(pos).play(model, packet.animationName(), packet.startTimeMillis(), System.currentTimeMillis());
                }
            });
            event.register(NilumBlockAnimationStopPayload.TYPE, (payload, context) -> {
                BlockAnimationStopPacket packet = BlockAnimationStopPacket.decode(payload.data());
                BlockPos pos = new BlockPos(packet.x(), packet.y(), packet.z());
                BbModel model = modelForBlock(modelStore, blockRegistry, pos);
                if (model != null) {
                    blockRegistry.animationState(pos).stop(model, System.currentTimeMillis());
                }
            });
            event.register(NilumItemAnimationPlayPayload.TYPE, (payload, context) -> {
                ItemAnimationPlayPacket packet = ItemAnimationPlayPacket.decode(payload.data());
                String modelId = heldItemAnimations.currentModelId(packet.holderId(), packet.rightHand());
                BbModel model = modelId == null ? null : modelStore.model(modelId).orElse(null);
                if (model != null) {
                    heldItemAnimations.get(packet.holderId(), packet.rightHand(), model)
                            .play(model, packet.animationName(), packet.startTimeMillis(), System.currentTimeMillis());
                }
            });
            event.register(NilumItemAnimationStopPayload.TYPE, (payload, context) -> {
                ItemAnimationStopPacket packet = ItemAnimationStopPacket.decode(payload.data());
                String modelId = heldItemAnimations.currentModelId(packet.holderId(), packet.rightHand());
                BbModel model = modelId == null ? null : modelStore.model(modelId).orElse(null);
                if (model != null) {
                    heldItemAnimations.get(packet.holderId(), packet.rightHand(), model).stop(model, System.currentTimeMillis());
                }
            });
            event.register(NilumChunkBlocksPayload.TYPE, (payload, context) -> {
                ChunkBlocksPacket packet = ChunkBlocksPacket.decode(payload.data());
                blockRegistry.apply(packet.entries());
            });
        });
    }

    private static BbModel modelForEntity(ClientModelStore modelStore, ClientModelPlacements placements, java.util.UUID entityId) {
        String modelId = placements.get(entityId);
        return modelId == null ? null : modelStore.model(modelId).orElse(null);
    }

    private static BbModel modelForBlock(ClientModelStore modelStore, ClientBlockRegistry blockRegistry, BlockPos pos) {
        Optional<String> wireOverlayModelId = blockRegistry.modelIdAt(pos);
        if (wireOverlayModelId.isPresent()) {
            return modelStore.model(wireOverlayModelId.get()).orElse(null);
        }

        // A real Nilum block from a NeoForge-hosted server isn't tracked in ClientBlockRegistry at
        // all, its model id lives on its own block entity.
        var level = Minecraft.getInstance().level;
        if (level != null && level.getBlockEntity(pos) instanceof NilumBlockEntity blockEntity) {
            return modelStore.model(blockEntity.definitionId()).orElse(null);
        }
        return null;
    }

    private static void handleHello(NilumHelloPayload payload, IPayloadContext context, NilumLogger logger, String modVersion,
                                     AssetCache assetCache, Path assetCacheRoot) {
        HelloPacket hello = HelloPacket.decode(payload.data());

        String serverId = ServerCacheId.sanitize(Minecraft.getInstance().getCurrentServer() == null
                ? null : Minecraft.getInstance().getCurrentServer().ip);
        assetCache.rebase(assetCacheRoot.resolve(serverId));

        if (SemanticVersions.isNewer(modVersion, hello.serverModVersion())) {
            logger.warn("This Nilum client (" + modVersion + ") is newer than the server ("
                    + hello.serverModVersion() + "), some features may not be available.");
        }

        ShaderCapability.Renderer shaderRenderer = ShaderCapability.detect();
        HelloAckPacket ack = new HelloAckPacket(
                "neoforge",
                modVersion,
                HandshakeProtocol.PROTOCOL_VERSION,
                true,
                false,
                shaderRenderer.wireName()
        );

        // context.reply() works regardless of phase; ClientPacketDistributor doesn't, it
        // requires Minecraft.getInstance().getConnection(), which is null during configuration.
        context.reply(new NilumHelloAckPayload(ack.encode()));
        logger.info("Nilum server detected (server " + hello.serverModVersion()
                + ", client " + modVersion + "), handshake acknowledged.");
    }

    private static void handleTcpOffer(NilumTcpOfferPayload payload, NilumLogger logger, AssetSyncSession assetSync) {
        TcpOfferPacket offer = TcpOfferPacket.decode(payload.data());
        logger.info("Received TCP side-channel offer from the server: " + offer.host() + ":" + offer.port() + ".");

        // NilumTcpClient.connect blocks until success/timeout; never call it on the
        // network callback thread, that would stall all other packet handling.
        Thread.ofVirtual().start(() -> {
            Socket socket = NilumTcpClient.connect(offer.host(), offer.port(), offer.token(),
                    TCP_CONNECT_TIMEOUT_MILLIS, logger);

            if (socket != null) {
                logger.info("TCP side-channel connected to " + offer.host() + ":" + offer.port() + ".");
                assetSync.onTcpConnected(socket);
            } else {
                logger.warn("Falling back to plugin-channel transfer for this session.");
                ClientPacketDistributor.sendToServer(
                        new NilumTcpUnavailablePayload(new TcpUnavailablePacket().encode()));
            }
        });
    }
}
