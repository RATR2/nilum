package io.github.r4t2.nilum.paper.ui;

import io.github.r4t2.nilum.common.expr.ExprEvaluationException;
import io.github.r4t2.nilum.common.expr.ExprEvaluator;
import io.github.r4t2.nilum.common.expr.ExprNode;
import io.github.r4t2.nilum.common.expr.ExprParser;
import io.github.r4t2.nilum.common.expr.TextValueSource;
import io.github.r4t2.nilum.common.expr.ValueSource;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import io.github.r4t2.nilum.common.ui.UiElement;
import io.github.r4t2.nilum.paper.NilumPlugin;
import io.github.r4t2.nilum.paper.hud.NilumJavaTextValueSource;
import io.github.r4t2.nilum.paper.hud.NilumJavaValueSource;
import io.github.r4t2.nilum.paper.hud.PlaceholderApiTextValueSource;
import io.github.r4t2.nilum.paper.hud.PlaceholderApiValueSource;
import io.github.r4t2.nilum.paper.hud.SkriptVariableValueSource;
import io.github.r4t2.nilum.paper.skript.NilumSkriptVariables;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Periodically evaluates every open Custom UI's requirement/server_connector expressions and pushes changes, mirroring HudTextService. */
public final class UiStateService {

    private record IndexedElement(String elementId, Optional<ExprNode> requirement, Optional<ExprNode> serverConnector) {
    }

    private record ValueSourcePair(ValueSource valueSource, TextValueSource textSource) {
    }

    private final NilumPlugin plugin;
    private final NilumLogger logger;
    private final Map<String, List<IndexedElement>> elementsByUi = new ConcurrentHashMap<>();
    private final Map<String, Boolean> lastVisibleByKey = new ConcurrentHashMap<>();
    private final Map<String, String> lastTextByKey = new ConcurrentHashMap<>();

    private BukkitTask task;

    public UiStateService(NilumPlugin plugin, NilumLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    /** Re-indexes every loaded UI's requirement/server_connector expressions; call after a UI reload. */
    public void refresh() {
        Map<String, List<IndexedElement>> rebuilt = new HashMap<>();
        for (String uiId : plugin.uis().uiIds()) {
            plugin.uis().descriptor(uiId).ifPresent(descriptor -> {
                List<IndexedElement> elements = new ArrayList<>();
                descriptor.elements().forEach((elementId, element) -> {
                    Optional<ExprNode> requirement = parseOrWarn(uiId, elementId, "requirement", element.requirement());
                    Optional<ExprNode> serverConnector = element instanceof UiElement.Text text
                            ? parseOrWarn(uiId, elementId, "server_connector", text.serverConnector())
                            : Optional.empty();
                    if (requirement.isPresent() || serverConnector.isPresent()) {
                        elements.add(new IndexedElement(elementId, requirement, serverConnector));
                    }
                });
                if (!elements.isEmpty()) {
                    rebuilt.put(uiId, elements);
                }
            });
        }
        elementsByUi.clear();
        elementsByUi.putAll(rebuilt);
    }

    private Optional<ExprNode> parseOrWarn(String uiId, String elementId, String field, Optional<String> raw) {
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ExprParser.parse(raw.get()));
        } catch (RuntimeException e) {
            logger.warn("Custom UI '" + uiId + "' element '" + elementId + "' has an invalid " + field + ": " + e);
            return Optional.empty();
        }
    }

    public void start(boolean enabled, int intervalTicks) {
        stop();
        if (!enabled) {
            return;
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, intervalTicks, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        lastVisibleByKey.clear();
        lastTextByKey.clear();
    }

    /** Sends every requirement/server_connector element's current value immediately, bypassing the change-diff since a fresh open has no prior state to compare against. */
    public void onOpen(Player player, String uiId) {
        List<IndexedElement> elements = elementsByUi.get(uiId);
        if (elements == null || elements.isEmpty()) {
            return;
        }
        ValueSourcePair sources = buildSources(player);
        for (IndexedElement element : elements) {
            evaluateAndSend(player, uiId, element, sources, true);
        }
    }

    private void tick() {
        if (elementsByUi.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, String> session : plugin.uiSessions().openSessions().entrySet()) {
            List<IndexedElement> elements = elementsByUi.get(session.getValue());
            if (elements == null) {
                continue;
            }
            Player player = plugin.getServer().getPlayer(session.getKey());
            if (player == null) {
                continue;
            }
            ValueSourcePair sources = buildSources(player);
            for (IndexedElement element : elements) {
                evaluateAndSend(player, session.getValue(), element, sources, false);
            }
        }
    }

    private void evaluateAndSend(Player player, String uiId, IndexedElement element, ValueSourcePair sources, boolean force) {
        double timeSeconds = System.nanoTime() / 1_000_000_000.0;
        String cacheKeyBase = player.getUniqueId() + "|" + uiId + "|" + element.elementId();

        if (element.requirement().isPresent()) {
            boolean visible;
            try {
                visible = ExprEvaluator.evaluate(element.requirement().get(), sources.valueSource(), timeSeconds) != 0;
            } catch (ExprEvaluationException e) {
                visible = true;
            }
            Boolean previous = lastVisibleByKey.put(cacheKeyBase + "|visible", visible);
            if (force || previous == null || previous != visible) {
                plugin.uiSessions().setElementVisible(player, uiId, element.elementId(), visible);
            }
        }

        if (element.serverConnector().isPresent()) {
            String resolved;
            try {
                resolved = ExprEvaluator.evaluateText(element.serverConnector().get(), sources.valueSource(), sources.textSource(), timeSeconds);
            } catch (ExprEvaluationException e) {
                return;
            }
            String previous = lastTextByKey.put(cacheKeyBase + "|text", resolved);
            if (force || !resolved.equals(previous)) {
                plugin.uiSessions().setElementText(player, uiId, element.elementId(), resolved);
            }
        }
    }

    private ValueSourcePair buildSources(Player player) {
        boolean placeholderApiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        boolean skriptAvailable = NilumSkriptVariables.isAvailable();

        ValueSource valueSource = placeholderApiAvailable
                ? new PlaceholderApiValueSource(player) : new NilumJavaValueSource(player);
        TextValueSource textSource = placeholderApiAvailable
                ? new PlaceholderApiTextValueSource(player) : new NilumJavaTextValueSource(player);

        if (skriptAvailable) {
            ValueSource base = valueSource;
            valueSource = (function, key) -> function.equals("skriptvar")
                    ? SkriptVariableValueSource.resolveNumeric(key, player) : base.resolve(function, key);
            TextValueSource baseText = textSource;
            textSource = (function, key) -> function.equals("skriptvar")
                    ? SkriptVariableValueSource.resolveText(key, player) : baseText.resolve(function, key);
        }
        return new ValueSourcePair(valueSource, textSource);
    }
}
