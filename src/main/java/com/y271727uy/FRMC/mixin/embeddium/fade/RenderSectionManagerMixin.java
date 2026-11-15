package com.y271727uy.FRMC.mixin.embeddium.fade;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.FRMC.integration.embeddium.fade.ChunkFadeIn;
import com.y271727uy.FRMC.integration.embeddium.fade.FadeSection;
import com.y271727uy.FRMC.integration.embeddium.fade.FadeSectionManager;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMaps;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class RenderSectionManagerMixin implements FadeSectionManager {
    @Shadow @Final private Long2ReferenceMap<RenderSection> sectionByPosition;

    @Override
    public LongSet frmc$collectSettledSections() {
        LongOpenHashSet settled = new LongOpenHashSet(this.sectionByPosition.size());

        for (var entry : Long2ReferenceMaps.fastIterable(this.sectionByPosition)) {
            if (((FadeSection) (Object) entry.getValue()).frmc$isFadeInDecided()) {
                settled.add(entry.getLongKey());
            }
        }

        return settled;
    }

    // 渲染器重载后重新登记的 section：上一个渲染器里已经显示过的不再淡入。
    @WrapOperation(
        method = "onSectionAdded",
        at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ReferenceMap;put(JLjava/lang/Object;)Ljava/lang/Object;"),
        require = 0
    )
    private Object frmc$carryOverSettledSection(Long2ReferenceMap<RenderSection> map, long key, Object section,
                                                Operation<Object> original) {
        LongSet settled = ChunkFadeIn.settledSections;
        if (settled != null && settled.remove(key)) {
            ((FadeSection) section).frmc$skipFadeIn();
        }
        return original.call(map, key, section);
    }
}
