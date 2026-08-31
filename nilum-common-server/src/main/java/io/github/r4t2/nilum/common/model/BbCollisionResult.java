package io.github.r4t2.nilum.common.model;

import java.util.List;

public sealed interface BbCollisionResult {

    record Found(List<BbCollisionBox> boxes) implements BbCollisionResult {
    }

    record IntentionallyNonSolid() implements BbCollisionResult {
    }

    record MissingWarning() implements BbCollisionResult {
    }
}
