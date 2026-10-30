package com.y271727uy.FRMC.mixin.ftbquests;

import com.y271727uy.FRMC.integration.ftbquests.PreviewTaskGuard;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = dev.ftb.mods.ftbquests.net.SubmitTaskMessage.class, remap = false)
public abstract class SubmitTaskMessageMixin {
    @Inject(method = "lambda$handle$0", at = @At("HEAD"), cancellable = true, remap = false)
    private static void frmc$requirePreviewConfirmation(Task task, TeamData teamData,
            ServerPlayer player, CallbackInfo callback) {
        PreviewTaskGuard.blockDirectSubmit(task, callback);
    }
}
