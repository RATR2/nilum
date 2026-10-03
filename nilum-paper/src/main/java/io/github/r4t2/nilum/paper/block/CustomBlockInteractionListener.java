package io.github.r4t2.nilum.paper.block;

import io.github.r4t2.nilum.paper.NilumPlugin;
import io.github.r4t2.nilum.paper.item.DropEntry;
import io.github.r4t2.nilum.paper.item.ItemDefinitionRegistry;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps CustomBlockRegistry honest against every way a Nilum block can be destroyed, applies drops, enforces exact break timing. */
public final class CustomBlockInteractionListener implements Listener {

    private final NilumPlugin plugin;
    private final CustomBlockRegistry registry;
    private final ItemDefinitionRegistry itemDefinitions;
    private final Random random = new Random();

    /** Digging-start timestamp per (player, position); only tracked for BlockProxy.Custom blocks. */
    private final Map<String, Long> diggingStartMillis = new ConcurrentHashMap<>();
    private final Map<String, BukkitTask> breakTasks = new ConcurrentHashMap<>();
    /** Keys currently mid-finishBreak(), so onBlockBreak can tell our own forced completion apart from a natural one to reject. */
    private final Set<String> forcedCompletionKeys = ConcurrentHashMap.newKeySet();

    public CustomBlockInteractionListener(NilumPlugin plugin, CustomBlockRegistry registry, ItemDefinitionRegistry itemDefinitions) {
        this.plugin = plugin;
        this.registry = registry;
        this.itemDefinitions = itemDefinitions;
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            // Creative should break these exactly like any other block: instant, no waiting.
            // The break-time enforcement below only applies outside creative.
            return;
        }
        Location location = event.getBlock().getLocation();
        registry.definitionAt(location).ifPresent(definition -> {
            if (definition.proxy() instanceof BlockProxy.Custom custom) {
                // Deny insta-break for Custom-mode blocks; vanilla's own instant destroy would
                // otherwise bypass breakTimeSeconds entirely.
                event.setInstaBreak(false);
                String key = diggingKey(event.getPlayer(), location);
                // putIfAbsent, not put: BlockDamageEvent fires every damage tick while the mouse is
                // held, so overwriting the start time each tick would reset elapsed time to ~0 and
                // the break would never complete.
                if (diggingStartMillis.putIfAbsent(key, System.currentTimeMillis()) == null) {
                    startBreakTask(key, event.getPlayer(), location, definition, custom);
                }
            }
        });
    }

    /**
     * Drives the crack overlay directly (Player.sendBlockDamage) instead of trusting vanilla's own
     * hardness-based rate, and completes the break itself at exactly breakTimeSeconds regardless of
     * how fast or slow the wire block's real hardness would otherwise finish it.
     */
    private void startBreakTask(String key, Player player, Location location, BlockDefinition definition, BlockProxy.Custom custom) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Long startedAt = diggingStartMillis.get(key);
            if (startedAt == null) {
                return;
            }
            long elapsedMillis = System.currentTimeMillis() - startedAt;
            float progress = (float) Math.min(1.0, elapsedMillis / (custom.breakTimeSeconds() * 1000));
            player.sendBlockDamage(location, progress);
            if (progress >= 1.0F) {
                finishBreak(key, player, location, definition, custom);
            }
        }, 0L, 1L);
        breakTasks.put(key, task);
    }

    @EventHandler
    public void onBlockDamageAbort(BlockDamageAbortEvent event) {
        String key = diggingKey(event.getPlayer(), event.getBlock().getLocation());
        diggingStartMillis.remove(key);
        BukkitTask task = breakTasks.remove(key);
        if (task != null) {
            task.cancel();
            event.getPlayer().sendBlockDamage(event.getBlock().getLocation(), 0F);
        }
    }

    /** Completes a Custom-mode block's break once our own timer (not vanilla's) says breakTimeSeconds has elapsed. */
    private void finishBreak(String key, Player player, Location location, BlockDefinition definition, BlockProxy.Custom custom) {
        BukkitTask task = breakTasks.remove(key);
        if (task != null) {
            task.cancel();
        }
        diggingStartMillis.remove(key);

        BlockBreakEvent event = new BlockBreakEvent(location.getBlock(), player);
        event.setExpToDrop(custom.xpPerBreak());
        forcedCompletionKeys.add(key);
        try {
            plugin.getServer().getPluginManager().callEvent(event);
        } finally {
            forcedCompletionKeys.remove(key);
        }
        if (event.isCancelled()) {
            player.sendBlockDamage(location, 0F);
            return;
        }

        location.getBlock().setType(Material.AIR);
        rollAndDropItems(definition, location);
        registry.forget(location);
        CustomBlockBroadcaster.broadcastRemoval(plugin, location);

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!tool.getType().isAir()) {
            player.damageItemStack(EquipmentSlot.HAND, 1);
        }
        if (event.getExpToDrop() > 0) {
            location.getWorld().spawn(location.toCenterLocation(), ExperienceOrb.class,
                    orb -> orb.setExperience(event.getExpToDrop()));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        var definitionOpt = registry.definitionAt(location);
        if (definitionOpt.isEmpty()) {
            return;
        }
        BlockDefinition definition = definitionOpt.get();

        if (definition.proxy() instanceof BlockProxy.Custom && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            String key = diggingKey(event.getPlayer(), location);
            if (!forcedCompletionKeys.contains(key)) {
                // Not our own forced completion (see finishBreak): a natural vanilla-timed break never
                // completes a Custom-mode block anymore, since our scheduled task is now the sole
                // authority on when breakTimeSeconds has actually elapsed.
                event.setCancelled(true);
            }
            // Either cancelled above, or this is our own forced completion, whose removal, drops,
            // durability, and XP already happened directly in finishBreak().
            return;
        }

        if (!definition.drops().isEmpty()) {
            event.setDropItems(false);
            rollAndDropItems(definition, location);
        }
        registry.forget(location);
        CustomBlockBroadcaster.broadcastRemoval(plugin, location);
    }

    private void rollAndDropItems(BlockDefinition definition, Location location) {
        for (DropEntry dropEntry : definition.drops()) {
            for (ItemStack stack : dropEntry.roll(itemDefinitions, random)) {
                location.getWorld().dropItemNaturally(location, stack);
            }
        }
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
