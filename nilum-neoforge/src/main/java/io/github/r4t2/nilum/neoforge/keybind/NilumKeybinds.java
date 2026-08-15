package io.github.r4t2.nilum.neoforge.keybind;

import io.github.r4t2.nilum.common.protocol.KeybindPacket;
import io.github.r4t2.nilum.neoforge.network.NilumKeybindPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
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

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.registerCategory(CATEGORY);
            for (int i = 0; i < SLOT_COUNT; i++) {
                BINDINGS[i] = new KeyMapping("key.nilum.keybind_" + (i + 1), GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
                event.register(BINDINGS[i]);
            }
        });

        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
    }

    private static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) {
            return;
        }

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            boolean isDown = BINDINGS[slot].isDown();
            if (isDown != wasDown[slot]) {
                wasDown[slot] = isDown;
                ClientPacketDistributor.sendToServer(new NilumKeybindPayload(new KeybindPacket(slot, isDown).encode()));
            }
        }
    }
}
