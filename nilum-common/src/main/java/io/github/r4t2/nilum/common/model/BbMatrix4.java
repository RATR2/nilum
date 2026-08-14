package io.github.r4t2.nilum.common.model;

/** Row-major 4x4 affine transform, used to compose a bone hierarchy's rest pose and animated deltas. */
public final class BbMatrix4 {

    private final float[] m;

    private BbMatrix4(float[] m) {
        this.m = m;
    }

    public static BbMatrix4 identity() {
        return new BbMatrix4(new float[]{
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        });
    }

    public static BbMatrix4 translation(float x, float y, float z) {
        return new BbMatrix4(new float[]{
                1, 0, 0, x,
                0, 1, 0, y,
                0, 0, 1, z,
                0, 0, 0, 1
        });
    }

    public static BbMatrix4 scale(float sx, float sy, float sz) {
        return new BbMatrix4(new float[]{
                sx, 0, 0, 0,
                0, sy, 0, 0,
                0, 0, sz, 0,
                0, 0, 0, 1
        });
    }

    /** Intrinsic X-then-Y-then-Z rotation, matching BbModelBaker's per-element rotation convention. Degrees. */
    public static BbMatrix4 rotationXYZDegrees(float rxDeg, float ryDeg, float rzDeg) {
        return rotationZ((float) Math.toRadians(rzDeg))
                .multiply(rotationY((float) Math.toRadians(ryDeg)))
                .multiply(rotationX((float) Math.toRadians(rxDeg)));
    }

    private static BbMatrix4 rotationX(float radians) {
        float c = (float) Math.cos(radians), s = (float) Math.sin(radians);
        return new BbMatrix4(new float[]{
                1, 0, 0, 0,
                0, c, -s, 0,
                0, s, c, 0,
                0, 0, 0, 1
        });
    }

    private static BbMatrix4 rotationY(float radians) {
        float c = (float) Math.cos(radians), s = (float) Math.sin(radians);
        return new BbMatrix4(new float[]{
                c, 0, s, 0,
                0, 1, 0, 0,
                -s, 0, c, 0,
                0, 0, 0, 1
        });
    }

    private static BbMatrix4 rotationZ(float radians) {
        float c = (float) Math.cos(radians), s = (float) Math.sin(radians);
        return new BbMatrix4(new float[]{
                c, -s, 0, 0,
                s, c, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        });
    }

    /** this * other: applying the result to a point applies other's transform first, then this one. */
    public BbMatrix4 multiply(BbMatrix4 other) {
        float[] r = new float[16];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                float sum = 0;
                for (int k = 0; k < 4; k++) {
                    sum += m[row * 4 + k] * other.m[k * 4 + col];
                }
                r[row * 4 + col] = sum;
            }
        }
        return new BbMatrix4(r);
    }

    public float[] transformPoint(float x, float y, float z) {
        return new float[]{
                m[0] * x + m[1] * y + m[2] * z + m[3],
                m[4] * x + m[5] * y + m[6] * z + m[7],
                m[8] * x + m[9] * y + m[10] * z + m[11]
        };
    }

    /** Direction-only transform (rotation/scale, no translation) for normals; exact for uniform scale only. */
    public float[] transformDirection(float x, float y, float z) {
        return new float[]{
                m[0] * x + m[1] * y + m[2] * z,
                m[4] * x + m[5] * y + m[6] * z,
                m[8] * x + m[9] * y + m[10] * z
        };
    }

    /** Element-wise interpolation toward other, used to blend poses. A plain lerp of matrix cells, not a proper rotation slerp. */
    public BbMatrix4 lerp(BbMatrix4 other, float t) {
        float[] r = new float[16];
        for (int i = 0; i < 16; i++) {
            r[i] = m[i] + (other.m[i] - m[i]) * t;
        }
        return new BbMatrix4(r);
    }

    /** Raw row-major 16-float array (row*4+col), for platform code that needs to hand this to its own matrix type. */
    public float[] toRowMajorArray() {
        return m.clone();
    }

    float get(int row, int col) {
        return m[row * 4 + col];
    }
}
