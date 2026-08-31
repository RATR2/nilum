package io.github.r4t2.nilum.paper.api;

import io.github.r4t2.nilum.api.NilumAPI;
import io.github.r4t2.nilum.paper.NilumPlugin;
import io.github.r4t2.nilum.paper.block.CustomBlockBroadcaster;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Thin wrapper delegating straight to NilumPlugin's own services, kept separate from the interface so its shape can stay stable. */
public final class NilumAPIImpl implements NilumAPI {

    private final NilumPlugin plugin;

    public NilumAPIImpl(NilumPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Optional<UUID> placeModel(Location location, String modelId) {
        return plugin.modelDisplays().place(location, modelId);
    }

    @Override
    public Optional<ItemStack> createModelItem(String modelId, Material baseMaterial) {
        return plugin.customItems().createItem(modelId, baseMaterial);
    }

    @Override
    public Optional<ItemStack> createIconItem(String iconId, Material baseMaterial) {
        return plugin.iconItems().createItem(iconId, baseMaterial);
    }

    @Override
    public Optional<ItemStack> createDefinedItem(String id) {
        return plugin.items().resolve(id);
    }

    @Override
    public Optional<String> placeBlock(Location location, String blockTypeId) {
        return plugin.customBlocks().place(location, blockTypeId).map(definition -> {
            CustomBlockBroadcaster.broadcastPlacement(plugin, location, definition.modelId());
            return definition.modelId();
        });
    }

    @Override
    public Optional<String> removeBlock(Location location) {
        return plugin.customBlocks().remove(location).map(blockTypeId -> {
            CustomBlockBroadcaster.broadcastRemoval(plugin, location);
            return blockTypeId;
        });
    }

    @Override
    public void playEntityAnimation(UUID entityId, String animationName) {
        plugin.animations().playEntityAnimation(entityId, animationName);
    }

    @Override
    public void stopEntityAnimation(UUID entityId) {
        plugin.animations().stopEntityAnimation(entityId);
    }

    @Override
    public void playBlockAnimation(Location location, String animationName) {
        plugin.animations().playBlockAnimation(location, animationName);
    }

    @Override
    public void stopBlockAnimation(Location location) {
        plugin.animations().stopBlockAnimation(location);
    }

    @Override
    public void setHudFrame(Player player, String atlasId, String elementId, int frame) {
        plugin.hud().setHudFrame(player, atlasId, elementId, frame);
    }

    @Override
    public void setHudAtlasVisible(Player player, String atlasId, boolean visible) {
        plugin.hud().setAtlasVisible(player, atlasId, visible);
    }

    @Override
    public void setHudElementVisible(Player player, String atlasId, String elementId, boolean visible) {
        plugin.hud().setElementVisible(player, atlasId, elementId, visible);
    }

    @Override
    public void activateShaderPack(Player player, String packId) {
        plugin.shaderPackService().activate(player, packId);
    }

    @Override
    public void deactivateShaderPack(Player player) {
        plugin.shaderPackService().deactivate(player);
    }

    @Override
    public boolean openCustomUi(Player player, String uiId) {
        return plugin.uiSessions().open(player, uiId);
    }

    @Override
    public Optional<String> openCustomUiFor(Player player) {
        return plugin.uiSessions().openUiFor(player);
    }

    @Override
    public Set<String> modelIds() {
        return plugin.models().modelIds();
    }

    @Override
    public Set<String> iconIds() {
        return plugin.icons().iconIds();
    }

    @Override
    public Set<String> blockTypeIds() {
        return plugin.blockDefinitions().blockIds();
    }

    @Override
    public Set<String> itemDefinitionIds() {
        return plugin.items().itemIds();
    }

    @Override
    public Set<String> shaderPackIds() {
        return plugin.shaderPacks().packIds();
    }

    @Override
    public Set<String> uiIds() {
        return plugin.uis().uiIds();
    }
}
