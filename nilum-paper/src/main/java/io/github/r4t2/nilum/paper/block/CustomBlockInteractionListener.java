package io.github.r4t2.nilum.paper.block;

import io.github.r4t2.nilum.paper.NilumPlugin;
import io.github.r4t2.nilum.paper.item.DropEntry;
import io.github.r4t2.nilum.paper.item.ItemDefinitionRegistry;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps CustomBlockRegistry honest against every way a Nilum block can be destroyed, applies drops, approximates break timing. */
public final class CustomBlockInteractionListener implements Listener {

    private final NilumPlugin plugin;
    private final CustomBlockRegistry registry;
    private final ItemDefinitionRegistry itemDefinitions;
    private final Random random = new Random();

    /** Digging-start timestamp per (player, position); only tracked for BlockProxy.Custom blocks. */
    private final Map<String, Long> diggingStartMillis = new ConcurrentHashMap<>();

    public CustomBlockInteractionListener(NilumPlugin plugin, CustomBlockRegistry registry, ItemDefinitionRegistry itemDefinitions) {
        this.plugin = plugin;
        this.registry = registry;
        this.itemDefinitions = itemDefinitions;
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            // Creative should break these exactly like any other block: instant, no waiting.
            // The approximate break-time enforcement below only applies outside creative.
            return;
        }
        registry.definitionAt(event.getBlock().getLocation()).ifPresent(definition -> {
            if (definition.proxy() instanceof BlockProxy.Custom) {
                // Deny insta-break for Custom-mode blocks; vanilla's own instant destroy would
                // otherwise bypass breakTimeSeconds entirely.
                event.setInstaBreak(false);
                // putIfAbsent, not put: BlockDamageEvent fires on every damage tick while the
                // player holds the mouse down, so overwriting the start time each tick would
                // keep resetting elapsed time back to ~0 and the break would never complete.
                diggingStartMillis.putIfAbsent(diggingKey(event.getPlayer(), event.getBlock().getLocation()), System.currentTimeMillis());
            }
        });
    }

    @EventHandler
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        diggingStartMillis.remove(diggingKey(event.getPlayer(), event.getBlock().getLocation()));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        var definition = registry.definitionAt(location);
        if (definition.isEmpty()) {
            return;
        }

        if (definition.get().proxy() instanceof BlockProxy.Custom custom
                && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            String key = diggingKey(event.getPlayer(), location);
            Long startedAt = diggingStartMillis.get(key);
            long elapsedMillis = startedAt == null ? 0 : System.currentTimeMillis() - startedAt;
            if (elapsedMillis < (long) (custom.breakTimeSeconds() * 1000)) {
                // Too early; vanilla's own real-hardness timing completed faster than our
                // configured duration. Deny it; the player has to keep holding.
                event.setCancelled(true);
                return;
            }
            diggingStartMillis.remove(key);
        }

        if (!definition.get().drops().isEmpty()) {
            event.setDropItems(false);
            for (DropEntry dropEntry : definition.get().drops()) {
                for (ItemStack stack : dropEntry.roll(itemDefinitions, random)) {
                    location.getWorld().dropItemNaturally(location, stack);
                }
            }
        }

        registry.forget(location);
        CustomBlockBroadcaster.broadcastRemoval(plugin, location);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> isExplosionResistant(block.getLocation()));
        event.blockList().forEach(this::cleanUpExploded);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> isExplosionResistant(block.getLocation()));
        event.blockList().forEach(this::cleanUpExploded);
    }

    private boolean isExplosionResistant(Location location) {
        return registry.definitionAt(location)
                .map(definition -> definition.proxy() instanceof BlockProxy.Custom custom && custom.explosionResistant())
                .orElse(false);
    }

    /** Explosions don't roll configured drops (matching vanilla's own reduced-drop-on-explosion convention); just registry sync. */
    private void cleanUpExploded(org.bukkit.block.Block block) {
        Location location = block.getLocation();
        if (registry.forget(location).isPresent()) {
            CustomBlockBroadcaster.broadcastRemoval(plugin, location);
        }
    }

    private static String diggingKey(Player player, Location location) {
        UUID playerId = player.getUniqueId();
        return playerId + "@" + location.getWorld().getName() + ":"
                + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }
}
