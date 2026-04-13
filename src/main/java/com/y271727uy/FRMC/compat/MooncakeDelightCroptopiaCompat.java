package com.y271727uy.FRMC.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class MooncakeDelightCroptopiaCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOONCAKE_DELIGHT_MODID = "mooncake_delight";
    private static final String CROPTOPIA_MODID = "croptopia";
    private static final String CROPTOPIA_ITEMS_CLASS = "com.chinaex123.mooncake_delight.item.ModCompat.Croptopia.CroptopiaItems";
    private static final String CREATIVE_TABS_CLASS = "com.chinaex123.mooncake_delight.ModCreativeTabs";
    private static final String MOONCAKE_TAB_FIELD = "MOONCAKE_DELIGHT_TAB";
    private static volatile boolean frmc$forcedRegisterAttached;
    private static volatile boolean frmc$registerFailureLogged;
    private static volatile boolean frmc$creativeTabFailureLogged;

    private MooncakeDelightCroptopiaCompat() {
    }

    public static void registerListeners(IEventBus modEventBus) {
        modEventBus.addListener(MooncakeDelightCroptopiaCompat::onBuildCreativeModeTabContents);
    }

    public static void forceRegisterCroptopiaCompat() {
        if (frmc$forcedRegisterAttached || !shouldForceCroptopiaCompat()) {
            return;
        }

        try {
            Method registerMethod = Class.forName(CROPTOPIA_ITEMS_CLASS).getDeclaredMethod("register", IEventBus.class);
            registerMethod.setAccessible(true);
            registerMethod.invoke(null, FMLJavaModLoadingContext.get().getModEventBus());
            frmc$forcedRegisterAttached = true;
            LOGGER.info("FRMC forced Mooncake Delight Croptopia compat item registration without Croptopia installed");
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (!frmc$registerFailureLogged) {
                frmc$registerFailureLogged = true;
                LOGGER.error("FRMC could not force Mooncake Delight Croptopia compat registration", exception);
            }
        }
    }

    private static void onBuildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!shouldForceCroptopiaCompat() || !isMooncakeDelightTab(event)) {
            return;
        }

        try {
            appendCroptopiaCompatItems(event);
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (!frmc$creativeTabFailureLogged) {
                frmc$creativeTabFailureLogged = true;
                LOGGER.error("FRMC could not append Mooncake Delight Croptopia compat items to the creative tab", exception);
            }
        }
    }

    private static boolean shouldForceCroptopiaCompat() {
        return ModList.get().isLoaded(MOONCAKE_DELIGHT_MODID) && !ModList.get().isLoaded(CROPTOPIA_MODID);
    }

    private static boolean isMooncakeDelightTab(BuildCreativeModeTabContentsEvent event) {
        ResourceLocation tabId = event.getTabKey().location();
        if (!MOONCAKE_DELIGHT_MODID.equals(tabId.getNamespace())) {
            return false;
        }

        try {
            Field mooncakeTabField = Class.forName(CREATIVE_TABS_CLASS).getDeclaredField(MOONCAKE_TAB_FIELD);
            mooncakeTabField.setAccessible(true);
            Object tabRegistryObject = mooncakeTabField.get(null);
            if (tabRegistryObject instanceof RegistryObject<?> registryObject) {
                ResourceLocation registryId = registryObject.getId();
                return registryId != null && registryId.equals(tabId);
            }
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (!frmc$creativeTabFailureLogged) {
                frmc$creativeTabFailureLogged = true;
                LOGGER.warn("FRMC could not resolve Mooncake Delight creative tab exactly, falling back to namespace match", exception);
            }
        }

        return true;
    }

    private static void appendCroptopiaCompatItems(BuildCreativeModeTabContentsEvent event) throws ReflectiveOperationException {
        for (Field field : Class.forName(CROPTOPIA_ITEMS_CLASS).getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !RegistryObject.class.isAssignableFrom(field.getType())) {
                continue;
            }

            field.setAccessible(true);
            Object registryObjectValue = field.get(null);
            if (!(registryObjectValue instanceof RegistryObject<?> registryObject) || !registryObject.isPresent()) {
                continue;
            }

            Object registryValue = registryObject.get();
            if (registryValue instanceof Item item) {
                event.accept(item);
            }
        }
    }
}

