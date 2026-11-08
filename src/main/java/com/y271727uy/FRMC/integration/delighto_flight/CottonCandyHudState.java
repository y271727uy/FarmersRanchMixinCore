package com.y271727uy.FRMC.integration.delighto_flight;

import com.cloudmeow.delightoflight.block.entity.CottonCandyMachineBlockEntity;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.world.level.Level;

/** Tracks synchronized machines that may show the progress HUD. */
public final class CottonCandyHudState {
    private static final Set<CottonCandyMachineBlockEntity> SPINNING =
            Collections.newSetFromMap(new WeakHashMap<>());

    private CottonCandyHudState() {
    }

    public static void onStateLoaded(CottonCandyMachineBlockEntity machine) {
        Level level = machine.getLevel();
        if (level != null && !level.isClientSide()) {
            return;
        }

        // A chunk packet can deserialize the block entity before assigning its level.
        synchronized (SPINNING) {
            if (machine.isSpinning() && machine.getSpinningPlayerId() != null) {
                SPINNING.add(machine);
            } else {
                SPINNING.remove(machine);
            }
        }
    }

    public static boolean mayShowProgress(Level level, UUID playerId) {
        synchronized (SPINNING) {
            if (SPINNING.isEmpty()) {
                return false;
            }
            Iterator<CottonCandyMachineBlockEntity> machines = SPINNING.iterator();
            while (machines.hasNext()) {
                CottonCandyMachineBlockEntity machine = machines.next();
                if (machine.isRemoved()) {
                    machines.remove();
                } else if (machine.getLevel() == level && machine.isSpinning()
                        && playerId.equals(machine.getSpinningPlayerId())
                        && !machine.getCurrentOutput().isEmpty() && machine.getSpinTimeTotal() > 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
