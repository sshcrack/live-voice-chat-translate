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

    private ModConfig(String apiKey, String targetLanguage, boolean enabled, int maxWebSockets) {
        this.apiKey = apiKey;
        this.targetLanguage = targetLanguage;
        this.enabled = enabled && !apiKey.isEmpty();
        this.maxWebSockets = Math.min(maxWebSockets, 3);
    }

    public static ModConfig load() {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        String apiKey = System.getenv("GEMINI_API_KEY");
        String targetLanguage = "en";
        boolean enabled = true;
        int maxWebSockets = 3;

        Path configPath = Paths.get("config", "live_voice_translate.properties");
        if (Files.exists(configPath)) {
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(configPath)) {
                props.load(in);
                if (apiKey == null || apiKey.isEmpty()) {
                    apiKey = props.getProperty("apiKey", "");
                }
                targetLanguage = props.getProperty("targetLanguage", "en");
                enabled = Boolean.parseBoolean(props.getProperty("enabled", "true"));
                maxWebSockets = Integer.parseInt(props.getProperty("maxWebSockets", "3"));
            } catch (IOException e) {
                LiveVoiceTranslate.LOGGER.error("Failed to load config from {}", configPath, e);
            }
        } else {
            if (apiKey == null || apiKey.isEmpty()) {
                LiveVoiceTranslate.LOGGER.warn(
                    "No GEMINI_API_KEY env var found and no config/{}.properties found. Translation will be disabled.",
                    LiveVoiceTranslate.MOD_ID
                );
                apiKey = "";
            }
        }

        INSTANCE = new ModConfig(apiKey, targetLanguage, enabled, maxWebSockets);
        return INSTANCE;
    }

    public static ModConfig get() {
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
}
