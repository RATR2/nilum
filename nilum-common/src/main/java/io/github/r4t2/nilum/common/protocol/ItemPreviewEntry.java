package io.github.r4t2.nilum.common.protocol;

import java.util.List;

/** One item definition's client-facing preview data: enough to render a real-looking creative tab entry. */
public record ItemPreviewEntry(String assetId, String displayName, List<String> lore, List<String> hideGroups) {
}
