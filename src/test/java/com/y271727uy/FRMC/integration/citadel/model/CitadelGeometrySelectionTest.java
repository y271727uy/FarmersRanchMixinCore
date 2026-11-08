package com.y271727uy.FRMC.integration.citadel.model;

import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import java.util.List;
import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CitadelGeometrySelectionTest {
    @Test
    void omittingParentGeometryKeepsChildAtItsOriginalBoneIndex() {
        var model = new TestModel();
        AdvancedModelBox root = box(model);
        AdvancedModelBox parent = box(model);
        AdvancedModelBox child = box(model);
        root.addChild(parent);
        parent.addChild(child);

        var layout = CitadelSkeletonLayoutCache.getOrBuild(root);
        long visible = CitadelGeometrySelection.visibleGeometry(layout);
        assertEquals(0b111L, visible);
        assertEquals(0b101L, CitadelGeometrySelection.omitOwnGeometry(visible, 0b010L));
        assertEquals(0b111L, CitadelGeometrySelection.visibleGeometry(layout));

        parent.showModel = false;
        assertEquals(0b001L, CitadelGeometrySelection.visibleGeometry(layout));
        parent.showModel = true;
        assertEquals(0b111L, CitadelGeometrySelection.visibleGeometry(layout));
    }

    @Test
    void omittingAllVisibleGeometryKeepsTheFullMesh() {
        assertEquals(0b101L, CitadelGeometrySelection.omitOwnGeometry(0b101L, 0b111L));
    }

    private static AdvancedModelBox box(AdvancedEntityModel<?> model) {
        var box = new AdvancedModelBox(model, 0, 0);
        box.addBox(0, 0, 0, 1, 1, 1);
        return box;
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
