package com.y271727uy.FRMC.capability.guidance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Writes the fixed policy, then the configured prompt, once at process start.
 * Client and dedicated server each inject from their own config file.
 */
public final class AiGuidanceLogInjector {
    private static final AtomicBoolean INJECTED = new AtomicBoolean();

    private AiGuidanceLogInjector() {
    }

    public static void injectOnce() {
        if (!INJECTED.compareAndSet(false, true)) {
            return;
        }
        try {
            AiGuidanceConfig config = AiGuidanceConfig.load();
            if (!config.enabled) {
                return;
            }
            logger().info("\n{}", render(config));
        } catch (Throwable exception) {
            try {
                logger().warn("Failed to inject AI guidance header", exception);
            } catch (Throwable ignored) {
                // Tests and very early boot may not have Log4j.
            }
        }
    }

    public static String render(AiGuidanceConfig config) {
        StringBuilder header = new StringBuilder();
        header.append("----- FRMC AI DIAGNOSIS POLICY -----\n");
        header.append(AiGuidancePolicy.TEXT.trim());
        header.append('\n');
        String prompt = config == null ? "" : config.selectedPrompt();
        if (!prompt.isEmpty()) {
            header.append('\n');
            header.append("----- FRMC AI GUIDANCE -----\n");
            header.append(prompt);
            header.append('\n');
        }
        header.append("----- END FRMC AI DIAGNOSIS POLICY -----");
        return header.toString();
    }

    private static Logger logger() {
        return LogManager.getLogger("FRMC-AI-Guidance");
    }
}
