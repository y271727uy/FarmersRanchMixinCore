package com.y271727uy.FRMC.mixin.alexscaves;

import com.github.alexmodguy.alexscaves.server.config.BiomeGenerationNoiseCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * 读取 Alex's Caves 群系生成条件。维度列表供 {@code CaveBiomeHookGate} 使用；
 * 其余字段供 {@code integration.alexscaves.locate.AlexBiome}（移植自 BiomeSpy）构建 /locate 用的参数包络。
 */
@Mixin(value = BiomeGenerationNoiseCondition.class, remap = false)
public interface BiomeGenerationNoiseConditionAccessor {
    @Accessor("dimensions")
    List<String> frmc$getDimensions();

    @Accessor("distanceFromSpawn")
    int frmc$getDistanceFromSpawn();

    @Accessor("continentalness")
    float[] frmc$getContinentalness();

    @Accessor("erosion")
    float[] frmc$getErosion();

    @Accessor("humidity")
    float[] frmc$getHumidity();

    @Accessor("temperature")
    float[] frmc$getTemperature();

    @Accessor("weirdness")
    float[] frmc$getWeirdness();

    @Accessor("depth")
    float[] frmc$getDepth();
}
