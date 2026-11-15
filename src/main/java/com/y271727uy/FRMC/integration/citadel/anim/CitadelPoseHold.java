package com.y271727uy.FRMC.integration.citadel.anim;

import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.github.alexthe666.citadel.client.model.ModelAnimator;
import com.github.alexthe666.citadel.client.model.basic.BasicModelPart;
import com.mojang.blaze3d.systems.RenderSystem;
import com.y271727uy.FRMC.config.Config;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import org.joml.Matrix4f;

/**
 * 远处的 Citadel 实体在同一个游戏 tick 内只计算一次动画姿态，其余帧直接写回该实体自己的上一份姿态。
 *
 * <p>Citadel 的模型按渲染器共享，所以姿态快照按实体保存；只复用同一实体、同一模型、同一 tick 的快照，
 * 不会借用别的实体的姿态。任何无法证明安全的模型（带每帧状态字段、含非 {@link AdvancedModelBox} 部件）
 * 一律不参与，仍然每帧执行原版 {@code setupAnim}。</p>
 */
public final class CitadelPoseHold {
    /** 每个部件保存：旋转 3、位置 3、缩放 3、偏移 3。 */
    static final int FLOATS_PER_PART = 12;
    private static final int INVALID_TICK = Integer.MIN_VALUE;

