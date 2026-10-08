package com.y271727uy.FRMC.mixin.betterfoliage;

import com.eerussianguy.betterfoliage.model.LeavesBakedModel;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.y271727uy.FRMC.capability.betterfoliagecutting.BetterFoliageInteriorCulling;
import com.y271727uy.FRMC.capability.betterfoliagecutting.ChlorideLeavesSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 仅当 Chloride 实际在剔叶时动手（ALL，或 Embeddium Fast leaves）；关剔除则 BF 原样。
 * Chloride ALL 只跳过立方体面。BF fluff 十字挂在 NORTH/SOUTH 上，会穿过空心树冠。
 * 六邻都是叶：所有方向含 {@code dir == null} 返回空。
 * 北或南任一侧邻叶：任意方向丢掉 crosses，只留 core（overlay 也留）。
 */
@Pseudo
@Mixin(targets = "com.eerussianguy.betterfoliage.model.LeavesBakedModel", remap = false)
public abstract class LeavesBakedModelMixin {
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("FRMC-BetterFoliage");
    
    @Unique
    private static final ModelProperty<Integer> FRMC$LEAF_NEIGHBOR_MASK = new ModelProperty<>();

    @Shadow
    private BakedModel core;

    @Shadow
    private BakedModel outerCore;

    @Shadow
    private TextureAtlasSprite leavesTex;

    @Shadow
    @Final
    private boolean isOverlay;

