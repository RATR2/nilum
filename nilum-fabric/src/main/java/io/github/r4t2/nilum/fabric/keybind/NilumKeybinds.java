package io.github.r4t2.nilum.fabric.keybind;

import io.github.r4t2.nilum.common.protocol.KeybindPacket;
import io.github.r4t2.nilum.fabric.network.NilumKeybindPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** 4 general-purpose, unbound-by-default keybinds a server can turn into abilities. Polled once per client tick for press/release edges. */
public final class NilumKeybinds {

    private static final int SLOT_COUNT = 4;
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("nilum", "keybinds"));

    private static final KeyMapping[] BINDINGS = new KeyMapping[SLOT_COUNT];
    private static final boolean[] wasDown = new boolean[SLOT_COUNT];

    private NilumKeybinds() {
    }

    public static void register() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            BINDINGS[i] = KeyBindingHelper.registerKeyBinding(
                    new KeyMapping("key.nilum.keybind_" + (i + 1), GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
        }

        ClientTickEvents.END_CLIENT_TICK.register(NilumKeybinds::tick);
    }

    private static void tick(net.minecraft.client.Minecraft client) {
        if (client.player == null || !ClientPlayNetworking.canSend(NilumKeybindPayload.TYPE)) {
            return;
        }

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            boolean isDown = BINDINGS[slot].isDown();
            if (isDown != wasDown[slot]) {
                wasDown[slot] = isDown;
                ClientPlayNetworking.send(new NilumKeybindPayload(new KeybindPacket(slot, isDown).encode()));
            }
        }
    }
}
