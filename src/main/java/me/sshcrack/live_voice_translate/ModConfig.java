package me.sshcrack.live_voice_translate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ModConfig {

    private static ModConfig INSTANCE;

    private final String apiKey;
    private final String targetLanguage;
    private final boolean enabled;
    private final int maxWebSockets;
    private final float originalAudioVolume;

    private ModConfig(String apiKey, String targetLanguage, boolean enabled, int maxWebSockets, float originalAudioVolume) {
        this.apiKey = apiKey;
        this.targetLanguage = targetLanguage;
        this.enabled = enabled && !apiKey.isEmpty();
        this.maxWebSockets = Math.min(maxWebSockets, 3);
        this.originalAudioVolume = Math.max(0, Math.min(1, originalAudioVolume));
    }

    public static synchronized ModConfig load() {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        String apiKey = System.getenv("GEMINI_API_KEY");
        String targetLanguage = "en";
        boolean enabled = true;
        int maxWebSockets = 3;
        float originalAudioVolume = 0.15f;

        String configDir = System.getProperty("live_voice_translate.config_dir", "config");
        Path configPath = Paths.get(configDir, LiveVoiceTranslate.MOD_ID + ".properties");
        if (Files.exists(configPath)) {
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(configPath)) {
                props.load(in);
                if (apiKey == null || apiKey.isEmpty()) {
                    apiKey = props.getProperty("apiKey", "");
                }
                targetLanguage = props.getProperty("targetLanguage", "en");
                enabled = Boolean.parseBoolean(props.getProperty("enabled", "true"));
                    try {
                        maxWebSockets = Integer.parseInt(props.getProperty("maxWebSockets", "3"));
                    } catch (NumberFormatException e) {
                        LiveVoiceTranslate.LOGGER.warn("Invalid maxWebSockets value in config, using default of 3");
                        maxWebSockets = 3;
                    }
                    try {
                        originalAudioVolume = Float.parseFloat(props.getProperty("originalAudioVolume", "0.15"));
                    } catch (NumberFormatException e) {
                        LiveVoiceTranslate.LOGGER.warn("Invalid originalAudioVolume value in config, using default of 0.15");
                        originalAudioVolume = 0.15f;
                    }
            } catch (IOException e) {
                LiveVoiceTranslate.LOGGER.error("Failed to load config from {}", configPath, e);
            }
        } else {
            if (apiKey == null || apiKey.isEmpty()) {
                LiveVoiceTranslate.LOGGER.warn(
                    "No GEMINI_API_KEY env var found and no {} found. Translation will be disabled.",
                    configPath
                );
                apiKey = "";
            }
        }

        INSTANCE = new ModConfig(apiKey, targetLanguage, enabled, maxWebSockets, originalAudioVolume);
        return INSTANCE;
    }

    public static synchronized ModConfig get() {
        if (INSTANCE == null) {
            return load();
        }
        return INSTANCE;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxWebSockets() {
        return maxWebSockets;
    }

    public float getOriginalAudioVolume() {
        return originalAudioVolume;
    }
}
