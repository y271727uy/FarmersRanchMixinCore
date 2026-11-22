package com.y271727uy.FRMC.integration.farmersrespite;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public final class KettleFluidTank extends FluidTank {
    public static final int MAX_FLUIDS = 4;
    private static final String ADDITIONAL_FLUIDS = "FrmcAdditionalFluids";

    private final List<FluidStack> additional = new ArrayList<>(MAX_FLUIDS - 1);
    private final Runnable onChange;
    private boolean deferPromotion;
    private boolean changedDuringDrain;

    public KettleFluidTank(int capacity, Runnable onChange) {
        super(capacity);
        this.onChange = onChange;
    }

    @Override
    public int getTanks() {
        return MAX_FLUIDS;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (tank == 0) {
            return fluid;
        }
        return tank > 0 && tank <= additional.size() ? additional.get(tank - 1) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank >= 0 && tank < MAX_FLUIDS ? getFluidInTank(tank).getAmount() + getSpace() : 0;
    }

    public int getTotalAmount() {
        return fluid.getAmount() + additional.stream().mapToInt(FluidStack::getAmount).sum();
    }

    @Override
    public int getSpace() {
        return Math.max(0, getCapacity() - getTotalAmount());
    }

    public boolean brew(KettleBrewing.Batch batch, FluidAction action) {
        List<FluidStack> remaining = new ArrayList<>(MAX_FLUIDS);
        for (int slot = 0; slot <= additional.size(); slot++) {
            FluidStack stored = getFluidInTank(slot);
            if (!stored.isEmpty()) {
                remaining.add(stored.copy());
            }
        }
        for (FluidStack input : batch.inputs()) {
            FluidStack stored = remaining.stream().filter(input::isFluidEqual).findFirst().orElse(FluidStack.EMPTY);
            if (input.isEmpty() || stored.getAmount() < input.getAmount()) {
                return false;
            }
            stored.shrink(input.getAmount());
        }
        long finalAmount = batch.output().getAmount();
        FluidStack result = batch.output().copy();
        remaining.removeIf(FluidStack::isEmpty);
        for (FluidStack stored : remaining) {
            finalAmount += stored.getAmount();
        }
        if (result.isEmpty() || !isFluidValid(result) || finalAmount > getCapacity()) {
            return false;
        }
        for (int index = 0; index < remaining.size(); index++) {
            FluidStack stored = remaining.get(index);
            if (stored.isFluidEqual(result)) {
                result.grow(stored.getAmount());
                remaining.remove(index);
                break;
            }
        }
        if (remaining.size() >= MAX_FLUIDS) {
            return false;
        }
        if (action.execute()) {
            fluid = result;
            additional.clear();
            additional.addAll(remaining);
            onContentsChanged();
        }
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(resource)) {
            return 0;
        }
        int index = findFluid(resource);
        if (index < 0 && additional.size() == MAX_FLUIDS - 1) {
            return 0;
        }
        int amount = Math.min(getSpace(), resource.getAmount());
        if (amount > 0 && action.execute()) {
            store(resource, index, amount);
            onContentsChanged();
        }
        return amount;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        int index = resource.isEmpty() ? -1 : findFluid(resource);
        return index < 0 ? FluidStack.EMPTY : drainTank(index, resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int amount, FluidAction action) {
        return drainTank(0, amount, action);
    }

    private FluidStack drainTank(int index, int amount, FluidAction action) {
        FluidStack stored = getFluidInTank(index);
        int drained = Math.min(amount, stored.getAmount());
        if (drained <= 0) {
            return FluidStack.EMPTY;
        }
        FluidStack result = new FluidStack(stored, drained);
        if (action.execute()) {
            stored.shrink(drained);
            if (stored.isEmpty()) {
                if (index == 0) {
                    if (!deferPromotion) {
                        promoteNextFluid();
                    }
                } else {
                    additional.remove(index - 1);
                }
            }
            onContentsChanged();
        }
        return result;
    }

    public <T> T drainPrimaryOnly(Supplier<T> drain) {
        if (additional.isEmpty() || deferPromotion) {
            return drain.get();
        }
        deferPromotion = true;
        try {
            return drain.get();
        } finally {
            deferPromotion = false;
            if (fluid.isEmpty()) {
                promoteNextFluid();
            }
            if (changedDuringDrain) {
                changedDuringDrain = false;
                onContentsChanged();
            }
        }
    }

    @Override
    public void setFluid(FluidStack replacement) {
        // Farmer's Respite replaces the primary fluid when an existing brewing recipe finishes.
        int amount = Math.min(replacement.getAmount(), getCapacity() - getTotalAmount() + fluid.getAmount());
        if (replacement.isEmpty() || amount <= 0) {
            promoteNextFluid();
            return;
        }
        fluid = new FluidStack(replacement, amount);
        for (int index = 0; index < additional.size(); index++) {
            FluidStack stored = additional.get(index);
            if (stored.isFluidEqual(fluid)) {
                fluid.grow(stored.getAmount());
                additional.remove(index);
                break;
            }
        }
    }

    @Override
    public FluidTank readFromNBT(CompoundTag tag) {
        additional.clear();
        fluid = FluidStack.EMPTY;
        super.readFromNBT(tag);
        ListTag rest = tag.getList(ADDITIONAL_FLUIDS, Tag.TAG_COMPOUND);
        for (int index = 0; index < rest.size() && index < MAX_FLUIDS - 1; index++) {
            FluidStack stored = FluidStack.loadFluidStackFromNBT(rest.getCompound(index));
            int amount = Math.min(stored.getAmount(), getSpace());
            if (amount > 0) {
                store(stored, findFluid(stored), amount);
            }
        }
        return this;
    }

    @Override
    public CompoundTag writeToNBT(CompoundTag tag) {
        super.writeToNBT(tag);
        if (additional.isEmpty()) {
            tag.remove(ADDITIONAL_FLUIDS);
        } else {
            ListTag rest = new ListTag();
            for (FluidStack stored : additional) {
                rest.add(stored.writeToNBT(new CompoundTag()));
            }
            tag.put(ADDITIONAL_FLUIDS, rest);
        }
        return tag;
    }

    @Override
    protected void onContentsChanged() {
        if (deferPromotion) {
            changedDuringDrain = true;
        } else {
            onChange.run();
        }
    }

    private void promoteNextFluid() {
        fluid = additional.isEmpty() ? FluidStack.EMPTY : additional.remove(0);
    }

    private int findFluid(FluidStack resource) {
        for (int tank = 0; tank <= additional.size(); tank++) {
            if (getFluidInTank(tank).isFluidEqual(resource)) {
                return tank;
            }
        }
        return -1;
    }

    private void store(FluidStack resource, int index, int amount) {
        if (index >= 0) {
            getFluidInTank(index).grow(amount);
        } else if (fluid.isEmpty()) {
            fluid = new FluidStack(resource, amount);
        } else {
            additional.add(new FluidStack(resource, amount));
        }
    }
}
