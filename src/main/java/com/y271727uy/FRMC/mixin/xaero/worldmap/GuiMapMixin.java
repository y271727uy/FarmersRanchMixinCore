package com.y271727uy.FRMC.mixin.xaero.worldmap;

import com.y271727uy.FRMC.integration.xaero.XaeroWorldMapIntegration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.GuiMap;
import xaero.map.gui.dropdown.rightclick.RightClickOption;

import java.util.ArrayList;

@Pseudo
@Mixin(targets = "xaero.map.gui.GuiMap", priority = 1100)
public abstract class GuiMapMixin {
    @Shadow(remap = false)
    public abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addButton(T button);

    @Shadow(remap = false)
    private int rightClickX;

    @Shadow(remap = false)
    private int rightClickZ;

    @Shadow(remap = false)
    private ResourceKey<Level> rightClickDim;

    @Shadow(remap = false)
    private double cameraX;

    @Shadow(remap = false)
    private double cameraZ;

    @Shadow(remap = false)
    private double scale;

    @Inject(method = "m_7856_", remap = false, at = @At("TAIL"))
    private void frmc$afterInit(CallbackInfo ci) {
        XaeroWorldMapIntegration.initButton((GuiMap) (Object) this);
    }

    @Inject(method = "m_88315_", remap = false, at = @At("TAIL"))
    private void frmc$afterRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        XaeroWorldMapIntegration.afterRender(
                (GuiMap) (Object) this, graphics, mouseX, mouseY, this.cameraX, this.cameraZ, this.scale);
    }

    @Inject(method = "getRightClickOptions", at = @At("RETURN"), remap = false)
    private void frmc$afterRightClickOptions(CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        ArrayList<RightClickOption> options = cir.getReturnValue();
        if (options == null) {
            return;
        }
        int blockX = this.rightClickX;
        int blockZ = this.rightClickZ;
        ResourceKey<Level> dimension = this.rightClickDim;
        GuiMap map = (GuiMap) (Object) this;
        options.add(new RightClickOption("frmc.blockchecking.map.inspect_chunk", options.size(), map) {
            @Override
            public void onAction(Screen screen) {
                XaeroWorldMapIntegration.inspectChunk(blockX, blockZ, dimension);
            }
        });
        options.add(new RightClickOption("frmc.blockchecking.map.uninspect_chunk", options.size(), map) {
            @Override
            public void onAction(Screen screen) {
                XaeroWorldMapIntegration.uninspectChunk(blockX, blockZ, dimension);
            }
        });
    }
}
