package io.github.r4t2.nilum.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public final class BbModelBaker {

    private BbModelBaker() {
    }

    public static List<BbBakedQuad> bake(BbModel model, List<BbElement> elements) {
        List<BbBakedQuad> quads = new ArrayList<>();
        for (BbElement element : elements) {
            bakeElement(model, element, quads);
        }
        return quads;
    }

    private static void bakeElement(BbModel model, BbElement element, List<BbBakedQuad> out) {
        float x0 = (float) (element.from().x() / 16.0);
        float y0 = (float) (element.from().y() / 16.0);
        float z0 = (float) (element.from().z() / 16.0);
        float x1 = (float) (element.to().x() / 16.0);
        float y1 = (float) (element.to().y() / 16.0);
        float z1 = (float) (element.to().z() / 16.0);

        BbVector3 rotation = element.rotation();
        boolean rotated = rotation != null
                && (rotation.x() != 0 || rotation.y() != 0 || rotation.z() != 0);
        float originX = 0, originY = 0, originZ = 0;
        if (rotated) {
            originX = (float) (element.origin().x() / 16.0);
            originY = (float) (element.origin().y() / 16.0);
            originZ = (float) (element.origin().z() / 16.0);
        }

        for (Map.Entry<String, BbFace> entry : element.faces().entrySet()) {
            BbBakedQuad quad = bakeFace(entry.getKey(), entry.getValue(), model, x0, y0, z0, x1, y1, z1);
            if (quad != null) {
                if (rotated) {
                    quad = rotateQuad(quad, (float) Math.toRadians(rotation.x()), (float) Math.toRadians(rotation.y()),
                            (float) Math.toRadians(rotation.z()), originX, originY, originZ);
                }
                out.add(quad);
            }
        }
    }

    /**
     * Blockbench's per-element rotation vector is applied intrinsically X, then Y, then Z, around
     * the element's own origin point. Positions rotate around that origin; normals rotate in
     * place (direction only, no translation).
     */
    private static BbBakedQuad rotateQuad(BbBakedQuad quad, float rx, float ry, float rz,
                                           float originX, float originY, float originZ) {
        return new BbBakedQuad(
                rotateVertex(quad.v0(), rx, ry, rz, originX, originY, originZ),
                rotateVertex(quad.v1(), rx, ry, rz, originX, originY, originZ),
                rotateVertex(quad.v2(), rx, ry, rz, originX, originY, originZ),
                rotateVertex(quad.v3(), rx, ry, rz, originX, originY, originZ),
                quad.textureIndex());
    }

    private static BbBakedVertex rotateVertex(BbBakedVertex v, float rx, float ry, float rz,
                                               float originX, float originY, float originZ) {
        float[] position = rotateXYZ(v.x() - originX, v.y() - originY, v.z() - originZ, rx, ry, rz);
        float[] normal = rotateXYZ(v.nx(), v.ny(), v.nz(), rx, ry, rz);
        return new BbBakedVertex(position[0] + originX, position[1] + originY, position[2] + originZ,
                v.u(), v.v(), normal[0], normal[1], normal[2]);
    }

    private static float[] rotateXYZ(float x, float y, float z, float rx, float ry, float rz) {
        float cosX = (float) Math.cos(rx), sinX = (float) Math.sin(rx);
        float y1 = y * cosX - z * sinX;
        float z1 = y * sinX + z * cosX;
        float x1 = x;

        float cosY = (float) Math.cos(ry), sinY = (float) Math.sin(ry);
        float x2 = x1 * cosY + z1 * sinY;
        float z2 = -x1 * sinY + z1 * cosY;
        float y2 = y1;

        float cosZ = (float) Math.cos(rz), sinZ = (float) Math.sin(rz);
        float x3 = x2 * cosZ - y2 * sinZ;
        float y3 = x2 * sinZ + y2 * cosZ;
        float z3 = z2;

        return new float[]{x3, y3, z3};
    }

    private static BbBakedQuad bakeFace(String direction, BbFace face, BbModel model,float x0, float y0, float z0, float x1, float y1, float z1) {
        float[][] positions;
        float[] normal;
        switch (direction) {
            case "down" -> {
                positions = new float[][]{{x0, y0, z0}, {x1, y0, z0}, {x1, y0, z1}, {x0, y0, z1}};
                normal = new float[]{0, -1, 0};
            }
            case "up" -> {
                positions = new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}, {x1, y1, z0}};
                normal = new float[]{0, 1, 0};
            }
            case "north" -> {
                positions = new float[][]{{x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}, {x1, y1, z0}};
                normal = new float[]{0, 0, -1};
            }
            case "south" -> {
                positions = new float[][]{{x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
                normal = new float[]{0, 0, 1};
            }
            case "west" -> {
                positions = new float[][]{{x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}, {x0, y1, z0}};
                normal = new float[]{-1, 0, 0};
            }
            case "east" -> {
                positions = new float[][]{{x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}, {x1, y1, z1}};
                normal = new float[]{1, 0, 0};
            }
            default -> {
                return null;
            }
        }

        int uvWidth = model.resolutionWidth();
        int uvHeight = model.resolutionHeight();
        if (face.textureIndex() != null && face.textureIndex() >= 0 && face.textureIndex() < model.textures().size()) {
            BbTexture texture = model.textures().get(face.textureIndex());
            uvWidth = texture.uvWidth();
            uvHeight = texture.uvHeight();
        }

        float u1 = (float) (face.u1() / uvWidth);
        float v1 = (float) (face.v1() / uvHeight);
        float u2 = (float) (face.u2() / uvWidth);
        float v2 = (float) (face.v2() / uvHeight);

        float[][] uvs = rotateUv(new float[][]{{u1, v1}, {u2, v1}, {u2, v2}, {u1, v2}}, face.rotation());

        BbBakedVertex[] vertices = new BbBakedVertex[4];
        for (int i = 0; i < 4; i++) {
            vertices[i] = new BbBakedVertex(positions[i][0], positions[i][1], positions[i][2],
                    uvs[i][0], uvs[i][1], normal[0], normal[1], normal[2]);
        }

        return new BbBakedQuad(vertices[0], vertices[1], vertices[2], vertices[3], face.textureIndex());
    }

    private static float[][] rotateUv(float[][] uvs, int rotationDegrees) {
        int steps = ((rotationDegrees / 90) % 4 + 4) % 4;
        if (steps == 0) {
            return uvs;
        }
        float[][] rotated = new float[4][2];
        for (int i = 0; i < 4; i++) {
            rotated[i] = uvs[(i + steps) % 4];
        }
        return rotated;
    }
}
