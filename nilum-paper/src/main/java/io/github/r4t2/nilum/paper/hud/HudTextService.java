package io.github.r4t2.nilum.paper.hud;

import io.github.r4t2.nilum.common.expr.ExprEvaluationException;
import io.github.r4t2.nilum.common.expr.ExprEvaluator;
import io.github.r4t2.nilum.common.expr.ExprNode;
import io.github.r4t2.nilum.common.expr.ExprParser;
import io.github.r4t2.nilum.common.expr.TextValueSource;
import io.github.r4t2.nilum.common.expr.ValueSource;
import io.github.r4t2.nilum.common.hud.HudAtlasAssetPayload;
import io.github.r4t2.nilum.common.hud.HudAtlasDescriptor;
import io.github.r4t2.nilum.common.hud.HudAtlasElement;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.paper.NilumPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Periodically evaluates every loaded HUD atlas's server_connector text elements per online
 * player and pushes the resolved string via HudAtlasService.setHudText. Runs on an interval on
 * the main thread, since PAPI placeholders can be expensive, and only re-sends changed values.
 */
public final class HudTextService {

    private record ServerTextElement(String elementId, ExprNode expression) {
    }

    private final NilumPlugin plugin;
    private final NilumLogger logger;
    private final Map<String, List<ServerTextElement>> elementsByAtlas = new ConcurrentHashMap<>();
    private final Map<String, String> lastSentByKey = new ConcurrentHashMap<>();

    private BukkitTask task;

    public HudTextService(NilumPlugin plugin, NilumLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    /** Re-indexes every loaded atlas's server_connector text elements; call after a HUD atlas reload. */
    public void refresh() {
        Map<String, List<ServerTextElement>> rebuilt = new HashMap<>();
        for (String atlasId : plugin.hudAtlases().atlasIds()) {
            plugin.hudAtlases().assetBytes(atlasId).ifPresent(assetBytes -> {
                HudAtlasDescriptor descriptor;
                try {
                    descriptor = HudAtlasAssetPayload.decode(assetBytes).decodeDescriptor();
                } catch (RuntimeException e) {
                    logger.warn("Couldn't parse HUD atlas '" + atlasId + "' while indexing server_connector text elements: " + e);
                    return;
                }

                List<ServerTextElement> elements = new ArrayList<>();
                descriptor.elements().forEach((elementId, element) -> {
                    if (element instanceof HudAtlasElement.Text text) {
                        text.serverConnector().ifPresent(source -> {
                            try {
                                elements.add(new ServerTextElement(elementId, ExprParser.parse(source)));
                            } catch (RuntimeException e) {
                                logger.warn("HUD atlas '" + atlasId + "' element '" + elementId
                                        + "' has an invalid server_connector: " + e);
                            }
                        });
                    }
                });
                if (!elements.isEmpty()) {
                    rebuilt.put(atlasId, elements);
                }
            });
        }
        elementsByAtlas.clear();
        elementsByAtlas.putAll(rebuilt);
    }

    public void start(boolean enabled, int intervalTicks) {
        stop();
        if (!enabled) {
            return;
        }
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            logger.info("PlaceholderAPI isn't installed - server_connector HUD text elements will stay blank.");
            return;
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        lastSentByKey.clear();
    }

    private void tick() {
        if (elementsByAtlas.isEmpty()) {
            return;
        }

        double timeSeconds = System.nanoTime() / 1_000_000_000.0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            if (!plugin.handshakes().hasClient(playerId)) {
                continue;
            }

            ValueSource valueSource = new PlaceholderApiValueSource(player);
            TextValueSource textSource = new PlaceholderApiTextValueSource(player);

            for (Map.Entry<String, List<ServerTextElement>> entry : elementsByAtlas.entrySet()) {
                String atlasId = entry.getKey();
                for (ServerTextElement element : entry.getValue()) {
                    String resolved;
                    try {
                        resolved = ExprEvaluator.evaluateText(element.expression(), valueSource, textSource, timeSeconds);
                    } catch (ExprEvaluationException e) {
                        continue;
                    }

                    String cacheKey = playerId + "|" + atlasId + "|" + element.elementId();
                    if (!resolved.equals(lastSentByKey.put(cacheKey, resolved))) {
                        plugin.hud().setHudText(player, atlasId, element.elementId(), resolved);
                    }
                }
            }
        }
    }
}
