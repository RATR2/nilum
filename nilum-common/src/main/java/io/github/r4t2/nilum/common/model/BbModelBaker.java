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

        for (Map.Entry<String, BbFace> entry : element.faces().entrySet()) {
            BbBakedQuad quad = bakeFace(entry.getKey(), entry.getValue(), model, x0, y0, z0, x1, y1, z1);
            if (quad != null) {
                out.add(quad);
            }
        }
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

        float u1 = (float) (face.u1() / model.resolutionWidth());
        float v1 = (float) (face.v1() / model.resolutionHeight());
        float u2 = (float) (face.u2() / model.resolutionWidth());
        float v2 = (float) (face.v2() / model.resolutionHeight());

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
