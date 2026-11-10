package com.y271727uy.FRMC.integration.ftbquests;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Map;
import java.util.regex.Pattern;

/** Turns an FTB description token into a link the quest screen can intercept. */
public final class GuideLinkComponent {
    public static final String KEY = "guideme";
    private static final Pattern SPACED_LINK = Pattern.compile("guideme:\\s*(\\S+)(?:\\s+text:\\s*(.*))?");

    private GuideLinkComponent() {
    }

    public static Component parse(String raw, Map<String, String> properties) {
        if (properties == null || !properties.containsKey(KEY)) {
            return null;
        }
        String target = properties.get(KEY);
        String label = properties.get("text");
        if (raw != null && (target == null || target.isEmpty() || "".equals(label))) {
            // FTB splits properties on spaces, so a space after ':' leaves an empty value.
            var match = SPACED_LINK.matcher(raw.strip());
            if (match.matches()) {
                target = match.group(1);
                label = match.group(2);
                if (label != null) {
                    label = label.replace("%20", " ");
                }
            }
        }
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
