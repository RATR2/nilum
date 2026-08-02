package io.github.r4t2.nilum.fabric.render;

/**
 * One glint-pass quad: positions matching a base-layer quad exactly, so the glint is masked to
 * the item's actual shape, plus that quad's own UV to use as the scroll basis.
 */
record GlintQuad(GlintVertex v0, GlintVertex v1, GlintVertex v2, GlintVertex v3) {

    record GlintVertex(float x, float y, float z, float u, float v) {
    }
}
