package io.github.r4t2.nilum.fabric;

import io.github.r4t2.nilum.common.hosting.NilumAssetHost;
import io.github.r4t2.nilum.fabric.handshake.FabricServerHandshake;
import io.github.r4t2.nilum.fabric.server.NilumFabricServerModels;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Runs only when this instance is an actual Fabric dedicated server, never on a physical
 * client, unlike the common "main" entrypoint.
 */
public final class NilumFabricDedicatedServer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        NilumAssetHost assetHost = new NilumAssetHost(
                FabricLoader.getInstance().getConfigDir().resolve("nilum"), NilumFabricMod.LOGGER);
        assetHost.loadAll();
        NilumFabricMod.LOGGER.info("Loaded " + assetHost.models().modelIds().size() + " model(s), "
                + assetHost.icons().iconIds().size() + " icon(s), "
                + assetHost.hudAtlases().atlasIds().size() + " HUD atlas(es), "
                + assetHost.shaderPacks().packIds().size() + " shader pack(s), "
                + assetHost.fonts().fontIds().size() + " font(s) for this Fabric-hosted server.");

        FabricServerHandshake.register(NilumFabricMod.CONFIG, NilumFabricMod.LOGGER, assetHost);
        NilumFabricServerModels.register(assetHost, NilumFabricMod.LOGGER);
    }
}
