package com.y271727uy.FRMC.mixin.youkaishomecoming;

import com.y271727uy.FRMC.integration.youkaishomecoming.ManorsBountyCuisineBoardIntegration;
import dev.xkmc.youkaishomecoming.content.pot.table.item.TableItemManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = TableItemManager.class, remap = false)
public class TableItemManagerMixin {
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void frmc$registerManorsBountySumeshiBase(CallbackInfo ci) {
        ManorsBountyCuisineBoardIntegration.registerCuisineBoardBase();
    }
}
