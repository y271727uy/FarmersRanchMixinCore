package com.y271727uy.FRMC.mixin.touhoulittlemaid;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.controller.AnimationController;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.controller.AnimationControllerContext;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.keyframe.AnimationPoint;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.keyframe.KeyFramePoint;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.keyframe.bone.BoneKeyFrame;
import java.util.List;
import java.util.RandomAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AnimationController.class, remap = false)
public abstract class AnimationControllerMixin {
    @Inject(method = "getKeyFramePointAtTick", at = @At("HEAD"), cancellable = true)
    private void frmc$findKeyFrame(List<BoneKeyFrame> track, double tick, AnimationControllerContext context,
                                   CallbackInfoReturnable<AnimationPoint> cir) {
        if (!(track instanceof RandomAccess) || track.size() < 8 || !(tick >= track.get(0).getStartTick())) {
            return;
        }
        // JsonKeyFrameUtils sorts the track before BoneKeyFrameProcessor bakes it.
        int low = 0;
        int high = track.size();
        while (low + 1 < high) {
            int middle = (low + high) >>> 1;
            if (track.get(middle).getStartTick() > tick) {
                high = middle;
            } else {
                low = middle;
            }
        }
        BoneKeyFrame frame = track.get(low);
        cir.setReturnValue(new KeyFramePoint(tick - frame.getStartTick(), frame, context));
    }
}
