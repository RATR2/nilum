package io.github.r4t2.nilum.paper.block;

import io.github.r4t2.nilum.paper.item.DropEntry;

import java.util.List;

/** A named, reusable Nilum block type: blocks/<id>.yml, same convention as items/icons. */
public record BlockDefinition(String id, String modelId, BlockProxy proxy, List<DropEntry> drops) {
}
