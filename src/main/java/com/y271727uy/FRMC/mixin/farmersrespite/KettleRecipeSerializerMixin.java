package com.y271727uy.FRMC.mixin.farmersrespite;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.y271727uy.FRMC.integration.farmersrespite.KettleFluidTank;
import com.y271727uy.FRMC.integration.farmersrespite.KettleRecipeFluids;
import com.y271727uy.FRMC.integration.farmersrespite.KettleSlots;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import umpaz.farmersrespite.common.crafting.KettleRecipe;

@Mixin(value = KettleRecipe.Serializer.class, remap = false)
public abstract class KettleRecipeSerializerMixin {
    @Unique
    private static final String FRMC_FLUID_INPUTS = "FrmcKettleFluidInputs";

    @WrapMethod(method = "fromJson(Lnet/minecraft/resources/ResourceLocation;Lcom/google/gson/JsonObject;)Lumpaz/farmersrespite/common/crafting/KettleRecipe;",
            require = 1)
    private KettleRecipe frmc$readMultiFluidRecipe(ResourceLocation id, JsonObject json, Operation<KettleRecipe> original) {
        if (!GsonHelper.isArrayNode(json, "base")) {
            return original.call(id, json);
        }
        var bases = GsonHelper.getAsJsonArray(json, "base");
        if (bases.size() < 2 || bases.size() > KettleFluidTank.MAX_FLUIDS) {
            throw new JsonParseException("A multi-fluid brewing recipe must specify 2 to 4 distinct base fluids");
        }
        List<FluidStack> inputs = new ArrayList<>(bases.size());
        for (int index = 0; index < bases.size(); index++) {
            JsonObject base = GsonHelper.convertToJsonObject(bases.get(index), "base[" + index + "]");
            ResourceLocation fluidId = ResourceLocation.tryParse(GsonHelper.getAsString(base, "fluid"));
            var fluid = fluidId == null ? null : ForgeRegistries.FLUIDS.getValue(fluidId);
            int amount = GsonHelper.getAsInt(base, "count");
            FluidStack input = fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, amount);
            if (input.isEmpty() || inputs.stream().anyMatch(input::isFluidEqual)) {
                throw new JsonParseException("Invalid or repeated base fluid at base[" + index + "]");
            }
            inputs.add(input);
        }
        if (GsonHelper.getAsInt(GsonHelper.getAsJsonObject(json, "result"), "count") <= 0) {
            throw new JsonParseException("A multi-fluid brewing recipe must specify a positive output count");
        }

        JsonObject compatibleJson = json.deepCopy();
        compatibleJson.add("base", bases.get(0).deepCopy());
        KettleRecipe recipe = original.call(id, compatibleJson);
        if (recipe.getFluidOut().isEmpty()) {
            throw new JsonParseException("A multi-fluid brewing recipe must produce a nonempty fluid");
        }
        ((KettleRecipeFluids) recipe).frmc$setAdditionalInputs(inputs.subList(1, inputs.size()));
        return recipe;
    }

    @ModifyArg(method = "toNetwork(Lnet/minecraft/network/FriendlyByteBuf;Lumpaz/farmersrespite/common/crafting/KettleRecipe;)V",
            at = @At(value = "INVOKE", target =
                    "Lnet/minecraft/network/FriendlyByteBuf;writeFluidStack(Lnet/minecraftforge/fluids/FluidStack;)V", ordinal = 0),
            index = 0, require = 1)
    private FluidStack frmc$syncAdditionalInputs(FluidStack base, @Local(argsOnly = true) KettleRecipe recipe) {
        List<FluidStack> extra = ((KettleRecipeFluids) recipe).frmc$getAdditionalInputs();
        if (extra.isEmpty()) {
            return base;
        }
        FluidStack encoded = base.copy();
        ListTag inputs = new ListTag();
        for (FluidStack input : extra) {
            inputs.add(input.writeToNBT(new CompoundTag()));
        }
        encoded.getOrCreateTag().put(FRMC_FLUID_INPUTS, inputs);
        return encoded;
    }

    @Inject(method = "fromNetwork(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/FriendlyByteBuf;)Lumpaz/farmersrespite/common/crafting/KettleRecipe;",
            at = @At("RETURN"), require = 1)
    private void frmc$readSyncedInputs(ResourceLocation id, FriendlyByteBuf buffer, CallbackInfoReturnable<KettleRecipe> cir) {
        KettleRecipe recipe = cir.getReturnValue();
        FluidStack base = recipe.getFluidIn();
        CompoundTag tag = base.getTag();
        if (tag == null || !tag.contains(FRMC_FLUID_INPUTS, Tag.TAG_LIST)) {
            return;
        }
        ListTag encoded = tag.getList(FRMC_FLUID_INPUTS, Tag.TAG_COMPOUND);
        tag.remove(FRMC_FLUID_INPUTS);
        if (tag.isEmpty()) {
            base.setTag(null);
        }
        if (encoded.isEmpty() || encoded.size() >= KettleFluidTank.MAX_FLUIDS) {
            throw new DecoderException("Invalid fluid count in kettle recipe " + id);
        }
        List<FluidStack> extra = new ArrayList<>(encoded.size());
        for (int index = 0; index < encoded.size(); index++) {
            FluidStack input = FluidStack.loadFluidStackFromNBT(encoded.getCompound(index));
            if (input.isEmpty() || input.isFluidEqual(base) || extra.stream().anyMatch(input::isFluidEqual)) {
                throw new DecoderException("Invalid fluid input in kettle recipe " + id);
            }
            extra.add(input);
        }
        ((KettleRecipeFluids) recipe).frmc$setAdditionalInputs(extra);
    }

    @ModifyConstant(method = "fromJson(Lnet/minecraft/resources/ResourceLocation;Lcom/google/gson/JsonObject;)Lumpaz/farmersrespite/common/crafting/KettleRecipe;",
            constant = @Constant(intValue = 2), require = 1)
    private int frmc$increaseIngredientLimit(int original) {
        return KettleSlots.INPUT_COUNT;
    }

    @ModifyConstant(method = "fromJson(Lnet/minecraft/resources/ResourceLocation;Lcom/google/gson/JsonObject;)Lumpaz/farmersrespite/common/crafting/KettleRecipe;",
            constant = @Constant(stringValue = "Too many ingredients for brewing recipe! The max is 2"), require = 1)
    private String frmc$updateIngredientLimitMessage(String original) {
        return "Too many ingredients for brewing recipe! The max is " + KettleSlots.INPUT_COUNT;
    }
}
