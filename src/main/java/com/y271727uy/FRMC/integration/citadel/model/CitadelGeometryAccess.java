package com.y271727uy.FRMC.integration.citadel.model;

import java.lang.reflect.Field;

/** Reads Citadel's package-private model geometry for the render backends. */
public final class CitadelGeometryAccess {
    private static final Field TABULA_BOX_QUADS = field("com.github.alexthe666.citadel.client.model.TabulaModelRenderUtils$ModelBox", "quads");
    private static final Field TABULA_QUAD_VERTICES = field("com.github.alexthe666.citadel.client.model.TabulaModelRenderUtils$TexturedQuad", "vertexPositions");
    private static final Field TABULA_QUAD_NORMAL = field("com.github.alexthe666.citadel.client.model.TabulaModelRenderUtils$TexturedQuad", "normal");
    private static final Field TABULA_VERTEX_POSITION = field("com.github.alexthe666.citadel.client.model.TabulaModelRenderUtils$PositionTextureVertex", "position");
    private static final Field TABULA_VERTEX_U = field("com.github.alexthe666.citadel.client.model.TabulaModelRenderUtils$PositionTextureVertex", "textureU");
    private static final Field TABULA_VERTEX_V = field("com.github.alexthe666.citadel.client.model.TabulaModelRenderUtils$PositionTextureVertex", "textureV");

    private static final Field BASIC_BOX_QUADS = field("com.github.alexthe666.citadel.client.model.basic.BasicModelPart$ModelBox", "quads");
    private static final Field BASIC_QUAD_VERTICES = field("com.github.alexthe666.citadel.client.model.basic.BasicModelPart$TexturedQuad", "vertexPositions");
    private static final Field BASIC_QUAD_NORMAL = field("com.github.alexthe666.citadel.client.model.basic.BasicModelPart$TexturedQuad", "normal");
    private static final Field BASIC_VERTEX_POSITION = field("com.github.alexthe666.citadel.client.model.basic.BasicModelPart$PositionTextureVertex", "position");
    private static final Field BASIC_VERTEX_U = field("com.github.alexthe666.citadel.client.model.basic.BasicModelPart$PositionTextureVertex", "textureU");
    private static final Field BASIC_VERTEX_V = field("com.github.alexthe666.citadel.client.model.basic.BasicModelPart$PositionTextureVertex", "textureV");

    private CitadelGeometryAccess() {
    }

    public static boolean isAvailable() {
        return TABULA_BOX_QUADS != null && TABULA_QUAD_VERTICES != null && TABULA_QUAD_NORMAL != null
                && TABULA_VERTEX_POSITION != null && TABULA_VERTEX_U != null && TABULA_VERTEX_V != null
                && BASIC_BOX_QUADS != null && BASIC_QUAD_VERTICES != null && BASIC_QUAD_NORMAL != null
                && BASIC_VERTEX_POSITION != null && BASIC_VERTEX_U != null && BASIC_VERTEX_V != null;
    }

    public static Object[] tabulaQuads(Object box) { return array(TABULA_BOX_QUADS, box); }
    public static Object[] tabulaVertices(Object quad) { return array(TABULA_QUAD_VERTICES, quad); }
    public static Object tabulaNormal(Object quad) { return get(TABULA_QUAD_NORMAL, quad); }
    public static Object tabulaPosition(Object vertex) { return get(TABULA_VERTEX_POSITION, vertex); }
    public static float tabulaU(Object vertex) { return (float) get(TABULA_VERTEX_U, vertex); }
    public static float tabulaV(Object vertex) { return (float) get(TABULA_VERTEX_V, vertex); }

    public static Object[] basicQuads(Object box) { return array(BASIC_BOX_QUADS, box); }
    public static Object[] basicVertices(Object quad) { return array(BASIC_QUAD_VERTICES, quad); }
    public static Object basicNormal(Object quad) { return get(BASIC_QUAD_NORMAL, quad); }
    public static Object basicPosition(Object vertex) { return get(BASIC_VERTEX_POSITION, vertex); }
    public static float basicU(Object vertex) { return (float) get(BASIC_VERTEX_U, vertex); }
    public static float basicV(Object vertex) { return (float) get(BASIC_VERTEX_V, vertex); }

    private static Object[] array(Field field, Object owner) {
        return (Object[]) get(field, owner);
    }

    private static Object get(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to read Citadel model geometry", exception);
        }
    }

    private static Field field(String className, String name) {
        try {
            Class<?> type = Class.forName(className, false, CitadelGeometryAccess.class.getClassLoader());
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return null;
        }
    }
}
