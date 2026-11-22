package com.y271727uy.FRMC.client.jade;

import com.y271727uy.FRMC.FRMCMod;
import com.y271727uy.FRMC.integration.farmersrespite.KettleFluidTank;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import snownee.jade.api.Accessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;
import umpaz.farmersrespite.common.block.entity.KettleBlockEntity;

enum KettleFluidJadeProvider implements IServerExtensionProvider<KettleBlockEntity, CompoundTag>,
        IClientExtensionProvider<CompoundTag, FluidView> {
    INSTANCE;

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(FRMCMod.MODID, "kettle_fluid_storage");

    @Override
    public List<ViewGroup<CompoundTag>> getGroups(ServerPlayer player, ServerLevel level,
                                                  KettleBlockEntity kettle, boolean showDetails) {
        if (!(kettle.getFluidTank() instanceof KettleFluidTank tank)) {
            return null;
        }
        int capacity = tank.getCapacity();
        List<CompoundTag> views = new ArrayList<>(tank.getTanks());
        for (int index = 0; index < tank.getTanks(); index++) {
            var fluid = tank.getFluidInTank(index);
            if (!fluid.isEmpty()) {
                var display = JadeFluidObject.of(fluid.getFluid(), fluid.getAmount(), fluid.getTag());
                views.add(FluidView.writeDefault(display, capacity));
            }
        }
        if (views.isEmpty()) {
            views.add(FluidView.writeDefault(JadeFluidObject.empty(), capacity));
        }
        return List.of(new ViewGroup<>(views));
    }

    @Override
    public List<ClientViewGroup<FluidView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<CompoundTag>> groups) {
        return ClientViewGroup.map(groups, FluidView::readDefault, null);
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }
}
