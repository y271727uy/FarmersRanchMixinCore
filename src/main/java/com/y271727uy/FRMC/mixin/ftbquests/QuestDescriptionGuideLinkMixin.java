package com.y271727uy.FRMC.mixin.ftbquests;

import com.y271727uy.FRMC.integration.ftbquests.client.GuideLinkClient;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Opens GuideME links embedded in an FTB quest description before the quest-id handler. */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbquests.client.gui.quests.ViewQuestPanel$QuestDescriptionField", remap = false)
public abstract class QuestDescriptionGuideLinkMixin {
    @Inject(method = "handleCustomClickEvent", at = @At("HEAD"), cancellable = true)
    private void frmc$openGuideMeLink(Style style, CallbackInfoReturnable<Boolean> cir) {
        if (GuideLinkClient.handle(style)) {
            cir.setReturnValue(true);
        }
    }
}
