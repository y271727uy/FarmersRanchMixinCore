package com.y271727uy.FRMC.mixin.eggdelight;

import com.y271727uy.FRMC.integration.eggdelight.EggDelightCookingHelper;
import net.minecraft.world.level.LevelAccessor;
import net.mcreator.eggdelight.init.EggdelightModBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

/** Avoids the generated property-name scan on every raw omelet tray update tick. */
@Pseudo
@Mixin(targets = "net.mcreator.eggdelight.procedures.RawOmeletTrayUpdateTickProcedure", remap = false)
public abstract class RawOmeletTrayUpdateTickProcedureMixin {
    /**
     * @author FRMC
     * @reason The generated procedure allocates a position twice, creates a tag key every call,
     * and scans all state properties before every cook operation.
     */
    @Overwrite(remap = false)
    public static void execute(LevelAccessor level, double x, double y, double z) {
        EggDelightCookingHelper.cook(level, x, y, z,
            EggdelightModBlocks.RAW_OMELET_TRAY.get(), EggdelightModBlocks.OMELET_TRAY.get(), true);
    }
}
