package com.y271727uy.FRMC.mixin.foragersinsight;

import com.tiomadre.foragersinsight.core.other.FIEvents;
import com.tiomadre.foragersinsight.core.other.OdorousAvoidance;
import com.tiomadre.foragersinsight.core.other.toolevents.SapTrapBaitGoal;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

/**
 * Prevents Forager's Insight from attaching its sap-trap search goal to every PathfinderMob.
 *
 * <p>The shipped FIEvents implementation adds SapTrapBaitGoal to every PathfinderMob even
 * though the 3.55 bait table only supports rabbits, chickens, ocelots, foxes, and pigs. The
 * goal then performs a 33x5x33 block scan for entities that can never match a bait.</p>
 */
@Mixin(value = FIEvents.class, remap = false)
public abstract class FIEventsMixin {
    /**
     * Replaces the upstream broad registration while preserving its server-side guard,
     * duplicate checks, priority, and OdorousAvoidance behavior.
     */
    @Overwrite(remap = false)
    public static void onMobJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }

        boolean alreadyHasOdorousAvoidance = mob.goalSelector.getAvailableGoals().stream()
                .map(net.minecraft.world.entity.ai.goal.WrappedGoal::getGoal)
                .anyMatch(goal -> goal instanceof OdorousAvoidance);
        if (!alreadyHasOdorousAvoidance) {
            mob.goalSelector.addGoal(3, new OdorousAvoidance(mob, 1.1D, 1.25D));
        }

        if (!isSapTrapTarget(mob)) {
            return;
        }

        boolean alreadyHasSapTrapBait = mob.goalSelector.getAvailableGoals().stream()
                .map(net.minecraft.world.entity.ai.goal.WrappedGoal::getGoal)
                .anyMatch(goal -> goal instanceof SapTrapBaitGoal);
        if (!alreadyHasSapTrapBait) {
            mob.goalSelector.addGoal(3, new SapTrapBaitGoal(mob));
        }
    }

    @Unique
    private static boolean isSapTrapTarget(PathfinderMob mob) {
        return mob instanceof Rabbit
                || mob instanceof Chicken
                || mob instanceof Ocelot
                || mob instanceof Fox
                || mob instanceof Pig;
    }
}
