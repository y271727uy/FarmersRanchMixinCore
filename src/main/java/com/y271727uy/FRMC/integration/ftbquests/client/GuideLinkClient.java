package com.y271727uy.FRMC.integration.ftbquests.client;

import com.y271727uy.FRMC.integration.ftbquests.GuideLinkComponent;
import com.y271727uy.FRMC.integration.ftbquests.GuideLinkTarget;
import dev.ftb.mods.ftblibrary.util.client.ClientTextComponentUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Client-only bridge from an FTB description link to a GuideME page. */
public final class GuideLinkClient {
    private static final String PREFIX = "guideme:";

    private GuideLinkClient() {
    }

    public static void registerParser() {
        ClientTextComponentUtils.addCustomParser(GuideLinkComponent::parse);
    }

    public static boolean handle(Style style) {
        ClickEvent click = style == null ? null : style.getClickEvent();
        if (click == null || click.getAction() != ClickEvent.Action.CHANGE_PAGE || click.getValue() == null
                || !click.getValue().startsWith(PREFIX)) {
            return false;
        }
        GuideLinkTarget target = GuideLinkTarget.parse(click.getValue().substring(PREFIX.length())).orElse(null);
        if (target == null || !open(target)) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(
                        Component.translatable("frmc.ftbquests.guideme.missing").withStyle(ChatFormatting.RED),
                        true);
            }
        }
        return true;
    }

    private static boolean open(GuideLinkTarget target) {
        if (!ModList.get().isLoaded("guideme")) {
            return false;
        }
        try {
            Class<?> guides = Class.forName("guideme.Guides");
            Class<?> anchorType = Class.forName("guideme.PageAnchor");
            Class<?> screenType = Class.forName("guideme.internal.screen.GuideScreen");
            Object guide = guides.getMethod("getById", ResourceLocation.class).invoke(null, target.guideId());
            if (guide == null) {
                return false;
            }
            ResourceLocation pageId = page(guide, target);
            if (pageId == null || guide.getClass().getMethod("getPage", ResourceLocation.class).invoke(guide, pageId) == null) {
                return false;
            }
            Object anchor = target.anchor().isEmpty()
                    ? anchorType.getMethod("page", ResourceLocation.class).invoke(null, pageId)
                    : anchorType.getConstructor(ResourceLocation.class, String.class).newInstance(pageId, target.anchor());
            Minecraft minecraft = Minecraft.getInstance();
            Screen previousScreen = minecraft.screen;
            Screen guideScreen = (Screen) screenType.getMethod("openNew", Class.forName("guideme.Guide"), anchorType)
                    .invoke(null, guide, anchor);
            screenType.getMethod("setReturnToOnClose", Screen.class).invoke(guideScreen, previousScreen);
            minecraft.setScreen(guideScreen);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static ResourceLocation page(Object guide, GuideLinkTarget target) throws ReflectiveOperationException {
        if (!target.folder()) {
            if (target.pagePath().isEmpty()) {
                return (ResourceLocation) guide.getClass().getMethod("getStartPage").invoke(guide);
            }
            return pageId(guide, target.pagePath());
        }

        String prefix = target.pagePath() + "/";
        String namespace = (String) guide.getClass().getMethod("getDefaultNamespace").invoke(guide);
        List<ResourceLocation> matches = new ArrayList<>();
        Object pages = guide.getClass().getMethod("getPages").invoke(guide);
        if (pages instanceof Iterable<?> iterable) {
            for (Object page : iterable) {
                ResourceLocation id = (ResourceLocation) page.getClass().getMethod("getId").invoke(page);
                if (id.getNamespace().equals(namespace) && id.getPath().startsWith(prefix)) {
                    matches.add(id);
                }
            }
        }
        matches.sort(Comparator.comparing(ResourceLocation::getPath));
        for (ResourceLocation id : matches) {
            if (id.getPath().equals(prefix + "index.md")) {
                return id;
            }
        }
        return matches.isEmpty() ? null : matches.get(0);
    }

    private static ResourceLocation pageId(Object guide, String pagePath) throws ReflectiveOperationException {
        Method namespace = guide.getClass().getMethod("getDefaultNamespace");
        String path = pagePath.endsWith(".md") ? pagePath : pagePath + ".md";
        return new ResourceLocation((String) namespace.invoke(guide), path);
    }
}
