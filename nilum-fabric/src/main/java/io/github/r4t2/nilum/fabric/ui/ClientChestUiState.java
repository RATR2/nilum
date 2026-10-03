package io.github.r4t2.nilum.fabric.ui;

import net.minecraft.resources.Identifier;

/** Bridges "the server just told us to open chest UI X" to ContainerScreenMixin, which renders its background. */
public final class ClientChestUiState {

    private static Identifier pendingBackground;

    private ClientChestUiState() {
    }

    public static void setPending(Identifier backgroundTextureId) {
        pendingBackground = backgroundTextureId;
    }

    /** Consumed once by ContainerScreenMixin's constructor injection; null after the first call. */
    public static Identifier consumePending() {
        Identifier id = pendingBackground;
        pendingBackground = null;
        return id;
    }
}
