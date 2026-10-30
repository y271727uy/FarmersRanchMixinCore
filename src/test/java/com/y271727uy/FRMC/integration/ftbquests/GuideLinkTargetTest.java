package com.y271727uy.FRMC.integration.ftbquests;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

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
        var properties = java.util.Map.of("guideme", "ae2:guide/inscribers#presses", "text", "压印器");
        Component component = GuideLinkComponent.parse("guideme: ae2:guide/inscribers#presses text: 压印器", properties);

        assertTrue(component.getString().contains("压印器"));
        assertEquals("guideme:ae2:guide/inscribers#presses", component.getStyle().getClickEvent().getValue());
    }

    @Test
    void rejectsMalformedTargets() {
        assertTrue(GuideLinkTarget.parse("ae2:guide/machines/#top").isEmpty());
        assertTrue(GuideLinkTarget.parse("ae2:guide/bad page").isEmpty());
        assertTrue(GuideLinkTarget.parse("guide/page").isEmpty());
        assertTrue(GuideLinkTarget.parse("ae2:guide/page#bad anchor").isEmpty());
        assertTrue(GuideLinkTarget.parse("").isEmpty());
    }
}
