package io.github.r4t2.nilum.common.block;

import java.util.Optional;
import java.util.Random;

/** One entry in a block's drops list. itemId is a vanilla item id (minecraft:...); nilum: custom item drops aren't supported on hosted servers yet. */
public record BlockDropEntry(String itemId, double chance, int minCount, int maxCount) {

    /** @return the rolled count, or empty if the chance roll failed. */
    public Optional<Integer> roll(Random random) {
        if (random.nextDouble() >= chance) {
            return Optional.empty();
        }
        int count = minCount == maxCount ? minCount : minCount + random.nextInt(maxCount - minCount + 1);
        return count > 0 ? Optional.of(count) : Optional.empty();
    }
}
