package io.github.r4t2.nilum.fabric.debug;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Unbound-by-default debug keybind that opens NilumHandTuneScreen. */
public final class HandTuneKeybind {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("nilum", "debug"));

    private static KeyMapping binding;

    private HandTuneKeybind() {
    }

    public static void register() {
        binding = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.nilum.hand_tune", GLFW.GLFW_KEY_UNKNOWN, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (binding.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new NilumHandTuneScreen());
                }
            }
        });
    }
}
