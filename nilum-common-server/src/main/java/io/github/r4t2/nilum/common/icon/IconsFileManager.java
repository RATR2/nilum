package io.github.r4t2.nilum.common.icon;

import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.model.BbDisplayTransform;
import io.github.r4t2.nilum.common.model.BbModel;
import io.github.r4t2.nilum.common.model.BbVector3;
import io.github.r4t2.nilum.common.model.ModelRegistry;
import io.github.r4t2.nilum.common.model.VanillaGeneratedDisplay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Loader-agnostic equivalent of nilum-paper's IconsYamlManager, using the project's hand-rolled grammar instead of YamlConfiguration. */
public final class IconsFileManager {

    private static final Pattern CONTEXT_HEADER = Pattern.compile("^context\\s+(\\S[\\w.-]*):\\s*$");
    private static final Pattern FIELD_LINE = Pattern.compile("^\\s+(\\S[\\w.-]*):\\s*(.*)$");
    private static final Pattern TOP_LEVEL_LINE = Pattern.compile("^(\\S[\\w.-]*):\\s*(.*)$");

    private final Path iconsDirectory;
    private final NilumLogger logger;

    public IconsFileManager(Path iconsDirectory, NilumLogger logger) {
        this.iconsDirectory = iconsDirectory;
        this.logger = logger;
    }

    /** Scans iconsDirectory for <iconId>.yml files, creating missing files/fields with defaults without touching existing values. */
    public Map<String, IconFileConfig> reload(ModelRegistry modelRegistry) throws IOException {
        Files.createDirectories(iconsDirectory);
        Map<String, IconFileConfig> resolved = new LinkedHashMap<>();

        try (Stream<Path> files = Files.list(iconsDirectory)) {
            for (Path iconFile : files.filter(p -> p.getFileName().toString().endsWith(".yml")).toList()) {
                String fileName = iconFile.getFileName().toString();
                String iconId = fileName.substring(0, fileName.length() - ".yml".length());

                Map<String, String> topLevel = new LinkedHashMap<>();
                Map<String, Map<String, String>> perContext = new LinkedHashMap<>();
                if (Files.isRegularFile(iconFile)) {
                    parse(Files.readString(iconFile), topLevel, perContext);
                }

                boolean changed = ensureDefaults(iconId, topLevel, perContext);
                if (changed) {
                    Files.writeString(iconFile, serialize(topLevel, perContext));
                }

                String textureFileName = topLevel.getOrDefault("texture", iconId + ".png");
                resolved.put(iconId, new IconFileConfig(textureFileName, resolve(iconId, topLevel, perContext, modelRegistry)));
            }
        }
        return resolved;
    }

    private void parse(String source, Map<String, String> topLevel, Map<String, Map<String, String>> perContext) {
        String currentContext = null;
        for (String line : source.split("\n", -1)) {
            if (line.isBlank() || line.stripLeading().startsWith("#")) {
                continue;
            }

            Matcher contextMatcher = CONTEXT_HEADER.matcher(line);
            if (contextMatcher.matches()) {
                currentContext = contextMatcher.group(1);
                perContext.putIfAbsent(currentContext, new LinkedHashMap<>());
                continue;
            }

            Matcher fieldMatcher = FIELD_LINE.matcher(line);
            if (fieldMatcher.matches() && currentContext != null) {
                perContext.get(currentContext).put(fieldMatcher.group(1), unquote(fieldMatcher.group(2).trim()));
                continue;
            }

            Matcher topMatcher = TOP_LEVEL_LINE.matcher(line);
            if (topMatcher.matches()) {
                currentContext = null;
                topLevel.put(topMatcher.group(1), unquote(topMatcher.group(2).trim()));
            }
        }
    }

