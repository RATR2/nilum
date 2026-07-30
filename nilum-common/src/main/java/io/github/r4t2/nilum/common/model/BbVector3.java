package io.github.r4t2.nilum.common.model;

public record BbVector3(double x, double y, double z) {

    public static final BbVector3 ZERO = new BbVector3(0, 0, 0);
}
