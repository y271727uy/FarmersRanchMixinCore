package com.y271727uy.FRMC.mixin.architectury;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Architectury 每次把事件交给一个监听器，都会现造一个 MethodHandle
 * （{@code lookup().unreflect(method).bindTo(listener).invokeWithArguments(args)}）。
 * 这里改为直接反射调用同一个接口方法，监听器、参数、调用顺序和返回值都不变。
 */
@Pseudo
@Mixin(targets = "dev.architectury.event.EventFactory", remap = false)
public abstract class EventFactoryDispatchMixin {
    @Inject(method = "invokeMethod(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private static void frmc$invokeListenerDirectly(Object listener, Method method, Object[] args,
                                                    CallbackInfoReturnable<Object> callback) throws Throwable {
        Object result;
        try {
            result = method.invoke(listener, args);
        } catch (InvocationTargetException exception) {
            // 原实现直接把监听器抛出的异常传出去，这里拆掉反射包的那一层保持一致。
            Throwable cause = exception.getCause();
            throw cause != null ? cause : exception;
        } catch (IllegalAccessException | IllegalArgumentException exception) {
            // 反射调不了的情况交还给原来的 MethodHandle 路径。
            return;
        }
        callback.setReturnValue(result);
    }
}
