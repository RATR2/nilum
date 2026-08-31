package io.github.r4t2.nilum.common.model;

public record ModelLoadError(String fileName, Exception cause) {
}
