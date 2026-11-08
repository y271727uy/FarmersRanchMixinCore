package com.y271727uy.FRMC.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.ai.CreatureAITargetItems;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(value = CreatureAITargetItems.class, targets = {
        "com.github.alexthe666.alexsmobs.entity.EntityBlueJay$AIScatter",
        "com.github.alexthe666.alexsmobs.entity.EntitySeagull$AIScatter",
        "com.github.alexthe666.alexsmobs.entity.EntityCrow$AIScatter",
        "com.github.alexthe666.alexsmobs.entity.EntityCrow$AIDepositChests"
})
public abstract class NearestBirdTargetMixin {
    @Group(name = "frmc$nearestTarget", min = 1, max = 1)
    @Redirect(method = "canUse", at = @At(value = "INVOKE", remap = false,
            target = "Ljava/util/Collections;sort(Ljava/util/List;Ljava/util/Comparator;)V"), require = 0)
    private <T> void frmc$selectFromCollectionsSort(List<T> candidates, Comparator<? super T> order) {
        frmc$selectFirst(candidates, order);
    }

    @Group(name = "frmc$nearestTarget", min = 1, max = 1)
    @Redirect(method = "canUse", at = @At(value = "INVOKE", remap = false,
            target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"), require = 0)
    private <T> void frmc$selectFromListSort(List<T> candidates, Comparator<? super T> order) {
        frmc$selectFirst(candidates, order);
    }

    @Unique
    private static <T> void frmc$selectFirst(List<T> candidates, Comparator<? super T> order) {
        if (candidates.size() > 1) {
            // These goals only consume index zero. Strict comparison preserves stable-sort ties.
            T first = candidates.get(0);
            T nearest = Collections.min(candidates, order);
            if (first != nearest) {
                candidates.set(0, nearest);
            }
        }
    }
}
