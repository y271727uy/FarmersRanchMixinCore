package com.y271727uy.FRMC.mixin.delighto_flight;

import com.cloudmeow.delightoflight.effect.WeatherSensingEffect;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = WeatherSensingEffect.class, remap = false)
public abstract class WeatherSensingEffectMixin {
    @Shadow @Final private static UUID SPEED_UUID;
    @Shadow @Final private static UUID ATTACK_SPEED_UUID;
    @Shadow @Final private static UUID DAMAGE_UUID;

    @Inject(method = {"applyEffectTick(Lnet/minecraft/world/entity/LivingEntity;I)V",
            "m_6742_(Lnet/minecraft/world/entity/LivingEntity;I)V"}, at = @At("HEAD"), cancellable = true, require = 1)
    private void frmc$skipUnchangedModifiers(LivingEntity entity, int amplifier, CallbackInfo ci) {
        Level level = entity.level();
        if (level.isClientSide()) {
            return;
        }

        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance attackSpeed = entity.getAttribute(Attributes.ATTACK_SPEED);
        AttributeInstance damage = entity.getAttribute(Attributes.ATTACK_DAMAGE);
        int strength = amplifier + 1;

        boolean unchanged;
        if (level.isThundering()) {
            unchanged = frmc$matches(damage, DAMAGE_UUID, 3.0F * strength, Operation.ADDITION)
                    && frmc$absent(speed, SPEED_UUID) && frmc$absent(attackSpeed, ATTACK_SPEED_UUID);
        } else if (level.isRaining()) {
            unchanged = frmc$matches(speed, SPEED_UUID, 0.2F * strength, Operation.MULTIPLY_BASE)
                    && frmc$absent(attackSpeed, ATTACK_SPEED_UUID) && frmc$absent(damage, DAMAGE_UUID);
        } else {
            unchanged = frmc$matches(attackSpeed, ATTACK_SPEED_UUID, 0.1F * strength, Operation.MULTIPLY_TOTAL)
                    && frmc$absent(speed, SPEED_UUID) && frmc$absent(damage, DAMAGE_UUID);
        }

        if (unchanged) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean frmc$matches(AttributeInstance attribute, UUID id, double amount, Operation operation) {
        if (attribute == null) {
            // Some mobs lack the attribute this weather would modify.
            return true;
        }
        AttributeModifier modifier = attribute.getModifier(id);
        return modifier != null && modifier.getAmount() == amount && modifier.getOperation() == operation;
    }

    @Unique
    private static boolean frmc$absent(AttributeInstance attribute, UUID id) {
        return attribute == null || attribute.getModifier(id) == null;
    }
}
