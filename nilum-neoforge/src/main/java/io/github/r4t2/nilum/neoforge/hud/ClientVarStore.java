package io.github.r4t2.nilum.neoforge.hud;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches server-pushed named variables (number("server:name") in the expression language) so
 * auto elements don't need a packet every tick, just once when the server updates them.
 */
public final class ClientVarStore {

    private final Map<String, Double> valuesByName = new ConcurrentHashMap<>();

    public void register(String name, double initialValue) {
        valuesByName.putIfAbsent(name, initialValue);
    }

    public void set(String name, double value) {
        valuesByName.put(name, value);
    }

    public double get(String name) {
        return valuesByName.getOrDefault(name, 0.0);
    }
}
