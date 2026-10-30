package com.y271727uy.FRMC.integration.ftbquests;

import dev.ftb.mods.ftbquests.quest.task.Task;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the generic submit path from completing a preview before its page has opened. */
public final class PreviewTaskGuard {
    private PreviewTaskGuard() {
    }

    public static void blockDirectSubmit(Task task, CallbackInfo callback) {
        if (task instanceof PreviewTask) {
            callback.cancel();
        }
    }
}
