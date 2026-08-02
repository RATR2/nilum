package io.github.r4t2.nilum.common.model;

import java.util.Optional;

/**
 * One Blockbench "display" panel entry for a single ItemDisplayContext (e.g. "gui",
 * "thirdperson_righthand"). Each field is only present if the .bbmodel specifies it.
 */
public record BbDisplayTransform(Optional<BbVector3> rotation, Optional<BbVector3> translation, Optional<BbVector3> scale) {
}
