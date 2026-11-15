package com.y271727uy.FRMC.mixin.citadel.tick;

import com.github.alexthe666.citadel.server.tick.TickRateTracker;
import com.github.alexthe666.citadel.server.tick.modifier.TickRateModifier;
import com.y271727uy.FRMC.integration.citadel.tick.TickListAccess;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 加速 Citadel TickRateTracker：空闲时直接短路，实体列表较大时用引用哈希集做 O(1) 查询，
 * 并提供已卸载实体的清理入口。
 */
@Pseudo
@Mixin(value = TickRateTracker.class, remap = false)
public abstract class TickRateTrackerMixin implements TickListAccess {
    /** 实体数达到该值后才启用哈希索引，小列表线性扫描更快。 */
    @Unique
    private static final int FRMC_INDEX_THRESHOLD = 32;

    @Shadow
    public List<TickRateModifier> tickRateModifierList;

    @Shadow
    public List<Entity> specialTickRateEntities;

    @Shadow
    private long masterTickCount;

    @Shadow
    protected abstract void tickBlockedEntity(Entity entity);

    @Shadow
    public abstract boolean hasNormalTickRate(Entity entity);

    @Shadow
    protected abstract void sync();

    @Unique
    private ReferenceOpenHashSet<Entity> frmc$index;

    @Inject(method = "masterTick", at = @At("HEAD"), cancellable = true)
    private void frmc$masterTick(CallbackInfo ci) {
        ci.cancel();
        List<Entity> entities = this.specialTickRateEntities;
        List<TickRateModifier> modifiers = this.tickRateModifierList;

        this.masterTickCount++;

        if (entities.isEmpty() && modifiers.isEmpty()) {
            this.frmc$index = null;
            return;
        }

        frmc$tickBlockedEntities(entities);
        frmc$sweepBlockedEntities(entities);
        frmc$tickModifiers(modifiers);
    }

    @Unique
    private void frmc$tickBlockedEntities(List<Entity> entities) {
        int initialSize = entities.size();
        // tickBlockedEntity 可能在遍历中改动列表，按下标并实时检查边界。
        for (int i = 0; i < initialSize && i < entities.size(); i++) {
            tickBlockedEntity(entities.get(i));
        }
    }

    @Unique
    private void frmc$sweepBlockedEntities(List<Entity> entities) {
        int size = entities.size();
        int write = 0;
        for (int read = 0; read < size; read++) {
            Entity entity = entities.get(read);
            boolean drop = entity.isRemoved() || hasNormalTickRate(entity);
            if (!drop) {
                if (write != read) {
                    entities.set(write, entity);
                }
                write++;
            }
        }
        if (write != size) {
            for (int i = size - 1; i >= write; i--) {
                entities.remove(i);
            }
            this.frmc$index = null;
        }
    }

    @Unique
    private void frmc$tickModifiers(List<TickRateModifier> modifiers) {
        int size = modifiers.size();
        if (size == 0) {
            return;
        }
        for (int i = 0; i < size && i < modifiers.size(); i++) {
            modifiers.get(i).masterTick();
        }

        int afterTick = modifiers.size();
        int write = 0;
        for (int read = 0; read < afterTick; read++) {
            TickRateModifier modifier = modifiers.get(read);
            if (!modifier.doRemove()) {
                if (write != read) {
                    modifiers.set(write, modifier);
                }
                write++;
            }
        }
        if (write != afterTick) {
            for (int i = afterTick - 1; i >= write; i--) {
                modifiers.remove(i);
            }
            sync();
        }
    }

    @Inject(method = "getEntityTickLengthModifier", at = @At("HEAD"), cancellable = true)
    private void frmc$fastTickLengthModifier(Entity entity, CallbackInfoReturnable<Float> cir) {
        if (this.tickRateModifierList.isEmpty()) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "getDayTimeIncrement", at = @At("HEAD"), cancellable = true)
    private void frmc$fastDayTimeIncrement(long timeIn, CallbackInfoReturnable<Long> cir) {
        if (this.tickRateModifierList.isEmpty()) {
            cir.setReturnValue(timeIn);
        }
    }

    @Inject(method = "isTickingHandled", at = @At("HEAD"), cancellable = true)
    private void frmc$fastIsTickingHandled(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        List<Entity> entities = this.specialTickRateEntities;
        int size = entities.size();
        if (size == 0) {
            cir.setReturnValue(false);
            return;
        }
        if (size < FRMC_INDEX_THRESHOLD) {
            return;
        }
        ReferenceOpenHashSet<Entity> index = this.frmc$index;
        if (index == null || index.size() != size) {
            index = new ReferenceOpenHashSet<>(entities);
            this.frmc$index = index;
        }
        cir.setReturnValue(index.contains(entity));
    }

    @Inject(method = "addTickBlockedEntity", at = @At("TAIL"))
    private void frmc$onAddBlockedEntity(Entity entity, CallbackInfo ci) {
        ReferenceOpenHashSet<Entity> index = this.frmc$index;
        if (index != null) {
            index.add(entity);
        }
    }

    @Override
    public void frmc$purgeEntity(Entity entity) {
        List<Entity> entities = this.specialTickRateEntities;
        if (!entities.isEmpty() && entities.remove(entity)) {
            this.frmc$index = null;
        }
    }

    @Override
    public void frmc$purgeLevel(Level level) {
        List<Entity> entities = this.specialTickRateEntities;
        if (entities.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (int i = entities.size() - 1; i >= 0; i--) {
            Entity entity = entities.get(i);
            if (entity == null || entity.level() == level) {
                entities.remove(i);
                changed = true;
            }
        }
        if (changed) {
            this.frmc$index = null;
        }
    }

    @Override
    public void frmc$purgeAll() {
        this.specialTickRateEntities.clear();
        this.frmc$index = null;
    }
}
