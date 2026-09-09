package com.y271727uy.FRMC.mixin.vinery;

import com.y271727uy.FRMC.integration.vinery.GrapevinePotCrusherCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.ItemStack;

@Pseudo
@Mixin(targets = "net.satisfy.vinery.core.block.GrapevinePotBlock", remap = false)
public abstract class GrapevinePotBlockMixin extends Block {
    protected GrapevinePotBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = {"fallOn", "m_142072_"}, at = @At("TAIL"), require = 0, remap = false)
    private void frmc$rememberCrusher(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance, CallbackInfo ci) {
        if (level.isClientSide || !(entity instanceof LivingEntity)) {
            return;
        }
        Property<?> stageProperty = state.getBlock().getStateDefinition().getProperty("stage");
        if (stageProperty instanceof IntegerProperty integerProperty && state.getValue(integerProperty) >= 3) {
            GrapevinePotCrusherCache.set(level, pos, entity);
        }
    }

    @Inject(method = {"use", "m_6227_"}, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void frmc$deferJuiceToServer(BlockState state, Level level, BlockPos pos, Player player,
                                         InteractionHand hand, BlockHitResult hit,
                                         CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Redirect(
        method = {"use", "m_6227_"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;add(Lnet/minecraft/world/item/ItemStack;)Z", remap = true),
        require = 0,
        remap = false
    )
    private boolean frmc$tagJuice(Inventory inventory, ItemStack output, BlockState state, Level level, BlockPos pos,
                                   Player player, InteractionHand hand, BlockHitResult hit) {
        String crusher = level.isClientSide ? "" : GrapevinePotCrusherCache.get(level, pos);
        if (!crusher.isEmpty()) {
            output.getOrCreateTag().putString("CrusherName", crusher);
        }
        boolean added = inventory.add(output);
        if (added && !crusher.isEmpty()) {
            GrapevinePotCrusherCache.clear(level, pos);
        }
        return added;
    }
}
