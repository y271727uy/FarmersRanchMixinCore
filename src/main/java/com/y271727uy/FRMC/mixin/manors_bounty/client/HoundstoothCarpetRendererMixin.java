package com.y271727uy.FRMC.mixin.manors_bounty.client;

import net.mcreator.manors_bounty.block.entity.HoundstoothInfiniteCarpetTileEntity;
import net.mcreator.manors_bounty.block.renderer.HoundstoothInfiniteCarpetTileRenderer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

@Pseudo
@Mixin(HoundstoothInfiniteCarpetTileRenderer.class)
public abstract class HoundstoothCarpetRendererMixin extends GeoBlockRenderer<HoundstoothInfiniteCarpetTileEntity> {
    protected HoundstoothCarpetRendererMixin(GeoModel<HoundstoothInfiniteCarpetTileEntity> model) {
        super(model);
    }

    @Override
    public boolean shouldRender(HoundstoothInfiniteCarpetTileEntity carpet, Vec3 cameraPos) {
        return carpet.getBlockState().getRenderShape() != RenderShape.MODEL && super.shouldRender(carpet, cameraPos);
    }
}
