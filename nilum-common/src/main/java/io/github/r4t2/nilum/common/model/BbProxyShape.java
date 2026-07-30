package io.github.r4t2.nilum.common.model;

/**
 * Result of {@link ProxyMaterialClassifier}: which vanilla-shaped proxy a
 * collision shape can be represented by, per the design doc's Proxy Material
 * Auto-Classification. {@code COMPLEX} covers anything that isn't a simple
 * full cube or half-height slab, including stair shapes (not classified yet).
 */
public enum BbProxyShape {
    FULL_CUBE,
    SLAB_BOTTOM,
    SLAB_TOP,
    COMPLEX
}
