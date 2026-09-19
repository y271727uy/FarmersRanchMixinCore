package com.y271727uy.FRMC.integration.xaero;

import java.util.Objects;
import java.util.function.Supplier;

/** Small, Minecraft-free lifecycle state machine for an optional Xaero integration. */
public final class XaeroIntegrationState {
    private boolean available;
    private boolean enabled;
    private boolean failed;

    public void initialize(boolean available) {
        this.available = available;
        this.enabled = available;
        this.failed = false;
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isEnabled() {
        return enabled && !failed;
    }

    public boolean hasFailed() {
        return failed;
    }

    public void disable() {
        enabled = false;
    }

    public void runSafely(Runnable lifecycleAction) {
        if (!isEnabled()) {
            return;
        }
        try {
            Objects.requireNonNull(lifecycleAction).run();
        } catch (Throwable ignored) {
            failed = true;
            enabled = false;
        }
    }

    public <T> T callSafely(Supplier<T> lifecycleAction, T fallback) {
        if (!isEnabled()) {
            return fallback;
        }
        try {
            return Objects.requireNonNull(lifecycleAction).get();
        } catch (Throwable ignored) {
            failed = true;
            enabled = false;
            return fallback;
        }
    }
}
