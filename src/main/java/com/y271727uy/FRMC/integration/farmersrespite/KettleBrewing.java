package com.y271727uy.FRMC.integration.farmersrespite;

import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.fluids.FluidStack;
import umpaz.farmersrespite.common.crafting.KettleRecipe;

public final class KettleBrewing {
    private KettleBrewing() {
    }

    public static Batch plan(KettleRecipe recipe, KettleFluidTank tank) {
        FluidStack base = recipe.getFluidIn();
        FluidStack output = recipe.getFluidOut();
        if (base == null || base.isEmpty() || output == null || output.isEmpty()) {
            return null;
        }

        List<FluidStack> extra = ((KettleRecipeFluids) recipe).frmc$getAdditionalInputs();
        if (!extra.isEmpty()) {
            List<FluidStack> inputs = new ArrayList<>(1 + extra.size());
            inputs.add(base);
            inputs.addAll(extra);
            return new Batch(inputs, output);
        }

        for (int slot = 0; slot < tank.getTanks(); slot++) {
            FluidStack stored = tank.getFluidInTank(slot);
            if (stored.isFluidEqual(base) && stored.getAmount() % base.getAmount() == 0
                    && FluidStack.areFluidStackTagsEqual(stored, output)) {
                // Legacy recipes brew the entire matching liquid in their original batch ratio.
                return new Batch(List.of(stored.copy()), new FluidStack(output, stored.getAmount()));
            }
        }
        return null;
    }

    public record Batch(List<FluidStack> inputs, FluidStack output) {
    }
}
