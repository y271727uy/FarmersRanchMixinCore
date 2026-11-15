package com.y271727uy.FRMC.integration.embeddium;

import com.y271727uy.FRMC.config.FRMCMixinLoadConfig;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpact;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import org.embeddedt.embeddium.api.OptionPageConstructionEvent;
import org.embeddedt.embeddium.client.gui.options.StandardOptions;

import javax.annotation.Nullable;

/**
 * 把 {@link EmbeddiumPortConfig} 的开关接进 Embeddium 自己的视频设置界面：
 * 视觉类放在“质量”页，性能类放在“性能”页，各自成一组排在该页末尾。
 * 只有对应 mixin 实际会加载时才显示选项，避免出现勾了也没效果的开关。
 */
public final class EmbeddiumPortOptions {
    private static final String MIXIN_PACKAGE = "com.y271727uy.FRMC.mixin.embeddium.";
    private static final String SHADOW_INTERVAL_MIXIN = "com.y271727uy.FRMC.mixin.oculus.shadow.IrisRenderingPipelineShadowIntervalMixin";

    /** 值直接存在 ForgeConfigSpec 里，这里只负责在点“应用”时落盘。 */
    private static final OptionStorage<Object> STORAGE = new OptionStorage<>() {
        @Override
        public Object getData() {
            return EmbeddiumPortConfig.SPEC;
        }

        @Override
        public void save() {
            EmbeddiumPortConfig.SPEC.save();
        }
    };

    private EmbeddiumPortOptions() {
    }

    public static void register() {
        OptionPageConstructionEvent.BUS.addListener(EmbeddiumPortOptions::onPageConstruction);
    }

    private static void onPageConstruction(OptionPageConstructionEvent event) {
        if (!EmbeddiumPortConfig.SPEC.isLoaded()) {
            return;
        }

        OptionGroup group = null;
        if (event.getId().matches(StandardOptions.Pages.QUALITY)) {
            group = group("visual",
                toggle("fade.RenderRegionMixin", "chunk_fade_in", EmbeddiumPortConfig.CHUNK_FADE_IN, OptionImpact.LOW),
                toggle("gui.ControlElementMixin", "gui_animations", EmbeddiumPortConfig.GUI_ANIMATIONS, null),
                shadowInterval());
        } else if (event.getId().matches(StandardOptions.Pages.PERFORMANCE)) {
            group = group("performance",
                toggle("perf.ChunkMeshBufferBuilderMixin", "mesh_buffer_growth_fix", EmbeddiumPortConfig.MESH_BUFFER_GROWTH_FIX, OptionImpact.LOW),
                toggle("perf.SodiumWorldRendererBoxCacheMixin", "block_entity_box_cache", EmbeddiumPortConfig.BLOCK_ENTITY_BOX_CACHE, OptionImpact.LOW),
                toggle("perf.AbstractVertexSorterMixin", "radix_vertex_sort", EmbeddiumPortConfig.RADIX_VERTEX_SORT, OptionImpact.LOW),
                toggle("perf.SodiumWorldRendererSpriteTickMixin", "sprite_marking_per_tick", EmbeddiumPortConfig.SPRITE_MARKING_PER_TICK, OptionImpact.LOW),
                toggle("perf.BlockRendererTintCacheMixin", "block_tint_cache", EmbeddiumPortConfig.BLOCK_TINT_CACHE, OptionImpact.LOW),
                uploadBudget());
        }

        if (group != null) {
            event.addGroup(group);
        }
    }

    @Nullable
    private static OptionGroup group(String path, Option<?>... options) {
        OptionGroup.Builder builder = OptionGroup.createBuilder().setId(new ResourceLocation("frmc", "embeddium_" + path));
        boolean any = false;
        for (Option<?> option : options) {
            if (option != null) {
                builder.add(option);
                any = true;
            }
        }
        return any ? builder.build() : null;
    }

    @Nullable
    private static Option<?> shadowInterval() {
        if (!FRMCMixinLoadConfig.isIntegrationEnabled(SHADOW_INTERVAL_MIXIN)) {
            return null;
        }

        ForgeConfigSpec.IntValue value = EmbeddiumPortConfig.SHADOW_UPDATE_INTERVAL;
        return OptionImpl.createBuilder(int.class, STORAGE)
            .setId(new ResourceLocation("frmc", "oculus_shadow_update_interval"))
            .setName(Component.translatable("frmc.embeddium.options.shadow_update_interval.name"))
            .setTooltip(Component.translatable("frmc.embeddium.options.shadow_update_interval.tooltip"))
            .setControl(option -> new SliderControl(option, 1, 4, 1, frames -> frames <= 1
                ? Component.translatable("frmc.embeddium.options.shadow_update_interval.every_frame")
                : Component.translatable("frmc.embeddium.options.shadow_update_interval.every_n_frames", frames)))
            .setBinding((storage, frames) -> value.set(frames), storage -> value.get())
            .setImpact(OptionImpact.HIGH)
            .build();
    }

    @Nullable
    private static Option<?> uploadBudget() {
        if (!FRMCMixinLoadConfig.isIntegrationEnabled(MIXIN_PACKAGE + "perf.RenderSectionManagerUploadBudgetMixin")) {
            return null;
        }

        ForgeConfigSpec.IntValue value = EmbeddiumPortConfig.CHUNK_UPLOAD_BUDGET_KB;
        return OptionImpl.createBuilder(int.class, STORAGE)
            .setId(new ResourceLocation("frmc", "embeddium_chunk_upload_budget"))
            .setName(Component.translatable("frmc.embeddium.options.chunk_upload_budget.name"))
            .setTooltip(Component.translatable("frmc.embeddium.options.chunk_upload_budget.tooltip"))
            .setControl(option -> new SliderControl(option, 0, 4096, 256, kb -> kb <= 0
                ? Component.translatable("frmc.embeddium.options.chunk_upload_budget.unlimited")
                : Component.translatable("frmc.embeddium.options.chunk_upload_budget.kb", kb)))
            .setBinding((storage, kb) -> value.set(kb), storage -> value.get())
            .setImpact(OptionImpact.VARIES)
            .build();
    }

    @Nullable
    private static Option<?> toggle(String mixin, String key, ForgeConfigSpec.BooleanValue value, @Nullable OptionImpact impact) {
        if (!FRMCMixinLoadConfig.isIntegrationEnabled(MIXIN_PACKAGE + mixin)) {
            return null;
        }

        var builder = OptionImpl.createBuilder(boolean.class, STORAGE)
            .setId(new ResourceLocation("frmc", "embeddium_" + key))
            .setName(Component.translatable("frmc.embeddium.options." + key + ".name"))
            .setTooltip(Component.translatable("frmc.embeddium.options." + key + ".tooltip"))
            .setControl(TickBoxControl::new)
            .setBinding((storage, enabled) -> value.set(enabled), storage -> value.get());
        if (impact != null) {
            builder.setImpact(impact);
        }
        return builder.build();
    }
}
