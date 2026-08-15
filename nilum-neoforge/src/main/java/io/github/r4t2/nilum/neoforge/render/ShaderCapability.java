package io.github.r4t2.nilum.neoforge.render;

import net.neoforged.fml.ModList;

/** Iris/vanilla detection for the handshake capability report. No Sodium-family renderer exists on NeoForge to detect. */
public final class ShaderCapability {

    public enum Renderer {
        IRIS("iris"),
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
        return ModList.get().isLoaded("iris") ? Renderer.IRIS : Renderer.VANILLA;
    }
}
