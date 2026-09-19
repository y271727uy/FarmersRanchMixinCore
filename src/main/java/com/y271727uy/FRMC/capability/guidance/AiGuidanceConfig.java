package com.y271727uy.FRMC.capability.guidance;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import com.y271727uy.FRMC.config.FRMCConfigPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Per-process AI guidance config. Client and dedicated server each read their own file.
 * Custom prompts never override {@link AiGuidancePolicy}.
 */
public final class AiGuidanceConfig {
    public static final String FILE_NAME = "frmc-ai-guidance.toml";
    public static final String DEFAULT_TEMPLATE = """
            # FRMC AI guidance.
            # Client and dedicated server each use their own copy of this file.
            # Custom prompts never override the built-in diagnosis policy.

            enabled = true
            lang = "zh_cn"

            # Single-language prompt. Use ''' for multiline text.
            prompt = '''
            '''

            # Optional per-language prompts. Matching lang wins over the top-level prompt.
            # [[prompts]]
            # lang = "zh_cn"
            # prompt = '''
            # 请把完整日志发到 QQ 群
            # '''
            #
            # [[prompts]]
            # lang = "en_us"
            # prompt = '''
            # Please send the full log to the Discord server.
            # '''
            """;
    private static final String DEFAULT_LANG = "zh_cn";
    private static final String FALLBACK_LANG = "en_us";

    public boolean enabled = true;
    public String lang = DEFAULT_LANG;
    public String prompt = "";
    public List<PromptEntry> prompts = new ArrayList<>();

    public static final class PromptEntry {
        public String lang = "";
        public String prompt = "";
    }

    private AiGuidanceConfig() {
    }

    public static AiGuidanceConfig defaults() {
        return new AiGuidanceConfig();
    }

    public static Path configPath() {
        return FRMCConfigPaths.resolve(FILE_NAME);
    }

    public static AiGuidanceConfig load() {
        Path path = configPath();
        try {
            FRMCConfigPaths.ensureDirectory();
            if (!Files.exists(path)) {
                Files.writeString(path, DEFAULT_TEMPLATE, StandardCharsets.UTF_8);
                return defaults();
            }
            return parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException exception) {
            return defaults();
        }
    }

    public static AiGuidanceConfig parse(String toml) {
        if (toml == null || toml.isBlank()) {
            return defaults();
        }
        try {
            Config parsed = new TomlParser().parse(toml);
            AiGuidanceConfig config = new AiGuidanceConfig();
            Object enabled = parsed.get("enabled");
            config.enabled = enabled instanceof Boolean value ? value : true;
            Object lang = parsed.get("lang");
            config.lang = lang == null ? DEFAULT_LANG : String.valueOf(lang);
            config.prompt = readString(parsed.get("prompt"));
            Object prompts = parsed.get("prompts");
            if (prompts instanceof List<?> list) {
                for (Object item : list) {
                    if (!(item instanceof Config entryConfig)) {
                        continue;
                    }
                    PromptEntry entry = new PromptEntry();
                    entry.lang = readString(entryConfig.get("lang"));
                    entry.prompt = readString(entryConfig.get("prompt"));
                    config.prompts.add(entry);
                }
            }
            config.normalize();
            return config;
        } catch (RuntimeException exception) {
            return defaults();
        }
    }

    public String selectedPrompt() {
        return selectedPrompt(lang);
    }

    public String selectedPrompt(String requestedLang) {
        String match = normalizeLang(requestedLang);
        String found = findPrompt(match);
        if (found != null) {
            return found;
        }
        String direct = trimToEmpty(prompt);
        if (!direct.isEmpty()) {
            return direct;
        }
        if (!FALLBACK_LANG.equals(match)) {
            found = findPrompt(FALLBACK_LANG);
            if (found != null) {
                return found;
            }
        }
        String first = firstNonBlankPrompt();
        return first == null ? "" : first;
    }

    private String findPrompt(String langCode) {
        if (prompts == null) {
            return null;
        }
        for (PromptEntry entry : prompts) {
            if (entry != null && langCode.equals(normalizeLang(entry.lang))) {
                String value = trimToEmpty(entry.prompt);
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    private String firstNonBlankPrompt() {
        if (prompts == null) {
            return null;
        }
        for (PromptEntry entry : prompts) {
            if (entry == null) {
                continue;
            }
            String value = trimToEmpty(entry.prompt);
            if (!value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private void normalize() {
        if (lang == null || lang.isBlank()) {
            lang = DEFAULT_LANG;
        } else {
            lang = normalizeLang(lang);
        }
        if (prompt == null) {
            prompt = "";
        }
        if (prompts == null) {
            prompts = new ArrayList<>();
        }
    }

    static String normalizeLang(String lang) {
        if (lang == null || lang.isBlank()) {
            return DEFAULT_LANG;
        }
        return lang.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String readString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