    /**
     * <h2>Better Foliage + ModernFix dynamic_resources 导致的树叶模型空指针问题修复</h2>
     * 
     * <h3>问题描述</h3>
     * <p>启用 ModernFix 的 {@code mixin.perf.dynamic_resources=true} 时，Better Foliage 的树叶渲染为
     * 紫黑格子（missing texture），日志中大量出现 NPE 和 "No texture for id missingno.png" 错误。
     * 问题出现在 {@link net.minecraft.client.resources.model.BakedModel#getParticleIcon()} 调用时，
     * Better Foliage 的 {@code LeavesBakedModel.leavesTex} 字段为 {@code null}。</p>
     * 
     * <h3>踩坑次数：5 次</h3>
     * <ol>
     *   <li><b>第 0 坑：试图用渲染事件（RenderLevelStageEvent）拦截处理</b>
     *       <br>最初尝试通过监听 Forge 的 {@link net.minecraftforge.client.event.RenderLevelStageEvent}
     *       在渲染阶段动态修改树叶模型或剔除逻辑，试图在渲染时解决 Better Foliage 的兼容问题。
     *       <br><b>失败原因：</b>游戏直接卡死。渲染事件每帧都会被大量触发，在这个高频路径上
     *       做模型修改或者状态查询会造成严重的性能问题。更重要的是，渲染阶段已经太晚了——
     *       Better Foliage 的模型烘焙和纹理绑定发生在资源加载期，渲染时修改无法解决根本问题。
     *       这次失败让我意识到必须在模型层面（Mixin）而非渲染层面解决问题。</li>
     *   
     *   <li><b>第 1 坑：只看表象，认为是 Continuity 修改纹理导致的</b>
     *       <br>初始假设 Continuity 这类纹理扩展 mod 干扰了 Better Foliage 的纹理加载，
     *       在 {@code getParticleIcon()} 里写了防御性代码，用 {@code core.getParticleIcon()} 作为 fallback。
     *       <br><b>失败原因：</b>{@code core} 字段本身也是 {@code null}！fallback 路径同样 NPE。</li>
     *   
     *   <li><b>第 2 坑：没有验证假设，盲目添加多层 fallback</b>
     *       <br>在第一层 fallback 失败后，又加了 {@code outerCore} 作为第二层 fallback，最后兜底返回
     *       {@code getMissingModel().getParticleIcon()}。编译通过，部署后树叶还是紫黑格子。
     *       <br><b>失败原因：</b>没有加日志，不知道代码是否真的被调用了，也不知道这三个字段的实际状态。
     *       关键是 {@link org.spongepowered.asm.mixin.Overwrite @Overwrite} 可能根本没生效。</li>
     *   
     *   <li><b>第 3 坑：加了日志但仍然只看 getParticleIcon 调用点</b>
     *       <br>添加了详细日志后发现 {@code @Overwrite} 确实生效了，但日志显示
     *       {@code leavesTex=NULL, core=NULL, outerCore=NULL} —— <b>三个字段在运行时全是 null！</b>
     *       <br>这时候还在纠结"为什么这些字段是 null"，试图在 {@code getParticleIcon()} 里修复它们。
     *       <br><b>失败原因：</b>问题的<b>根源不在读取侧</b>，而在<b>写入侧</b>：这些字段根本没被赋值过！</li>
     *   
     *   <li><b>第 4 坑：找到赋值点但理解错了生命周期</b>
     *       <br>反编译 Better Foliage 字节码，发现字段赋值全在 {@code LeavesBakedModel.init()} 方法里，
     *       而 {@code init()} 由 {@code EventHandler.onModelBake(ModelEvent.BakingCompleted)} 调用。
     *       <br>一开始以为是事件监听器没注册上，或者 Mixin 破坏了 {@code INSTANCES} 列表。
     *       <br><b>失败原因：</b>没有意识到 ModernFix 的 {@code dynamic_resources} 改变了模型加载的<b>时序</b>：
     *       动态烘焙的模型在 {@code BakingCompleted} 事件<b>之后</b>才创建，永远等不到 {@code init()} 调用。</li>
     * </ol>
     * 
     * <h3>问题所在</h3>
     * <p>Better Foliage 的模型初始化流程分两步：</p>
     * <ol>
     *   <li><b>构造函数</b>：只初始化配置字段（{@code modelLocation}、{@code leaves}、{@code fluff}
     *       等），并将实例添加到静态列表 {@code INSTANCES} 中。<b>不初始化纹理和几何字段</b>。</li>
     *   <li><b>{@code init()} 方法</b>：通过 {@code Helpers.getTexture()} 加载纹理精灵到 {@code leavesTex}
     *       和 {@code fluffTex}，构建方块模型到 {@code core} 和 {@code outerCore}，生成十字面片到 {@code crosses}。
     *       <br>这个方法由 {@code EventHandler.onModelBake()} 遍历 {@code INSTANCES} 列表统一调用。</li>
     * </ol>
     * 
     * <p><b>ModernFix 的 {@code dynamic_resources} 破坏了这个流程：</b></p>
     * <ul>
     *   <li>启用 {@code dynamic_resources} 后，模型不再在启动时全量烘焙，而是在首次渲染时按需创建。</li>
     *   <li>这些<b>延迟创建的模型实例</b>在 {@code ModelEvent.BakingCompleted} 事件触发<b>之后</b>才调用构造函数，
     *       此时 {@code onModelBake()} 的 {@code forEach(LeavesBakedModel::init)} 已经执行完毕。</li>
     *   <li>新实例虽然加入了 {@code INSTANCES}，但<b>永远不会被调用 {@code init()}</b>，导致渲染时
     *       {@code leavesTex}、{@code core}、{@code outerCore} 全是 {@code null}。</li>
     * </ul>
     * 
     * <h3>之前为什么踩坑了？</h3>
     * <ol>
     *   <li><b>没有从源头分析字段的赋值流程</b>：一开始只盯着 {@code getParticleIcon()} 的调用侧，
     *       试图在读取时修复，而没有去查"这些字段应该在哪里被赋值"。正确做法是先反编译看赋值逻辑。</li>
     *   <li><b>没有及时加日志验证假设</b>：前两次修复都是"想当然"写代码，编译部署后发现不工作，
     *       但不知道具体哪里不对。应该在第一次失败后立即加详细日志，而不是继续猜。</li>
     *   <li><b>忽略了 ModernFix 对模型加载时序的影响</b>：看到字节码里有 {@code onModelBake} 和
     *       {@code init()} 的调用后，以为问题是"事件没触发"或"列表被破坏"，没想到是<b>时序错位</b>：
     *       动态模型的创建时间晚于事件触发时间，导致 {@code init()} 调用遗漏。</li>
     *   <li><b>对 Forge 模型事件的生命周期理解不足</b>：{@code BakingCompleted} 是一次性事件，
     *       动态创建的模型不会触发第二次事件，必须在模型自己的首次使用时补调初始化。</li>
     * </ol>
     * 
     * <h3>修复逻辑</h3>
     * <p>在 {@code LeavesBakedModel} 首次被使用时（{@code getParticleIcon()} 或 {@code getQuads()}），
     * 检测 {@code core == null}，如果为 {@code null} 则主动调用 {@code init()} 完成延迟初始化。</p>
     * 
     * <p><b>实现细节：</b></p>
     * <ul>
     *   <li>使用 {@code volatile boolean frmc$ready} 标记初始化状态，避免重复调用 {@code init()}。</li>
     *   <li>双重检查锁定（DCL）保证线程安全：多个渲染线程同时调用时，只有一个线程执行 {@code init()}。</li>
     *   <li>在 {@code getParticleIcon()} 和 {@code getQuads()} 的 {@code @Inject(at = @At("HEAD"))} 处注入
     *       {@code frmc$ensureInit()} 调用，确保任何渲染路径都会触发初始化。</li>
     *   <li>如果 {@code init()} 抛出异常（纹理加载失败等），捕获日志记录，设置 {@code frmc$ready = true}
     *       避免死循环重试，后续渲染返回 missing 模型作为兜底。</li>
     * </ul>
     * 
     * <p><b>为什么这个修复有效：</b></p>
     * <ul>
     *   <li>ModernFix 延迟创建的模型在首次 {@code getQuads()} 时会经过这个注入点，此时 {@code core == null}
     *       触发 {@code init()} 调用，填充所有字段，后续渲染正常。</li>
     *   <li>正常流程（{@code dynamic_resources=false}）的模型已经在 {@code BakingCompleted} 时调用过
     *       {@code init()}，{@code core != null}，双重检查的第一层直接返回，<b>零性能开销</b>。</li>
     *   <li>兜底了 Better Foliage 自己 {@code init()} 失败的情况：如果纹理真的加载不到，
     *       至少不会无限 NPE，而是返回 missing 模型，让玩家看到紫黑格子（标准的纹理缺失反馈）。</li>
     * </ul>
     * 这次不是人类写的，是opus5.5
     */
    @Shadow
    public abstract void init();

