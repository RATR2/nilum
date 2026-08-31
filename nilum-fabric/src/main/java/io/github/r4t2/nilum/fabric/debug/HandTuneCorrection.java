package io.github.r4t2.nilum.fabric.debug;

/**
 * Correction applied on top of the hand-IK transform in ItemInHandRendererMixin, live-adjustable
 * via NilumHandTuneScreen. Defaults are the tuned values matching vanilla's own first-person hand,
 * found against a neutral origin-0,0 test rig.
 */
public final class HandTuneCorrection {

    private static final float DEFAULT_ROT_X = 0.0F;
    private static final float DEFAULT_ROT_Y = 90.0F;
    private static final float DEFAULT_ROT_Z = 0.0F;
    private static final float DEFAULT_POS_X = 0.0F;
    private static final float DEFAULT_POS_Y = 0.0F;
    private static final float DEFAULT_POS_Z = 0.0F;

    public static float rotX = DEFAULT_ROT_X;
    public static float rotY = DEFAULT_ROT_Y;
    public static float rotZ = DEFAULT_ROT_Z;
    public static float posX = DEFAULT_POS_X;
    public static float posY = DEFAULT_POS_Y;
    public static float posZ = DEFAULT_POS_Z;

    private HandTuneCorrection() {
    }

    public static void reset() {
        rotX = DEFAULT_ROT_X;
        rotY = DEFAULT_ROT_Y;
        rotZ = DEFAULT_ROT_Z;
        posX = DEFAULT_POS_X;
        posY = DEFAULT_POS_Y;
        posZ = DEFAULT_POS_Z;
    }
}
