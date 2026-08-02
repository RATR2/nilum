package io.github.r4t2.nilum.paper.icon;

import io.github.r4t2.nilum.common.icon.IconDisplay;
import io.github.r4t2.nilum.common.icon.IconTransform;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.model.BbDisplayTransform;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.BbVector3;
import io.github.r4t2.nilum.common.model.ModelRegistry;
import io.github.r4t2.nilum.common.model.VanillaGeneratedDisplay;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Owns each icon's icons/<iconId>.yml: which texture file it uses and how it's
 * positioned/scaled/rotated per ItemDisplayContext. Auto-creates missing files/fields
 * on reload without touching values an admin already set.
 *
 * <p>Each context field is either "generated" (vanilla's flat-item default),
 * "blockbench" (pulled from blockbench-reference), or a literal {x, y, z}.
 */
public final class IconsYamlManager {

    private final Path iconsDirectory;
    private final NilumLogger logger;

    public IconsYamlManager(Path iconsDirectory, NilumLogger logger) {
        this.iconsDirectory = iconsDirectory;
        this.logger = logger;
    }

    /**
     * Scans iconsDirectory for <iconId>.yml files (creating missing files/fields with defaults,
     * without touching existing values), and returns each icon's texture filename plus its
     * fully-resolved IconDisplay.
     */
    public Map<String, IconYamlConfig> reload(ModelRegistry modelRegistry) throws IOException {
        Files.createDirectories(iconsDirectory);
        Map<String, IconYamlConfig> resolved = new HashMap<>();

        try (Stream<Path> files = Files.list(iconsDirectory)) {
            for (Path iconFile : files.filter(p -> p.getFileName().toString().endsWith(".yml")).toList()) {
                String iconId = iconFile.getFileName().toString();
                iconId = iconId.substring(0, iconId.length() - ".yml".length());

                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(iconFile.toFile());
                if (ensureDefaults(iconId, yaml)) {
                    yaml.save(iconFile.toFile());
                }

                String textureFileName = yaml.getString("texture", iconId + ".png");
                resolved.put(iconId, new IconYamlConfig(textureFileName, resolve(iconId, yaml, modelRegistry)));
            }
        }
        return resolved;
    }

    /** @return true if anything was added (file was missing or missing fields, and needs a save). */
    private boolean ensureDefaults(String iconId, ConfigurationSection section) {
        boolean changed = false;
        if (!section.isSet("texture")) {
            section.set("texture", iconId + ".png");
            changed = true;
        }
        if (!section.isSet("use-blockbench-reference")) {
            section.set("use-blockbench-reference", false);
            changed = true;
        }
        if (!section.isSet("blockbench-reference")) {
            section.set("blockbench-reference", "");
            changed = true;
        }

        for (String context : IconDisplay.CONTEXTS) {
            ConfigurationSection contextSection = section.getConfigurationSection(context);
            if (contextSection == null) {
                contextSection = section.createSection(context);
                changed = true;
            }
            for (String field : new String[]{"rotation", "translation", "scale"}) {
                if (!contextSection.isSet(field)) {
                    contextSection.set(field, "generated");
                    changed = true;
                }
            }
        }
        return changed;
    }

    private IconDisplay resolve(String iconId, ConfigurationSection section, ModelRegistry modelRegistry) {
        boolean useBlockbenchReference = section.getBoolean("use-blockbench-reference", false);
        String blockbenchReference = section.getString("blockbench-reference", "");

        BbModel referencedModel = null;
        if (useBlockbenchReference && blockbenchReference != null && !blockbenchReference.isBlank()) {
            String modelId = blockbenchReference.endsWith(".bbmodel")
                    ? blockbenchReference.substring(0, blockbenchReference.length() - ".bbmodel".length())
                    : blockbenchReference;
            referencedModel = modelRegistry.get(modelId).orElse(null);
            if (referencedModel == null) {
                logger.warn("Icon '" + iconId + "' references model '" + blockbenchReference
                        + "' for its display data, but that model isn't loaded. Falling back to generated defaults.");
            }
        }

        Map<String, IconTransform> byContext = new HashMap<>();
        for (String context : IconDisplay.CONTEXTS) {
            ConfigurationSection contextSection = section.getConfigurationSection(context);
            BbDisplayTransform generatedDefault = VanillaGeneratedDisplay.forContext(context);
            Optional<BbDisplayTransform> blockbenchTransform = referencedModel != null
                    ? Optional.ofNullable(referencedModel.display().get(context))
                    : Optional.empty();

            BbVector3 rotation = resolveField(iconId, contextSection, "rotation", generatedDefault.rotation(),
                    blockbenchTransform.flatMap(BbDisplayTransform::rotation), BbVector3.ZERO);
            BbVector3 translation = resolveField(iconId, contextSection, "translation", generatedDefault.translation(),
                    blockbenchTransform.flatMap(BbDisplayTransform::translation), BbVector3.ZERO);
            BbVector3 scale = resolveField(iconId, contextSection, "scale", generatedDefault.scale(),
                    blockbenchTransform.flatMap(BbDisplayTransform::scale), new BbVector3(1, 1, 1));

            byContext.put(context, new IconTransform(rotation, translation, scale));
        }
        return new IconDisplay(byContext);
    }

    private BbVector3 resolveField(String iconId, ConfigurationSection contextSection, String field,
                                    Optional<BbVector3> generatedValue, Optional<BbVector3> blockbenchValue,
                                    BbVector3 identity) {
        if (contextSection.isConfigurationSection(field)) {
            ConfigurationSection literal = contextSection.getConfigurationSection(field);
            return new BbVector3(literal.getDouble("x", 0), literal.getDouble("y", 0), literal.getDouble("z", 0));
        }

        String keyword = contextSection.getString(field, "generated");
        return switch (keyword) {
            case "blockbench" -> blockbenchValue.orElseGet(() -> {
                logger.warn("Icon '" + iconId + "' has '" + contextSection.getName() + "." + field
                        + ": blockbench' but no matching data was found, using generated default.");
                return generatedValue.orElse(identity);
            });
            default -> generatedValue.orElse(identity);
        };
    }
}