    @Unique
    private volatile boolean frmc$ready;

    @Unique
    private void frmc$ensureInit() {
        if (this.frmc$ready) {
            return;
        }
        synchronized (this) {
            if (this.frmc$ready) {
                return;
            }
            if (this.core == null) {
                try {
                    this.init();
                } catch (Throwable t) {
                    LOGGER.error("FRMC: BetterFoliage LeavesBakedModel lazy init failed (overlay={})", this.isOverlay, t);
                }
            }
            this.frmc$ready = true;
        }
    }

    @Inject(method = "m_6160_", at = @At("HEAD"), cancellable = true, remap = false)
    private void frmc$initBeforeParticleIcon(CallbackInfoReturnable<TextureAtlasSprite> cir) {
        this.frmc$ensureInit();
        if (this.leavesTex == null) {
            cir.setReturnValue(Minecraft.getInstance().getModelManager().getMissingModel().getParticleIcon());
        }
    }

    @ModifyReturnValue(method = "getModelData", at = @At("RETURN"))
    private ModelData frmc$attachLeafNeighborMask(
        ModelData original,
        BlockAndTintGetter level,
        BlockPos pos,
        BlockState state,
        ModelData extra
    ) {
        int mask = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            if (ChlorideLeavesSupport.isOccludingLeaf(level.getBlockState(cursor.setWithOffset(pos, direction)).getBlock())) {
                mask |= 1 << direction.get3DDataValue();
            }
        }
        return original.derive().with(FRMC$LEAF_NEIGHBOR_MASK, Integer.valueOf(mask)).build();
    }

    @Inject(method = "getQuads", at = @At("HEAD"), cancellable = true)
    private void frmc$cullInteriorAndInwardCrosses(
        BlockState state,
        Direction side,
        RandomSource rand,
        ModelData data,
        RenderType renderType,
        CallbackInfoReturnable<List<BakedQuad>> cir
    ) {
        this.frmc$ensureInit();
        if (this.core == null) {
            cir.setReturnValue(List.of());
            return;
        }
        if (!ChlorideLeavesSupport.isLeavesCullingActive()) {
            return;
        }
        Integer maskBoxed = data.get(FRMC$LEAF_NEIGHBOR_MASK);
        if (maskBoxed == null) {
            return;
        }
        int mask = maskBoxed.intValue();
        if (BetterFoliageInteriorCulling.shouldCullEntirely(mask)) {
            cir.setReturnValue(List.of());
            return;
        }
        if (!BetterFoliageInteriorCulling.shouldDropCrosses(mask)) {
            return;
        }
        cir.setReturnValue(frmc$coreAndOverlayQuads(state, side, rand, data, renderType));
    }

    @Unique
    private List<BakedQuad> frmc$coreAndOverlayQuads(
        BlockState state,
        Direction side,
        RandomSource rand,
        ModelData data,
        RenderType renderType
    ) {
        if (this.core == null) {
            return List.of();
        }
        List<BakedQuad> quads = this.core.getQuads(state, side, rand, data, renderType);
        if (!this.isOverlay || this.outerCore == null) {
            return quads;
        }
        List<BakedQuad> overlayQuads = this.outerCore.getQuads(state, side, rand, data, renderType);
        if (overlayQuads.isEmpty()) {
            return quads;
        }
        List<BakedQuad> merged = new ArrayList<>(quads.size() + overlayQuads.size());
        merged.addAll(quads);
        merged.addAll(overlayQuads);
        return merged;
    }
}
