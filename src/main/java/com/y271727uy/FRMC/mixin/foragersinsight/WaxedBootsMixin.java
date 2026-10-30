package com.y271727uy.FRMC.mixin.foragersinsight;

import net.minecraft.nbt.CompoundTag;
import com.tiomadre.foragersinsight.core.other.toolevents.WaxedBoots;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/** Avoids allocating an ItemStack tag for every living entity without waxed boots. */
@Mixin(value = WaxedBoots.class, remap = false)
public abstract class WaxedBootsMixin {
    /**
     * The upstream implementation uses getOrCreateTag() for a read-only lookup.
     * A missing tag is equivalent to a duration of zero, so getTag() preserves
     * behavior while removing the per-entity allocation/mutation.
     */
    @Overwrite
    public static int getWaxedDuration(net.minecraft.world.item.ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt("ForagersInsightWaxedDuration");
    }
}
