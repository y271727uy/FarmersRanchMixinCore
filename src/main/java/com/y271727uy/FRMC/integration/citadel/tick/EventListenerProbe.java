package com.y271727uy.FRMC.integration.citadel.tick;

import java.lang.reflect.Field;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.ListenerList;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventListenerHelper;

/**
 * 查询 Forge 事件总线上某个事件类当前是否有监听器（含父类监听器）。
 * 任何一步反射失败都按"有监听器"处理，保证不会误跳过事件。
 */
public final class EventListenerProbe {
    private static final int BUS_ID;

    static {
        int busId = -1;
        try {
            Field field = MinecraftForge.EVENT_BUS.getClass().getDeclaredField("busID");
            field.setAccessible(true);
            busId = field.getInt(MinecraftForge.EVENT_BUS);
        } catch (Throwable ignored) {
            // 保持 -1：不可用
        }
        BUS_ID = busId;
    }

    private final ListenerList list;

    private EventListenerProbe(ListenerList list) {
        this.list = list;
    }

    public static EventListenerProbe of(Class<? extends Event> eventClass) {
        if (BUS_ID < 0) {
            return new EventListenerProbe(null);
        }
        try {
            return new EventListenerProbe(EventListenerHelper.getListenerList(eventClass));
        } catch (Throwable ignored) {
            return new EventListenerProbe(null);
        }
    }

    public boolean hasListeners() {
        if (list == null) {
            return true;
        }
        try {
            return list.getListeners(BUS_ID).length > 0;
        } catch (Throwable ignored) {
            return true;
        }
    }
}
