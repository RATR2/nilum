package io.github.r4t2.nilum.fabric;

import io.github.r4t2.nilum.fabric.handshake.FabricServerHandshake;
import net.fabricmc.api.DedicatedServerModInitializer;

/**
 * Runs only when this instance is an actual Fabric dedicated server, never on a physical
 * client, unlike the common "main" entrypoint.
 */
public final class NilumFabricDedicatedServer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        FabricServerHandshake.register(NilumFabricMod.CONFIG, NilumFabricMod.LOGGER);
    }
}
