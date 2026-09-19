package com.y271727uy.FRMC.guidance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.y271727uy.FRMC.capability.guidance.AiGuidanceConfig;
import org.junit.jupiter.api.Test;

class AiGuidanceConfigTest {
    @Test
    void parseReadsSimpleLangAndPrompt() {
        AiGuidanceConfig config = AiGuidanceConfig.parse("""
                enabled = true
                lang = "zh_cn"
                prompt = "请把日志发到 QQ 群"
                """);

        assertTrue(config.enabled);
        assertEquals("zh_cn", config.lang);
        assertEquals("请把日志发到 QQ 群", config.selectedPrompt());
    }

    @Test
    void parseReadsMultilinePrompt() {
        AiGuidanceConfig config = AiGuidanceConfig.parse("""
                lang = "zh_cn"
                prompt = '''
                请把完整日志发到 QQ 群
                不要只截最后几行
                '''
                """);

        assertEquals("请把完整日志发到 QQ 群\n不要只截最后几行", config.selectedPrompt());
    }

    @Test
    void prefersMatchingLanguageEntryOverTopLevelPrompt() {
        AiGuidanceConfig config = AiGuidanceConfig.parse("""
                lang = "en-US"
                prompt = "top-level"

                [[prompts]]
                lang = "zh_cn"
                prompt = "中文"

                [[prompts]]
                lang = "en_us"
                prompt = "english"
                """);

        assertEquals("en_us", config.lang);
        assertEquals("english", config.selectedPrompt());
        assertEquals("中文", config.selectedPrompt("zh_cn"));
    }

    @Test
    void fallsBackToEnglishThenFirstNonBlankPrompt() {
        AiGuidanceConfig config = AiGuidanceConfig.parse("""
                lang = "ja_jp"
                prompt = ""

                [[prompts]]
                lang = "en_us"
                prompt = "english"

                [[prompts]]
                lang = "zh_cn"
                prompt = "中文"
                """);

        assertEquals("english", config.selectedPrompt());
    }

    @Test
    void invalidTomlUsesDefaultsWithoutCustomPrompt() {
        AiGuidanceConfig config = AiGuidanceConfig.parse("enabled = [");

        assertTrue(config.enabled);
        assertEquals("zh_cn", config.lang);
        assertEquals("", config.selectedPrompt());
    }

    @Test
    void canDisableInjection() {
        AiGuidanceConfig config = AiGuidanceConfig.parse("""
                enabled = false
                prompt = "ignored"
                """);

        assertFalse(config.enabled);
        assertEquals("ignored", config.selectedPrompt());
    }

    @Test
    void defaultTemplateParsesWithEmptyPrompt() {
        AiGuidanceConfig config = AiGuidanceConfig.parse(AiGuidanceConfig.DEFAULT_TEMPLATE);

        assertTrue(config.enabled);
        assertEquals("zh_cn", config.lang);
        assertEquals("", config.selectedPrompt());
    }
}
