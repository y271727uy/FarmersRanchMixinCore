package com.y271727uy.frmc.mixinagent;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeepFieldsTest {
    @Test
    void overwritesStaticFinal() throws Exception {
        assertEquals("old", read());
        DeepFields.setStatic(Holder.class, "VALUE", "rewritten");
        assertEquals("rewritten", read());
    }

    @Test
    void opensUnnamedModuleAndJavaLang() throws Exception {
        assertTrue(ModuleOpener.open(DeepFieldsTest.class.getModule()));
        assertTrue(ModuleOpener.openPackage(Class.class.getModule(), "java.lang")
                || Class.class.getModule().isOpen("java.lang"));
        Field module = Class.class.getDeclaredField("module");
        module.setAccessible(true);
        assertEquals(Class.class.getModule(), module.get(Class.class));
    }

    private static String read() throws Exception {
        Field field = Holder.class.getDeclaredField("VALUE");
        field.setAccessible(true);
        return (String) field.get(null);
    }

    static final class Holder {
        static final String VALUE = "old";
    }
}
