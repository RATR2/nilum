package io.github.r4t2.nilum.common.model;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per (holder, visual hand side) animation playback state for held items. Unlike placed models,
 * which auto-loop their first animation until a trigger arrives, a held item starts at rest and
 * stays there until explicitly triggered.
 */
public final class ClientHeldItemAnimationStates {

    private record Key(UUID holderId, boolean rightHand) {
    }

    private final Map<Key, AnimationPlaybackState> states = new ConcurrentHashMap<>();
    private final Map<Key, String> currentModelIds = new ConcurrentHashMap<>();

    public AnimationPlaybackState get(UUID holderId, boolean rightHand, BbModel model) {
        return states.computeIfAbsent(new Key(holderId, rightHand), key -> {
            AnimationPlaybackState state = new AnimationPlaybackState();
            state.stop(model, System.currentTimeMillis());
            return state;
        });
    }

    /** Called every frame a held item renders, so a later play/stop trigger packet (which carries no model id) knows which model to animate. */
    public void noteCurrentModel(UUID holderId, boolean rightHand, String modelId) {
        currentModelIds.put(new Key(holderId, rightHand), modelId);
    }

    public String currentModelId(UUID holderId, boolean rightHand) {
        return currentModelIds.get(new Key(holderId, rightHand));
    }
}
