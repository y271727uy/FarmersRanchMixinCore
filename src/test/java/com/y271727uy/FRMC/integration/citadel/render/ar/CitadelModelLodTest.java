package com.y271727uy.FRMC.integration.citadel.render.ar;

import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import java.util.List;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitadelModelLodTest {
    @Test
    void plansOneMaskFromMaterialBoundsAndSubmittedVertexCounts() {
        var bounds = new CitadelModelLod.MeshBounds(3);
        bounds.add(0, 24, 0, 0, 0, 0.0625F, 0.0625F, 0.0625F);
        bounds.add(1, 144, -0.5F, -0.5F, -0.5F, 0.5F, 0.5F, 0.5F);
        bounds.add(2, 24, 0, 0, 0, 0.0625F, 0.0625F, 0.0625F);
        var plan = bounds.plan(192);
        assertEquals(0b100L, plan.omitted());
        assertEquals(168, plan.vertices());
        assertNull(bounds.plan(300));
    }

    @Test
    void keepsDetailWhenCloseOrEnlargedByAnAnimatedAncestor() {
        var bounds = new CitadelModelLod.MeshBounds(3);
        bounds.add(0, 72, -0.5F, -0.5F, -0.5F, 0.5F, 0.5F, 0.5F);
        bounds.add(2, 24, 0, 0, 0, 0.0625F, 0.0625F, 0.0625F);
        var plan = bounds.plan(96);
        var model = new TestModel();
        var root = new AdvancedModelBox(model);
        var parent = new AdvancedModelBox(model);
        var detail = new AdvancedModelBox(model);
        BasicModelPart[] nodes = {root, parent, detail};
        int[] parents = {-1, 0, 1};
        var lod = new CitadelModelLod();
        var projection = new Matrix4f().perspective((float) Math.toRadians(70), 16.0F / 9.0F, 0.05F, 256);
        var far = new Matrix4f().translation(0, 0, -80);
        assertTrue(lod.fits(plan, nodes, parents, far, projection, 1920, 1080));
        assertFalse(lod.fits(plan, nodes, parents, new Matrix4f().translation(0, 0, -25), projection, 1920, 1080));
        assertFalse(lod.fits(plan, nodes, parents, far, new Matrix4f(), 1920, 1080));

        parent.scaleX = 4;
        parent.scaleChildren = true;
        assertFalse(lod.fits(plan, nodes, parents, far, projection, 1920, 1080));
        parent.scaleChildren = false;
        assertTrue(lod.fits(plan, nodes, parents, far, projection, 1920, 1080));

        parent.rotationPointZ = 70 * 16;
        assertFalse(lod.fits(plan, nodes, parents, far, projection, 1920, 1080));
    }

    private static final class TestModel extends AdvancedEntityModel<Entity> {
        @Override
        public Iterable<AdvancedModelBox> getAllParts() {
            return List.of();
        }

        @Override
        public Iterable<BasicModelPart> parts() {
            return List.of();
        }

        @Override
        public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                              float ageInTicks, float netHeadYaw, float headPitch) {
        }
    }
}
