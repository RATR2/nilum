package io.github.r4t2.nilum.neoforge.hud;

import io.github.r4t2.nilum.common.expr.ValueSource;
import io.github.r4t2.nilum.common.logging.NilumLogger;
import net.minecraft.client.player.LocalPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Resolves minecraft:... against the local player, server:name against ClientVarStore, and anything else via register(String, Supplier). */
public final class NilumHudValueSource implements ValueSource {

    private static final Map<String, Supplier<Double>> externalValues = new ConcurrentHashMap<>();

    /** Lets other mods expose a value under a namespaced key, e.g. "origins:current_origin_power". */
    public static void register(String key, Supplier<Double> supplier) {
        externalValues.put(key, supplier);
    }

    private final LocalPlayer player;
    private final ClientVarStore clientVars;
    private final NilumLogger logger;

    public NilumHudValueSource(LocalPlayer player, ClientVarStore clientVars, NilumLogger logger) {
        this.player = player;
        this.clientVars = clientVars;
        this.logger = logger;
    }

    @Override
    public double resolve(String function, String key) {
        if (key.startsWith("minecraft:")) {
            return resolveVanilla(key.substring("minecraft:".length()));
        }
        if (key.startsWith("server:")) {
            return clientVars.get(key.substring("server:".length()));
        }

        Supplier<Double> external = externalValues.get(key);
        if (external != null) {
            return external.get();
        }

        logger.warn("Unknown HUD value source key '" + key + "', resolving to 0.");
        return 0;
    }

    private double resolveVanilla(String key) {
        return switch (key) {
            case "health" -> player.getHealth();
            case "max_health" -> player.getMaxHealth();
            case "food_level" -> player.getFoodData().getFoodLevel();
            case "saturation" -> player.getFoodData().getSaturationLevel();
            case "air" -> player.getAirSupply();
            case "max_air" -> player.getMaxAirSupply();
            case "armor" -> player.getArmorValue();
            case "xp_level" -> player.experienceLevel;
            case "xp_progress" -> player.experienceProgress;
            case "absorption" -> player.getAbsorptionAmount();
            case "on_fire" -> player.isOnFire() ? 1 : 0;
            case "in_water" -> player.isInWater() ? 1 : 0;
            case "swimming" -> player.isSwimming() ? 1 : 0;
            case "sneaking" -> player.isCrouching() ? 1 : 0;
            case "sprinting" -> player.isSprinting() ? 1 : 0;
            case "flying" -> player.getAbilities().flying ? 1 : 0;
            case "sleeping" -> player.isSleeping() ? 1 : 0;
            default -> {
                logger.warn("Unknown HUD value source key 'minecraft:" + key + "', resolving to 0.");
                yield 0;
            }
        };
    }
}
