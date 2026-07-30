package io.github.r4t2.nilum.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BbCollisionParser {

    private static final String COLLISION_GROUP_NAME = "collision";
    private static final String NO_COLLISION_INTENT = "none";

    private BbCollisionParser() {
    }

    public static BbCollisionResult resolve(BbModel model) {
        Optional<BbOutlinerGroup> collisionGroup = model.findGroup(COLLISION_GROUP_NAME);

        if (collisionGroup.isEmpty()) {
            boolean intentionallyNonSolid = model.collisionIntent()
                    .map(intent -> intent.equalsIgnoreCase(NO_COLLISION_INTENT))
                    .orElse(false);
            return intentionallyNonSolid ? new BbCollisionResult.IntentionallyNonSolid() : new BbCollisionResult.MissingWarning();
        }

        List<BbCollisionBox> boxes = new ArrayList<>();
        for (BbElement element : model.resolveElements(collisionGroup.get())) {
            boxes.add(toBox(element));
        }
        return new BbCollisionResult.Found(boxes);
    }

    private static BbCollisionBox toBox(BbElement element) {
        double x0 = element.from().x() / 16.0;
        double y0 = element.from().y() / 16.0;
        double z0 = element.from().z() / 16.0;
        double x1 = element.to().x() / 16.0;
        double y1 = element.to().y() / 16.0;
        double z1 = element.to().z() / 16.0;

        return new BbCollisionBox(
                Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1),
                Math.max(x0, x1), Math.max(y0, y1), Math.max(z0, z1));
    }
}
