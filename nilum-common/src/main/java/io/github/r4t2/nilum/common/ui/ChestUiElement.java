package io.github.r4t2.nilum.common.ui;

import java.util.List;
import java.util.Optional;

/** One interactive slot inside a chest UI. */
public record ChestUiElement(int slot, String material, Optional<String> name, List<String> lore,
                              Optional<String> action, Optional<String> requirement) {
}
