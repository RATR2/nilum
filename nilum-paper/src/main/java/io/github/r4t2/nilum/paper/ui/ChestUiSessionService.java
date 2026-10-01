package io.github.r4t2.nilum.paper.ui;

import io.github.r4t2.nilum.common.expr.ExprEvaluator;
import io.github.r4t2.nilum.common.expr.ExprParser;
import io.github.r4t2.nilum.common.expr.ValueSource;
import io.github.r4t2.nilum.common.protocol.NilumChannels;
import io.github.r4t2.nilum.common.protocol.OpenChestUiPacket;
import io.github.r4t2.nilum.common.ui.ChestUiDescriptor;
import io.github.r4t2.nilum.common.ui.ChestUiElement;
import io.github.r4t2.nilum.paper.NilumPlugin;
import io.github.r4t2.nilum.paper.event.NilumUiCloseEvent;
import io.github.r4t2.nilum.paper.hud.NilumJavaValueSource;
import io.github.r4t2.nilum.paper.hud.PlaceholderApiValueSource;
import io.github.r4t2.nilum.paper.hud.SkriptVariableValueSource;
import io.github.r4t2.nilum.paper.skript.NilumSkriptVariables;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Opens a real Bukkit chest inventory for a Nilum chest UI, populating its configured slots. */
public final class ChestUiSessionService {

    private final NilumPlugin plugin;
    private final Map<UUID, NilumChestUiHolder> openByPlayer = new ConcurrentHashMap<>();

    public ChestUiSessionService(NilumPlugin plugin) {
        this.plugin = plugin;
    }

    /** @return false if uiId isn't a loaded chest UI, or the player hasn't completed the Nilum handshake. */
    public boolean open(Player player, String uiId) {
        ChestUiDescriptor descriptor = plugin.chestUis().descriptor(uiId).orElse(null);
        if (descriptor == null || !plugin.handshakes().hasClient(player.getUniqueId())) {
            return false;
        }

        NilumChestUiHolder holder = new NilumChestUiHolder(uiId, descriptor);
        Component title = LegacyComponentSerializer.legacyAmpersand().deserialize(descriptor.title());
        Inventory inventory = Bukkit.createInventory(holder, descriptor.rows() * 9, title);
        holder.setInventory(inventory);

        ValueSource valueSource = buildValueSource(player);
        for (ChestUiElement element : descriptor.elements().values()) {
            if (passesRequirement(element, valueSource)) {
                inventory.setItem(element.slot(), buildItemStack(element));
            }
        }

        openByPlayer.put(player.getUniqueId(), holder);
        player.sendPluginMessage(plugin, NilumChannels.OPEN_CHEST_UI_QUALIFIED, new OpenChestUiPacket(uiId).encode());
        player.openInventory(inventory);
        return true;
    }

    public Optional<NilumChestUiHolder> openFor(Player player) {
        return Optional.ofNullable(openByPlayer.get(player.getUniqueId()));
    }

    public void onClosed(Player player) {
        NilumChestUiHolder holder = openByPlayer.remove(player.getUniqueId());
        if (holder != null) {
            plugin.getServer().getPluginManager().callEvent(new NilumUiCloseEvent(player, holder.uiId()));
        }
    }

    private boolean passesRequirement(ChestUiElement element, ValueSource valueSource) {
        if (element.requirement().isEmpty()) {
            return true;
        }
        try {
            double timeSeconds = System.nanoTime() / 1_000_000_000.0;
            return ExprEvaluator.evaluate(ExprParser.parse(element.requirement().get()), valueSource, timeSeconds) != 0;
        } catch (RuntimeException e) {
            return true;
        }
    }

    private ValueSource buildValueSource(Player player) {
        boolean placeholderApiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        ValueSource valueSource = placeholderApiAvailable
                ? new PlaceholderApiValueSource(player) : new NilumJavaValueSource(player);
        if (NilumSkriptVariables.isAvailable()) {
            ValueSource base = valueSource;
            return (function, key) -> function.equals("skriptvar")
                    ? SkriptVariableValueSource.resolveNumeric(key, player) : base.resolve(function, key);
        }
        return valueSource;
    }

    private ItemStack buildItemStack(ChestUiElement element) {
        Material material = Material.matchMaterial(element.material());
        ItemStack stack = new ItemStack(material != null ? material : Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        element.name().ifPresent(name ->
                meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name)));
        if (!element.lore().isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : element.lore()) {
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
            }
            meta.lore(lore);
        }
        stack.setItemMeta(meta);
        return stack;
    }
}
