package com.y271727uy.FRMC.mixin.kubejs;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.latvian.mods.kubejs.script.ScriptManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * isClassAllowed：Java.loadClass 出口。
 * 6029 / 5367 掏空 ClassFilter 之后，{@code org.apache.http...HttpGet} 仍会被打成 false。
 *
 * visibleToScripts：犀牛成员访问出口。
 * KubeJS 原版只在 type == 2 查过滤器；这里对所有 type 套同一份名单，但不改成走完整
 * ClassFilter，所以 {@code java.lang.reflect} 仍可用。
 *
 * 策略写在本类方法体里，没有可写的静态名单字段。脚本反射 integration 入口改不到这里。
 */
@Pseudo
@Mixin(targets = "dev.latvian.mods.kubejs.script.ScriptManager", remap = false)
public abstract class ScriptManagerMixin {
    @ModifyReturnValue(method = "isClassAllowed", at = @At("RETURN"), require = 0)
    private boolean frmc$denyScriptNetworkClasses(boolean allowed, String className) {
        return allow(allowed, className);
    }

    @ModifyReturnValue(method = "visibleToScripts", at = @At("RETURN"), require = 0)
    private boolean frmc$denyScriptNetworkMembers(boolean visible, String className) {
        return allow(visible, className);
    }

    @Unique
    private static boolean allow(boolean original, String className) {
        return original && !isBlocked(className);
    }

    @Unique
    private static boolean isBlocked(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }

        String name = className.replace('/', '.').replace('$', '.');
        String[] prefixes = {
            "java.net",
            "javax.net",
            "io.netty",
            "sun.net",
            "com.sun.net",
            "org.apache.http",
            "org.apache.hc",
            "org.apache.commons.httpclient",
            "okhttp3",
            "com.squareup.okhttp",
            "net.minecraft.util.HttpUtil"
        };
        for (String prefix : prefixes) {
            if (name.equals(prefix) || name.startsWith(prefix + ".")) {
                return true;
            }
        }
        return false;
    }
}
