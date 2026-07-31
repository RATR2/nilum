package io.github.r4t2.nilum.fabric.hud;

import io.github.r4t2.nilum.common.expr.ValueSource;
import io.github.r4t2.nilum.fabric.NilumFabricMod;
import net.minecraft.client.player.LocalPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Resolves {@code number("minecraft:...")}/{@code boolean("minecraft:...")} against the local
 * player, {@code server:name} against a {@link ClientVarStore}, and anything else against
 * values other client mods have registered via {@link #register(String, Supplier)} - matching
 * the design doc's "Other Mod Integration" example.
 */
public final class NilumHudValueSource implements ValueSource {

    private static final Map<String, Supplier<Double>> externalValues = new ConcurrentHashMap<>();

    /** Lets other mods expose a value under a namespaced key, e.g. {@code "origins:current_origin_power"}. */
    public static void register(String key, Supplier<Double> supplier) {
        externalValues.put(key, supplier);
    }

    private final LocalPlayer player;
    private final ClientVarStore clientVars;

    public NilumHudValueSource(LocalPlayer player, ClientVarStore clientVars) {
        this.player = player;
        this.clientVars = clientVars;
    }

    @Override
    public double resolve(String key) {
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

        NilumFabricMod.LOGGER.warn("Unknown HUD value source key '" + key + "', resolving to 0.");
        return 0;
    }

    private double resolveVanilla(String key) {
        return switch (key) {
            case "health" -> player.getHealth();
            case "max_health" -> player.getMaxHealth();
            case "food_level" -> player.getFoodData().getFoodLevel();
            case "air" -> player.getAirSupply();
            case "armor" -> player.getArmorValue();
            case "xp_level" -> player.experienceLevel;
            case "xp_progress" -> player.experienceProgress;
            case "absorption" -> player.getAbsorptionAmount();
            case "on_fire" -> player.isOnFire() ? 1 : 0;
            case "in_water" -> player.isInWater() ? 1 : 0;
            case "sneaking" -> player.isCrouching() ? 1 : 0;
            case "sprinting" -> player.isSprinting() ? 1 : 0;
            case "flying" -> player.getAbilities().flying ? 1 : 0;
            case "sleeping" -> player.isSleeping() ? 1 : 0;
            default -> {
                NilumFabricMod.LOGGER.warn("Unknown HUD value source key 'minecraft:" + key + "', resolving to 0.");
                yield 0;
            }
        };
    }
}
