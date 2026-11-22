package com.y271727uy.FRMC.mixin.alexsmobs;

import com.github.alexthe666.alexsmobs.world.AMWorldData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** 读取 Alex's Mobs 以世界为 key、从不清理的静态缓存。 */
@Mixin(value = AMWorldData.class, remap = false)
public interface AMWorldDataAccessor {
    @Accessor("dataMap")
    static Map<Level, AMWorldData> frmc$dataMap() {
        throw new AssertionError();
    }
}
