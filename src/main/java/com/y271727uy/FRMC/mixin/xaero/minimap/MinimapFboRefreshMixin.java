package com.y271727uy.FRMC.mixin.xaero.minimap;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.y271727uy.FRMC.integration.xaero.MinimapRefreshLimiter;
import com.y271727uy.FRMC.integration.xaero.XaeroPerformanceConfig;
import java.lang.ref.WeakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.graphics.CustomVertexConsumers;
import xaero.common.graphics.ImprovedFramebuffer;
import xaero.common.minimap.MinimapProcessor;
import xaero.common.minimap.render.MinimapFBORenderer;
import xaero.hud.minimap.module.MinimapSession;

@Mixin(value = MinimapFBORenderer.class, remap = false)
public abstract class MinimapFboRefreshMixin {
    @Shadow private ImprovedFramebuffer scalingFramebuffer;
    @Shadow private ImprovedFramebuffer rotationFramebuffer;
    @Shadow private boolean loadedFBO;

    @Unique private final MinimapRefreshLimiter frmc$refresh = new MinimapRefreshLimiter();
    @Unique private WeakReference<Object> frmc$world = new WeakReference<>(null);
    @Unique private WeakReference<Object> frmc$session = new WeakReference<>(null);
    @Unique private ResourceKey<Level> frmc$dimension;
    @Unique private ImprovedFramebuffer frmc$scaling;
    @Unique private ImprovedFramebuffer frmc$rotation;
    @Unique private int frmc$scalingTexture;
    @Unique private int frmc$rotationTexture;
    @Unique private int frmc$viewWidth;
    @Unique private int frmc$layer;
    @Unique private int frmc$shape;
    @Unique private double frmc$dimensionScale;
    @Unique private double frmc$zoom;
    @Unique private boolean frmc$worldMap;
    @Unique private boolean frmc$north;
    @Unique private boolean frmc$cave;
    @Unique private Vec3 frmc$renderPos;

    @WrapMethod(method = "renderChunksToFBO", require = 1)
    private void frmc$refreshCachedMap(MinimapSession session, GuiGraphics graphics, MinimapProcessor minimap,
            Vec3 renderPos, ResourceKey<Level> dimension, double dimensionScale, int viewWidth,
            float partialTick, int layer, boolean worldMap, boolean north, int shape, double ps,
            double pc, boolean cave, CustomVertexConsumers consumers, Operation<Void> original) {
        int cap = XaeroPerformanceConfig.minimapRefreshFps();
        if (cap <= 0) {
            frmc$refresh.invalidate();
            original.call(session, graphics, minimap, renderPos, dimension, dimensionScale,
                    viewWidth, partialTick, layer, worldMap, north, shape, ps, pc, cave, consumers);
            return;
        }
        long started = System.nanoTime();
        double zoom = ((MinimapFBORenderer) (Object) this).getZoom();
        Object world = Minecraft.getInstance().level;
        // Only reuse an actual successful render in the same rendering context.
        // Continuous position/angle changes are deliberately capped with the map
        // contents; discontinuous changes must not display a stale map.
        if (!frmc$refresh.shouldRefresh(started, cap) && loadedFBO && !minimap.isToResetImage()
                && world == frmc$world.get() && session == frmc$session.get() && dimension == frmc$dimension
                && scalingFramebuffer != null && rotationFramebuffer != null
                && scalingFramebuffer == frmc$scaling && rotationFramebuffer == frmc$rotation
                && scalingFramebuffer.getFramebufferTexture() == frmc$scalingTexture
                && rotationFramebuffer.getFramebufferTexture() == frmc$rotationTexture
                && viewWidth == frmc$viewWidth && layer == frmc$layer && shape == frmc$shape
                && dimensionScale == frmc$dimensionScale && zoom == frmc$zoom
                && worldMap == frmc$worldMap && north == frmc$north && cave == frmc$cave
                && frmc$renderPos != null && renderPos.distanceToSqr(frmc$renderPos) < 4096.0D) {
            return;
        }
        // If drawing fails, do not reuse a possibly half-written FBO on retry.
        frmc$refresh.invalidate();
        original.call(session, graphics, minimap, renderPos, dimension, dimensionScale,
                viewWidth, partialTick, layer, worldMap, north, shape, ps, pc, cave, consumers);
        if (loadedFBO && scalingFramebuffer != null && rotationFramebuffer != null) {
            if (frmc$world.get() != world) frmc$world = new WeakReference<>(world);
            if (frmc$session.get() != session) frmc$session = new WeakReference<>(session);
            frmc$dimension = dimension;
            frmc$scaling = scalingFramebuffer;
            frmc$rotation = rotationFramebuffer;
            frmc$scalingTexture = scalingFramebuffer.getFramebufferTexture();
            frmc$rotationTexture = rotationFramebuffer.getFramebufferTexture();
            frmc$viewWidth = viewWidth;
            frmc$layer = layer;
            frmc$shape = shape;
            frmc$dimensionScale = dimensionScale;
            frmc$zoom = zoom;
            frmc$worldMap = worldMap;
            frmc$north = north;
            frmc$cave = cave;
            frmc$renderPos = renderPos;
            frmc$refresh.rendered(started);
        }
    }

    @Inject(method = {"deleteFramebuffers", "loadFrameBuffer", "resetEntityIconsResources"},
            at = @At("HEAD"), require = 3)
    private void frmc$invalidateCachedMap(CallbackInfo ci) {
        frmc$refresh.invalidate();
        frmc$scaling = null;
        frmc$rotation = null;
        frmc$renderPos = null;
        frmc$world.clear();
        frmc$session.clear();
    }
}
