package com.y271727uy.FRMC.mixin.manors_bounty.client;

import net.mcreator.manors_bounty.entity.BresseChickenEntity;
import net.mcreator.manors_bounty.entity.MallardDuckEntity;
import net.mcreator.manors_bounty.entity.TurkeyEntity;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

@Pseudo
@Mixin(value = {BresseChickenEntity.class, MallardDuckEntity.class, TurkeyEntity.class}, remap = false)
public abstract class PoultryMovementAnimationMixin {
    @Unique
    private static final RawAnimation[] FRMC$CHICKEN_MOVEMENT = frmc$movementAnimations("bresse_chicken");
    @Unique
    private static final RawAnimation[] FRMC$DUCK_MOVEMENT = frmc$movementAnimations("mallard_duck");
    @Unique
    private static final RawAnimation[] FRMC$TURKEY_MOVEMENT = frmc$movementAnimations("turkey");

    @Shadow
    public String animationprocedure;

    /**
     * @author FRMC
     * @reason Reuse the three fixed movement animation sequences for each bird.
     */
    @Overwrite(remap = false)
    private PlayState movementPredicate(AnimationState<?> event) {
        if (!this.animationprocedure.equals("empty")) {
            return PlayState.STOP;
        }

        Animal bird = (Animal) (Object) this;
        RawAnimation[] animations = FRMC$TURKEY_MOVEMENT;
        if (bird instanceof BresseChickenEntity) {
            animations = FRMC$CHICKEN_MOVEMENT;
        } else if (bird instanceof MallardDuckEntity) {
            animations = FRMC$DUCK_MOVEMENT;
        }

        if (!bird.onGround()) {
            return event.setAndContinue(animations[2]);
        }
        float swing = event.getLimbSwingAmount();
        boolean walking = event.isMoving() || !(swing > -0.15F && swing < 0.15F);
        return event.setAndContinue(animations[walking ? 1 : 0]);
    }

    @Unique
    private static RawAnimation[] frmc$movementAnimations(String species) {
        String prefix = "animation." + species + ".";
        return new RawAnimation[]{
                RawAnimation.begin().thenLoop(prefix + "ground_idle"),
                RawAnimation.begin().thenLoop(prefix + "ground_walk"),
                RawAnimation.begin().thenLoop(prefix + "air_flying")
        };
    }
}
