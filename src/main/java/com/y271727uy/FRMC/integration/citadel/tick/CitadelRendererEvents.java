package com.y271727uy.FRMC.integration.citadel.tick;

import com.github.alexthe666.citadel.client.event.EventGetFluidRenderType;
import com.github.alexthe666.citadel.client.event.EventGetOutlineColor;
import com.github.alexthe666.citadel.client.event.EventLivingRenderer;
import com.github.alexthe666.citadel.client.event.EventPosePlayerHand;

/** Citadel 在渲染热路径里 post 的事件：没人监听时可以跳过构造和分发。 */
public final class CitadelRendererEvents {
    private static final EventListenerProbe SETUP_ROTATIONS =
            EventListenerProbe.of(EventLivingRenderer.SetupRotations.class);
    private static final EventListenerProbe PRE_SETUP_ANIMATIONS =
            EventListenerProbe.of(EventLivingRenderer.PreSetupAnimations.class);
    private static final EventListenerProbe POST_SETUP_ANIMATIONS =
            EventListenerProbe.of(EventLivingRenderer.PostSetupAnimations.class);
    private static final EventListenerProbe POST_RENDER_MODEL =
            EventListenerProbe.of(EventLivingRenderer.PostRenderModel.class);

    private static final EventListenerProbe POSE_PLAYER_HAND =
            EventListenerProbe.of(EventPosePlayerHand.class);
    private static final EventListenerProbe GET_FLUID_RENDER_TYPE =
            EventListenerProbe.of(EventGetFluidRenderType.class);
    private static final EventListenerProbe GET_OUTLINE_COLOR =
            EventListenerProbe.of(EventGetOutlineColor.class);

    private CitadelRendererEvents() {}

    public static boolean wantsPosePlayerHand() {
        return POSE_PLAYER_HAND.hasListeners();
    }

    public static boolean wantsGetFluidRenderType() {
        return GET_FLUID_RENDER_TYPE.hasListeners();
    }

    public static boolean wantsGetOutlineColor() {
        return GET_OUTLINE_COLOR.hasListeners();
    }

    public static boolean wantsSetupRotations() {
        return SETUP_ROTATIONS.hasListeners();
    }

    public static boolean wantsPreSetupAnimations() {
        return PRE_SETUP_ANIMATIONS.hasListeners();
    }

    public static boolean wantsPostSetupAnimations() {
        return POST_SETUP_ANIMATIONS.hasListeners();
    }

    public static boolean wantsPostRenderModel() {
        return POST_RENDER_MODEL.hasListeners();
    }
}
