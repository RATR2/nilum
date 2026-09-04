package io.github.r4t2.nilum.api;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Stable entry point for other plugins, via Bukkit.getServicesManager().load(NilumAPI.class). Trusts the caller; no permission checks. */
public interface NilumAPI {

    /** Spawns an anchor entity for modelId at location. Empty if modelId isn't a loaded model. */
    Optional<UUID> placeModel(Location location, String modelId);

    /** A custom item wrapping a full model, based on baseMaterial. Empty if modelId isn't a loaded model. */
    Optional<ItemStack> createModelItem(String modelId, Material baseMaterial);

    /** An icon-only custom item, based on baseMaterial. Empty if iconId isn't a loaded icon. */
    Optional<ItemStack> createIconItem(String iconId, Material baseMaterial);

    /** A fully defined item (name/lore/enchants included) from items/&lt;id&gt;.yml. Empty if id isn't defined or its model/icon isn't loaded. */
    Optional<ItemStack> createDefinedItem(String id);

    /** Places a real, rendered custom block, returning the model id it renders. Empty if blockTypeId isn't a loaded block type. */
    Optional<String> placeBlock(Location location, String blockTypeId);

    /** Removes a Nilum block, returning the block type id that was there. Empty if there wasn't one. */
    Optional<String> removeBlock(Location location);

    /** Plays a named animation on a player skeleton or placed model entity, honoring its authored loop mode. */
    void playEntityAnimation(UUID entityId, String animationName);

    /** Stops a triggered entity animation, back to its rest pose. */
    void stopEntityAnimation(UUID entityId);

    /** Plays a named animation on a Nilum block at location. */
    void playBlockAnimation(Location location, String animationName);

    /** Stops a triggered block animation, back to its rest pose. */
    void stopBlockAnimation(Location location);

    /** Plays a named animation on whatever Nilum item player is holding in mainHand (or their off hand). */
    void playHeldItemAnimation(Player player, boolean mainHand, String animationName);

    /** Stops a triggered held-item animation, back to its rest pose. */
    void stopHeldItemAnimation(Player player, boolean mainHand);

    /** Pushes a HUD atlas frame update to player. */
    void setHudFrame(Player player, String atlasId, String elementId, int frame);

    /** Shows or hides an entire HUD atlas for player. */
    void setHudAtlasVisible(Player player, String atlasId, boolean visible);

    /** Shows or hides one HUD atlas element for player. */
    void setHudElementVisible(Player player, String atlasId, String elementId, boolean visible);

    /** Switches player's client to a Nilum shaderpack. Needs Iris on their client. */
    void activateShaderPack(Player player, String packId);

    /** Restores player's client to its previous shaderpack state. */
    void deactivateShaderPack(Player player);

    /** Opens a custom UI for player. False if uiId isn't a loaded custom UI. */
    boolean openCustomUi(Player player, String uiId);

    /** The custom UI id currently open for player, if any. */
    Optional<String> openCustomUiFor(Player player);

    Set<String> modelIds();

    Set<String> iconIds();

    Set<String> blockTypeIds();

    Set<String> itemDefinitionIds();

    Set<String> shaderPackIds();

    Set<String> uiIds();
}
