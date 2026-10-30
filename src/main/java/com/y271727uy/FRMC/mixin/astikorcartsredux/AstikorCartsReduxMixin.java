package com.y271727uy.FRMC.mixin.astikorcartsredux;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.function.Supplier;

/** Re-registers Astikor's tab without allowing it to participate in vanilla-tab ordering. */
@Pseudo
@Mixin(targets = "com.jusipat.astikorcartsredux.AstikorCartsRedux", remap = false)
public abstract class AstikorCartsReduxMixin {
    @Shadow
    @Final
    public static RegistryObject<Item> WHEEL;

    @Shadow
    @Final
    public static Map<String, Map<?, RegistryObject<?>>> CARTS;

    @WrapOperation(
        method = "<clinit>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/registries/DeferredRegister;register(Ljava/lang/String;Ljava/util/function/Supplier;)Lnet/minecraftforge/registries/RegistryObject;",
            remap = false
        ),
        require = 2
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static RegistryObject frmc$replaceCreativeTabRegistration(
        DeferredRegister register,
        String name,
        Supplier factory,
        Operation<RegistryObject> original
    ) {
        if (!"astikorcartsredux_tab".equals(name)) {
            return original.call(register, name, factory);
        }

        Supplier<CreativeModeTab> replacement = () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.astikorcartsredux"))
            .icon(() -> WHEEL.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(WHEEL.get());
                CARTS.values().forEach(cartsByWood -> cartsByWood.values().forEach(cart ->
                    output.accept((ItemLike) cart.get())
                ));
            })
            .build();

        return original.call(register, name, replacement);
    }
}
