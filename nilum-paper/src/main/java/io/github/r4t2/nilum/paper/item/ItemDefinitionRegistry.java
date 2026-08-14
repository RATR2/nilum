package io.github.r4t2.nilum.paper.item;

import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.paper.NilumKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/** Per-file item templates (items/<id>.yml). Resolving one builds an ItemStack with display name, lore, and enchantments layered on. */
public final class ItemDefinitionRegistry {

    private final NilumLogger logger;
    private final CustomItemService customItemService;
    private final IconItemService iconItemService;

    private final Map<String, ItemDefinition> definitionsById = new ConcurrentHashMap<>();

    public ItemDefinitionRegistry(NilumLogger logger, CustomItemService customItemService, IconItemService iconItemService) {
        this.logger = logger;
        this.customItemService = customItemService;
        this.iconItemService = iconItemService;
    }

    public void loadDirectory(Path directory) throws IOException {
        definitionsById.clear();
        Files.createDirectories(directory);

        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".yml")).toList()) {
                String id = stripExtension(file.getFileName().toString());
                try {
                    definitionsById.put(id, parse(id, YamlConfiguration.loadConfiguration(file.toFile())));
                } catch (RuntimeException e) {
                    logger.warn("Failed to load item definition '" + id + "': " + e);
                }
            }
        }
    }

    public Set<String> itemIds() {
        return Set.copyOf(definitionsById.keySet());
    }

    /** Builds a fresh ItemStack for this definition, or empty if the id isn't loaded (or its underlying model/icon isn't). */
    public Optional<ItemStack> resolve(String id) {
        ItemDefinition definition = definitionsById.get(id);
        if (definition == null) {
            return Optional.empty();
        }

        Optional<ItemStack> base = switch (definition.base()) {
            case ItemBase.Vanilla vanilla -> Optional.of(new ItemStack(vanilla.material()));
            case ItemBase.Model model -> customItemService.createItem(model.modelId(), model.baseMaterial());
            case ItemBase.Icon icon -> iconItemService.createItem(icon.iconId(), icon.baseMaterial());
        };
        if (base.isEmpty()) {
            return Optional.empty();
        }

        ItemStack item = base.get();
        ItemMeta meta = item.getItemMeta();

        definition.displayName().ifPresent(name ->
                meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name)));

        if (!definition.lore().isEmpty()) {
            List<Component> lore = definition.lore().stream()
                    .map(line -> (Component) LegacyComponentSerializer.legacyAmpersand().deserialize(line))
                    .toList();
            meta.lore(lore);
        }

        for (Map.Entry<String, Integer> enchant : definition.enchantments().entrySet()) {
            Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.fromString(enchant.getKey()));
            if (enchantment == null) {
                logger.warn("Item definition '" + id + "' references unknown enchantment '" + enchant.getKey() + "'.");
                continue;
            }
            meta.addEnchant(enchantment, enchant.getValue(), true);
        }

        definition.glint().ifPresent(glint -> {
            var pdc = meta.getPersistentDataContainer();
            pdc.set(NilumKeys.GLINT_COLOR, PersistentDataType.STRING, String.format("%06X", glint.colorRgb()));
            pdc.set(NilumKeys.GLINT_INTENSITY, PersistentDataType.STRING, Float.toString(glint.intensity()));
            pdc.set(NilumKeys.GLINT_SPEED, PersistentDataType.STRING, Float.toString(glint.speed()));
            if (glint.textureIconId() != null) {
                pdc.set(NilumKeys.GLINT_TEXTURE, PersistentDataType.STRING, glint.textureIconId());
            }
            // Suppress vanilla's own foil at the source via the real supported API, rather than
            // fighting the client's private per-layer state. A custom-glinted item should never
            // show both vanilla's shimmer and ours stacked together, even if it's also genuinely
            // enchanted.
            meta.setEnchantmentGlintOverride(false);
        });

        item.setItemMeta(meta);
        return Optional.of(item);
    }

    private ItemDefinition parse(String id, ConfigurationSection section) {
        ItemBase base = parseBase(id, section);

        Optional<String> displayName = section.isSet("name") ? Optional.of(section.getString("name")) : Optional.empty();
        List<String> lore = section.getStringList("lore");

        Map<String, Integer> enchantments = new HashMap<>();
        ConfigurationSection enchantSection = section.getConfigurationSection("enchantments");
        if (enchantSection != null) {
            for (String key : enchantSection.getKeys(false)) {
                enchantments.put(key, enchantSection.getInt(key));
            }
        }

        Optional<GlintDefinition> glint = parseGlint(id, section.getConfigurationSection("glint"));

        return new ItemDefinition(id, base, displayName, lore, enchantments, glint);
    }

    private Optional<GlintDefinition> parseGlint(String id, ConfigurationSection glintSection) {
        if (glintSection == null) {
            return Optional.empty();
        }

        // Optional; null means "use Minecraft's own enchanted_glint_item.png", recolored below,
        // so admins can recolor the familiar glint without authoring a texture of their own.
        String texture = glintSection.getString("texture");
        if (texture != null && texture.isBlank()) {
            texture = null;
        }

        String colorHex = glintSection.getString("color", "#FFFFFF");
        int colorRgb;
        try {
            colorRgb = GlintDefinition.parseColor(colorHex);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("item definition '" + id + "' has an invalid glint 'color' '" + colorHex + "'");
        }

        float intensity = (float) glintSection.getDouble("intensity", 1.0);
        float speed = (float) glintSection.getDouble("speed", 1.0);

        return Optional.of(new GlintDefinition(colorRgb, intensity, speed, texture));
    }

    private ItemBase parseBase(String id, ConfigurationSection section) {
        String base = section.getString("base");
        if (base == null || base.isBlank()) {
            throw new IllegalArgumentException("missing required 'base' field");
        }

        Material baseMaterial = Optional.ofNullable(section.getString("base_material"))
                .map(Material::matchMaterial)
                .orElse(Material.PAPER);

        if (base.startsWith("nilum:model:")) {
            return new ItemBase.Model(base.substring("nilum:model:".length()), baseMaterial);
        }
        if (base.startsWith("nilum:icon:")) {
            return new ItemBase.Icon(base.substring("nilum:icon:".length()), baseMaterial);
        }

        Material material = Material.matchMaterial(base);
        if (material == null) {
            throw new IllegalArgumentException("item definition '" + id + "' has an unrecognized base '" + base + "'");
        }
        return new ItemBase.Vanilla(material);
    }

    private static String stripExtension(String fileName) {
        return fileName.substring(0, fileName.length() - ".yml".length());
    }
}