    private String serialize(Map<String, String> topLevel, Map<String, Map<String, String>> perContext) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> entry : topLevel.entrySet()) {
            out.append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }
        for (Map.Entry<String, Map<String, String>> context : perContext.entrySet()) {
            out.append('\n').append("context ").append(context.getKey()).append(":\n");
            for (Map.Entry<String, String> field : context.getValue().entrySet()) {
                out.append("  ").append(field.getKey()).append(": ").append(field.getValue()).append('\n');
            }
        }
        return out.toString();
    }

    /** @return true if anything was added (file was missing or missing fields, and needs a save). */
    private boolean ensureDefaults(String iconId, Map<String, String> topLevel, Map<String, Map<String, String>> perContext) {
        boolean changed = false;
        if (!topLevel.containsKey("texture")) {
            topLevel.put("texture", iconId + ".png");
            changed = true;
        }
        if (!topLevel.containsKey("use-blockbench-reference")) {
            topLevel.put("use-blockbench-reference", "false");
            changed = true;
        }
        if (!topLevel.containsKey("blockbench-reference")) {
            topLevel.put("blockbench-reference", "");
            changed = true;
        }

        for (String context : IconDisplay.CONTEXTS) {
            Map<String, String> contextFields = perContext.computeIfAbsent(context, ignored -> new LinkedHashMap<>());
            for (String field : new String[]{"rotation", "translation", "scale"}) {
                if (!contextFields.containsKey(field)) {
                    contextFields.put(field, "generated");
                    changed = true;
                }
            }
        }
        return changed;
    }

    private IconDisplay resolve(String iconId, Map<String, String> topLevel, Map<String, Map<String, String>> perContext,
                                 ModelRegistry modelRegistry) {
        boolean useBlockbenchReference = "true".equalsIgnoreCase(topLevel.getOrDefault("use-blockbench-reference", "false"));
        String blockbenchReference = topLevel.getOrDefault("blockbench-reference", "");

        BbModel referencedModel = null;
        if (useBlockbenchReference && !blockbenchReference.isBlank()) {
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
            Map<String, String> contextFields = perContext.getOrDefault(context, Map.of());
            BbDisplayTransform generatedDefault = VanillaGeneratedDisplay.forContext(context);
            Optional<BbDisplayTransform> blockbenchTransform = referencedModel != null
                    ? Optional.ofNullable(referencedModel.display().get(context))
                    : Optional.empty();

            BbVector3 rotation = resolveField(iconId, context, contextFields, "rotation", generatedDefault.rotation(),
                    blockbenchTransform.flatMap(BbDisplayTransform::rotation), BbVector3.ZERO);
            BbVector3 translation = resolveField(iconId, context, contextFields, "translation", generatedDefault.translation(),
                    blockbenchTransform.flatMap(BbDisplayTransform::translation), BbVector3.ZERO);
            BbVector3 scale = resolveField(iconId, context, contextFields, "scale", generatedDefault.scale(),
                    blockbenchTransform.flatMap(BbDisplayTransform::scale), new BbVector3(1, 1, 1));

            byContext.put(context, new IconTransform(rotation, translation, scale));
        }
        return new IconDisplay(byContext);
    }

    private BbVector3 resolveField(String iconId, String context, Map<String, String> contextFields, String field,
                                    Optional<BbVector3> generatedValue, Optional<BbVector3> blockbenchValue, BbVector3 identity) {
        String raw = contextFields.getOrDefault(field, "generated");
        if (raw.startsWith("[")) {
            return parseVector(raw);
        }

        return switch (raw) {
            case "blockbench" -> blockbenchValue.orElseGet(() -> {
                logger.warn("Icon '" + iconId + "' has '" + context + "." + field
                        + ": blockbench' but no matching data was found, using generated default.");
                return generatedValue.orElse(identity);
            });
            default -> generatedValue.orElse(identity);
        };
    }

    private static BbVector3 parseVector(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        String[] parts = trimmed.split(",");
        double x = parts.length > 0 ? Double.parseDouble(parts[0].trim()) : 0;
        double y = parts.length > 1 ? Double.parseDouble(parts[1].trim()) : 0;
        double z = parts.length > 2 ? Double.parseDouble(parts[2].trim()) : 0;
        return new BbVector3(x, y, z);
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
