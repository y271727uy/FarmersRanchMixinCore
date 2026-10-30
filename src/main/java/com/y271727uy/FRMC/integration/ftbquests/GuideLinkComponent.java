package com.y271727uy.FRMC.integration.ftbquests;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Map;

/** Turns an FTB description token into a link the quest screen can intercept. */
public final class GuideLinkComponent {
    public static final String KEY = "guideme";

    private GuideLinkComponent() {
    }

    public static Component parse(String raw, Map<String, String> properties) {
        if (properties == null || !properties.containsKey(KEY)) {
            return null;
        }
        String target = properties.get(KEY);
        String label = properties.getOrDefault("text", target == null ? "" : target);
        return link(label, target);
    }

    public static Component link(String label, String target) {
        GuideLinkTarget parsed = GuideLinkTarget.parse(target).orElse(null);
        MutableComponent text = Component.literal(label == null || label.isBlank() ? (parsed == null ? "" : parsed.display()) : label);
        if (parsed == null) {
            return text.withStyle(Style.EMPTY
                    .withColor(ChatFormatting.DARK_GRAY)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Component.translatable("frmc.ftbquests.guideme.invalid"))));
        }
        return text.withStyle(Style.EMPTY
                .withColor(ChatFormatting.BLUE)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, "guideme:" + parsed.normalized()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("frmc.ftbquests.guideme.hover", parsed.display()))));
    }
}
