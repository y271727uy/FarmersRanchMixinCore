package com.y271727uy.FRMC.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.tileentity.TileEntityLeafcutterAnthill;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = TileEntityLeafcutterAnthill.class, remap = false)
public abstract class TileEntityLeafcutterAnthillMixin {
	@Unique
	private static final int FRMC$TICK_INTERVAL = 100;
	@Unique
	private int frmc$tickCountdown;

	@Inject(method = "serverTick", at = @At("HEAD"), cancellable = true, require = 0)
	private static void frmc$throttleAnthillMaintenance(Level level, BlockPos pos, BlockState state,
			TileEntityLeafcutterAnthill anthill, CallbackInfo ci) {
		TileEntityLeafcutterAnthillMixin self = (TileEntityLeafcutterAnthillMixin) (Object) anthill;
		if (self.frmc$tickCountdown > 0) {
			self.frmc$tickCountdown--;
			ci.cancel();
			return;
		}
		self.frmc$tickCountdown = FRMC$TICK_INTERVAL - 1;
	}
}
