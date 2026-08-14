package io.github.r4t2.nilum.paper.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/** One entry in a block's drops list. itemRef is nilum:<id> or minecraft:<id>; count is in [minCount, maxCount]. */
public record DropEntry(String itemRef, double chance, int minCount, int maxCount) {

    /** @return the rolled stack(s), or empty if the chance roll failed or the item couldn't be resolved. */
    public List<ItemStack> roll(ItemDefinitionRegistry itemDefinitions, Random random) {
        if (random.nextDouble() >= chance) {
            return List.of();
        }

        int count = minCount == maxCount ? minCount : minCount + random.nextInt(maxCount - minCount + 1);
        if (count <= 0) {
            return List.of();
        }

        Optional<ItemStack> template = resolveTemplate(itemDefinitions);
        if (template.isEmpty()) {
            return List.of();
        }

        ItemStack single = template.get();
        List<ItemStack> stacks = new java.util.ArrayList<>();
        int remaining = count;
        while (remaining > 0) {
            int stackSize = Math.min(remaining, single.getMaxStackSize());
            ItemStack stack = single.clone();
            stack.setAmount(stackSize);
            stacks.add(stack);
            remaining -= stackSize;
        }
        return stacks;
    }

    /**
     * Parses a drops: list, e.g.:
     * <pre>
     * drops:
     *   - item: nilum:healing_potion
     *     chance: 0.5
     *     count: [1, 3]
     *   - item: minecraft:stick
     *     count: 2
     * </pre>
     * chance defaults to 1.0, count defaults to 1, a bare number means an exact count, and a
     * 2-element list means an inclusive [min, max] range.
     */
    public static List<DropEntry> parseList(List<Map<?, ?>> raw) {
        List<DropEntry> entries = new ArrayList<>();
        for (Map<?, ?> entryMap : raw) {
            Object itemRef = entryMap.get("item");
            if (itemRef == null) {
                continue;
            }

            double chance = entryMap.get("chance") instanceof Number n ? n.doubleValue() : 1.0;

            int minCount = 1;
            int maxCount = 1;
            Object count = entryMap.get("count");
            if (count instanceof Number n) {
                minCount = maxCount = n.intValue();
            } else if (count instanceof List<?> range && range.size() == 2
                    && range.get(0) instanceof Number min && range.get(1) instanceof Number max) {
                minCount = min.intValue();
                maxCount = max.intValue();
            }

            entries.add(new DropEntry(itemRef.toString(), chance, minCount, maxCount));
        }
        return entries;
    }

    private Optional<ItemStack> resolveTemplate(ItemDefinitionRegistry itemDefinitions) {
        if (itemRef.startsWith("nilum:")) {
            return itemDefinitions.resolve(itemRef.substring("nilum:".length()));
        }
        Material material = Material.matchMaterial(itemRef);
        return Optional.ofNullable(material).map(ItemStack::new);
    }
}
