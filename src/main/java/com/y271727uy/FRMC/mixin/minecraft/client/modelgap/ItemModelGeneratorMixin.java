package com.y271727uy.FRMC.mixin.minecraft.client.modelgap;

import com.y271727uy.FRMC.client.modelgap.ModelGapFix;
import java.util.List;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemModelGenerator.class)
public abstract class ItemModelGeneratorMixin {
    @Inject(method = "createSideElements", at = @At("RETURN"))
    private void frmc$adjustGeneratedFaces(
        SpriteContents spriteContents,
        String texture,
        int tintIndex,
        CallbackInfoReturnable<List<BlockElement>> cir
    ) {
        ModelGapFix.enlargeFaces(cir.getReturnValue());
    }

    /**
     * @author MehVahdJukaar, adapted for FRMC
     * @reason Keep transparent-pixel boundaries separate so expanded item quads cannot cover them.
     */
    @Overwrite
    private void createOrExpandSpan(
        List<ItemModelGenerator.Span> spans,
        ItemModelGenerator.SpanFacing facing,
        int pixelX,
        int pixelY
    ) {
        ModelGapFix.createOrExpandSpan(spans, facing, pixelX, pixelY);
    }
}
