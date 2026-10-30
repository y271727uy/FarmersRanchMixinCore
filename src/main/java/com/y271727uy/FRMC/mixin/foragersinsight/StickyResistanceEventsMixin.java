package com.y271727uy.FRMC.mixin.foragersinsight;

import com.tiomadre.foragersinsight.core.other.StickyResistanceEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/** Avoids touching persistent NBT for entities that cannot have the effect. */
@Mixin(value = StickyResistanceEvents.class, remap = false)
public abstract class StickyResistanceEventsMixin {
    private static final java.util.UUID SPEED_BOOST_UUID =
            java.util.UUID.fromString("8b341286-6da1-4a7d-a35b-4fb71d37d678");
    /**
     * The end-tick tag is only written while Sticky Resistance is active. Check
     * that effect first; ordinary living entities now take one effect lookup
     * instead of creating/accessing their persistent-data compound.
     */
    @Overwrite
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()
                || entity.getAttribute(Attributes.MOVEMENT_SPEED) == null
                || entity.getAttribute(Attributes.MOVEMENT_SPEED)
                    .getModifier(SPEED_BOOST_UUID) == null) {
            return;
        }
        net.minecraft.nbt.CompoundTag data = entity.getPersistentData();
        String key = "ForagersInsightStickyResistanceSpeedEndTick";
        long endTick = data.getLong(key);
        if (endTick == 0L || entity.level().getGameTime() < endTick) {
            return;
        }
        removeSpeedBoost(entity);
        data.remove(key);
    }

    private static void removeSpeedBoost(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(SPEED_BOOST_UUID);
        }
    }
}