    private static final BasicModelPart[] UNSUPPORTED = new BasicModelPart[0];
    private static final Map<Entity, Snapshot> SNAPSHOTS = new WeakHashMap<>();
    private static final Map<EntityModel<?>, BasicModelPart[]> PARTS = new WeakHashMap<>();
    private static final ClassValue<Boolean> STATELESS = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            return isStateless(type);
        }
    };

    private CitadelPoseHold() {
    }

    /**
     * 尝试用本 tick 已保存的姿态代替 {@code setupAnim}。
     *
     * @return 非 null 表示调用方应执行原版 {@code setupAnim} 后调用 {@link Ticket#capture()}；
     *         {@link Ticket#HELD} 表示姿态已写回，应跳过 {@code setupAnim}；null 表示不参与。
     */
    public static Ticket begin(EntityModel<?> model, LivingEntity entity) {
        int limit = Config.citadelPoseHoldPixels;
        if (limit <= 0 || !(model instanceof AdvancedEntityModel<?> advanced)
                || !RenderSystem.isOnRenderThread() || !eligible(entity) || !small(entity, limit)) {
            return null;
        }
        BasicModelPart[] parts = partsOf(advanced);
        if (parts == UNSUPPORTED) {
            return null;
        }
        Snapshot snapshot = SNAPSHOTS.get(entity);
        if (snapshot == null) {
            snapshot = new Snapshot();
            SNAPSHOTS.put(entity, snapshot);
        } else if (snapshot.model == model && snapshot.parts == parts && snapshot.tick == entity.tickCount) {
            snapshot.restore();
            return Ticket.HELD;
        }
        // 在 capture 之前快照保持无效，setupAnim 中途抛异常也不会留下半份姿态。
        snapshot.tick = INVALID_TICK;
        snapshot.model = model;
        snapshot.parts = parts;
        snapshot.pendingTick = entity.tickCount;
        return snapshot;
    }

    public static void clear() {
        SNAPSHOTS.clear();
        PARTS.clear();
    }

    private static boolean eligible(LivingEntity entity) {
        return !(entity instanceof Player)
                && entity.hurtTime == 0
                && entity.deathTime == 0
                && !entity.isVehicle()
                && !entity.isPassenger()
                && entity != Minecraft.getInstance().getCameraEntity()
                && !(entity instanceof IAnimatedEntity animated && animated.getAnimation() != IAnimatedEntity.NO_ANIMATION)
                && !entity.getType().is(Tags.EntityTypes.BOSSES);
    }

    private static boolean small(Entity entity, int limitPixels) {
        Minecraft minecraft = Minecraft.getInstance();
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        // 正交投影（GUI 内的实体预览、光影的阴影阶段）不参与。
        if (projection.m33() != 0.0F) {
            return false;
        }
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        double dx = entity.getX() - camera.x;
        double dy = entity.getY() - camera.y;
        double dz = entity.getZ() - camera.z;
        AABB box = entity.getBoundingBoxForCulling();
        double extent = Math.max(box.getYsize(), Math.max(box.getXsize(), box.getZsize()));
        return isSmall(extent, projection.m11(), minecraft.getWindow().getHeight(),
                dx * dx + dy * dy + dz * dz, limitPixels);
    }

    /** 实体在屏幕上的高度（像素）是否低于阈值；跟随视野、缩放和窗口高度变化。 */
    static boolean isSmall(double extent, float projectionScaleY, int windowHeight,
                           double distanceSquared, int limitPixels) {
        double pixelsAtUnitDistance = extent * projectionScaleY * windowHeight * 0.5;
        return pixelsAtUnitDistance > 0.0
                && pixelsAtUnitDistance * pixelsAtUnitDistance < (double) limitPixels * limitPixels * distanceSquared;
    }

    private static BasicModelPart[] partsOf(AdvancedEntityModel<?> model) {
        BasicModelPart[] parts = PARTS.get(model);
        if (parts == null) {
            parts = collectParts(model);
            PARTS.put(model, parts);
        }
        return parts;
    }

    private static BasicModelPart[] collectParts(AdvancedEntityModel<?> model) {
        try {
            if (!STATELESS.get(model.getClass())) {
                return UNSUPPORTED;
            }
            Set<BasicModelPart> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            List<BasicModelPart> ordered = new ArrayList<>();
            // getAllParts 和 parts 都可能漏列部件，所以沿子节点把整棵树走全。
            for (BasicModelPart part : model.getAllParts()) {
                if (!collect(part, seen, ordered)) {
                    return UNSUPPORTED;
                }
            }
            for (BasicModelPart part : model.parts()) {
                if (!collect(part, seen, ordered)) {
                    return UNSUPPORTED;
                }
            }
            return ordered.isEmpty() ? UNSUPPORTED : ordered.toArray(new BasicModelPart[0]);
        } catch (RuntimeException | LinkageError ignored) {
            return UNSUPPORTED;
        }
    }

    private static boolean collect(BasicModelPart part, Set<BasicModelPart> seen, List<BasicModelPart> ordered) {
        if (part == null) {
            return true;
        }
        // 子类可能带额外的每帧状态，无法保存，整模型放弃。
        if (part.getClass() != AdvancedModelBox.class) {
            return false;
        }
        if (!seen.add(part)) {
            return true;
        }
        ordered.add(part);
        for (BasicModelPart child : ((AdvancedModelBox) part).childModels) {
            if (!collect(child, seen, ordered)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 模型类自己声明的可变实例字段只允许是部件、部件数组或 {@link ModelAnimator}。
     * 其他可变字段通常是 {@code setupAnim} 写给 {@code renderToBuffer} 的每帧状态，跳过 {@code setupAnim} 会读到别的实体留下的值。
     */
    static boolean isStateless(Class<?> type) {
        try {
            for (Class<?> current = type; current != null && current != AdvancedEntityModel.class
                    && current != Object.class; current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                        continue;
                    }
                    Class<?> fieldType = field.getType();
                    Class<?> element = fieldType.isArray() ? fieldType.getComponentType() : fieldType;
                    if (!BasicModelPart.class.isAssignableFrom(element) && element != ModelAnimator.class) {
                        return false;
                    }
                }
            }
            return true;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    /** {@link #begin} 的结果：要么姿态已写回，要么等待原版动画跑完后保存。 */
    public abstract static class Ticket {
        static final Ticket HELD = new Ticket() {
            @Override
            public void capture() {
            }
        };

        public final boolean held() {
            return this == HELD;
        }

        public abstract void capture();
    }

    private static final class Snapshot extends Ticket {
        private EntityModel<?> model;
        private BasicModelPart[] parts;
        private int tick = INVALID_TICK;
        private int pendingTick;
        private float[] pose = new float[0];
        private boolean[] shown = new boolean[0];

        @Override
        public void capture() {
            BasicModelPart[] parts = this.parts;
            if (pose.length != parts.length * FLOATS_PER_PART) {
                pose = new float[parts.length * FLOATS_PER_PART];
                shown = new boolean[parts.length];
            }
            float[] pose = this.pose;
            for (int i = 0, offset = 0; i < parts.length; i++, offset += FLOATS_PER_PART) {
                AdvancedModelBox part = (AdvancedModelBox) parts[i];
                pose[offset] = part.rotateAngleX;
                pose[offset + 1] = part.rotateAngleY;
                pose[offset + 2] = part.rotateAngleZ;
                pose[offset + 3] = part.rotationPointX;
                pose[offset + 4] = part.rotationPointY;
                pose[offset + 5] = part.rotationPointZ;
                pose[offset + 6] = part.scaleX;
                pose[offset + 7] = part.scaleY;
                pose[offset + 8] = part.scaleZ;
                pose[offset + 9] = part.offsetX;
                pose[offset + 10] = part.offsetY;
                pose[offset + 11] = part.offsetZ;
                shown[i] = part.showModel;
            }
            tick = pendingTick;
        }

        private void restore() {
            BasicModelPart[] parts = this.parts;
            float[] pose = this.pose;
            for (int i = 0, offset = 0; i < parts.length; i++, offset += FLOATS_PER_PART) {
                AdvancedModelBox part = (AdvancedModelBox) parts[i];
                part.rotateAngleX = pose[offset];
                part.rotateAngleY = pose[offset + 1];
                part.rotateAngleZ = pose[offset + 2];
                part.rotationPointX = pose[offset + 3];
                part.rotationPointY = pose[offset + 4];
                part.rotationPointZ = pose[offset + 5];
                part.scaleX = pose[offset + 6];
                part.scaleY = pose[offset + 7];
                part.scaleZ = pose[offset + 8];
                part.offsetX = pose[offset + 9];
                part.offsetY = pose[offset + 10];
                part.offsetZ = pose[offset + 11];
                part.showModel = shown[i];
            }
        }
    }
}
