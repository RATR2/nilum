package io.github.r4t2.nilum.fabric.render;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Iris/Sodium/vanilla detection for the handshake capability report. Iris present: full
 * pipeline access. Sodium present (no Iris): restricted to the Sodium-safe set. Otherwise
 * vanilla: full core shader access.
 */
public final class ShaderCapability {

    public enum Renderer {
        IRIS("iris"),
        SODIUM_SAFE("sodium_safe"),
        VANILLA("vanilla");

        private final String wireName;

        Renderer(String wireName) {
            this.wireName = wireName;
        }

        public String wireName() {
            return wireName;
        }
    }

    private ShaderCapability() {
    }

    public static Renderer detect() {
        FabricLoader loader = FabricLoader.getInstance();
        if (loader.isModLoaded("iris")) {
            return Renderer.IRIS;
        }
        if (loader.isModLoaded("sodium")) {
            return Renderer.SODIUM_SAFE;
        }
        return Renderer.VANILLA;
    }
}
