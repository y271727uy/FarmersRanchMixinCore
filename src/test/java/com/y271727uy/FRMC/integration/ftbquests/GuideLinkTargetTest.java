package com.y271727uy.FRMC.integration.ftbquests;

import dev.ftb.mods.ftblibrary.util.client.ClientTextComponentUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideLinkTargetTest {
    @Test
    void parsesPageAnchorAndFolder() {
        GuideLinkTarget page = GuideLinkTarget.parse("ae2:guide/inscribers#presses").orElseThrow();
        assertEquals("ae2:guide", page.guideKey());
        assertEquals("inscribers", page.pagePath());
        assertEquals("presses", page.anchor());
        assertFalse(page.folder());

        GuideLinkTarget folder = GuideLinkTarget.parse("ae2:guide/machines/").orElseThrow();
        assertEquals("machines", folder.pagePath());
        assertTrue(folder.folder());

        GuideLinkTarget guide = GuideLinkTarget.parse("ae2:guide").orElseThrow();
        assertEquals("", guide.pagePath());
        assertEquals("ae2:guide", guide.display());
    }

    @Test
    void keepsTargetAfterFtbPropertySplit() {
        ClientTextComponentUtils.addCustomParser(GuideLinkComponent::parse);

        Component compact = ClientTextComponentUtils.parse("{guideme:ae2:guide/inscribers#presses text:Inscriber%20Press}");
        assertEquals("Inscriber Press", compact.getString());
        assertEquals("guideme:ae2:guide/inscribers#presses", clickTarget(compact));

        Component questExample = ClientTextComponentUtils.parse("先看 {guideme: ae2:guide/inscribers#presses text: 压印器}。");
        assertEquals("先看 压印器。", questExample.getString());
        assertEquals("guideme:ae2:guide/inscribers#presses", clickTarget(questExample));

        Component spaced = ClientTextComponentUtils.parse("{guideme: ae2:guide/inscribers.md#presses text: Inscriber Press}");
        assertEquals("Inscriber Press", spaced.getString());
        assertEquals("guideme:ae2:guide/inscribers.md#presses", clickTarget(spaced));
    }

    private static String clickTarget(Component component) {
        return component.visit((style, text) -> Optional.ofNullable(style.getClickEvent())
                        .filter(event -> event.getAction() == ClickEvent.Action.CHANGE_PAGE)
                        .map(ClickEvent::getValue), Style.EMPTY)
                .orElseThrow();
    }

    @Test
    void rejectsMalformedTargets() {
        assertTrue(GuideLinkTarget.parse("ae2:guide/machines/#top").isEmpty());
        assertTrue(GuideLinkTarget.parse("ae2:guide/bad page").isEmpty());
        assertTrue(GuideLinkTarget.parse("ae2:guide/.md").isEmpty());
        assertTrue(GuideLinkTarget.parse("ae2:guide/machines.md/").isEmpty());
        assertTrue(GuideLinkTarget.parse("guide/page").isEmpty());
        assertTrue(GuideLinkTarget.parse("ae2:guide/page#bad anchor").isEmpty());
        assertTrue(GuideLinkTarget.parse("").isEmpty());
    }
}
