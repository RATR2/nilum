package io.github.r4t2.nilum.common.model;

/**
 * Result of ProxyMaterialClassifier: which vanilla-shaped proxy a collision shape can be
 * represented by. COMPLEX covers anything that isn't a full cube or half-height slab.
 */
public enum BbProxyShape {
    FULL_CUBE,
    SLAB_BOTTOM,
    SLAB_TOP,
    COMPLEX
}
