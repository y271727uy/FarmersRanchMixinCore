package com.y271727uy.FRMC.integration.farmersrespite;

import java.util.List;
import net.minecraftforge.fluids.FluidStack;

public interface KettleRecipeFluids {
    List<FluidStack> frmc$getAdditionalInputs();

    void frmc$setAdditionalInputs(List<FluidStack> inputs);
}
