package me.sshcrack.live_voice_translate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ModConfig {

    private static ModConfig INSTANCE;

    private String apiKey;
    private String targetLanguage;
    private boolean enabled;
    private int maxWebSockets;
    private boolean loadedFromEnv;

    private ModConfig(String apiKey, String targetLanguage, boolean enabled, int maxWebSockets, boolean loadedFromEnv) {
        this.apiKey = apiKey;
        this.targetLanguage = targetLanguage;
        this.enabled = enabled && !apiKey.isEmpty();
        this.maxWebSockets = Math.min(maxWebSockets, 3);
        this.loadedFromEnv = loadedFromEnv;
    }

    public static synchronized ModConfig load() {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        String envApiKey = System.getenv("GEMINI_API_KEY");
        boolean loadedFromEnv = envApiKey != null && !envApiKey.isEmpty();
        String apiKey = envApiKey;
        String targetLanguage = "en";
        boolean enabled = true;
        int maxWebSockets = 3;

        Path configPath = getConfigPath();
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

        INSTANCE = new ModConfig(apiKey, targetLanguage, enabled, maxWebSockets, loadedFromEnv);
        return INSTANCE;
    }

    public static synchronized ModConfig get() {
        if (INSTANCE == null) {
            return load();
        }
        return INSTANCE;
    }

    public static synchronized void reload() {
        INSTANCE = null;
        load();
    }

    private static Path getConfigPath() {
        String configDir = System.getProperty("live_voice_translate.config_dir", "config");
        return Paths.get(configDir, LiveVoiceTranslate.MOD_ID + ".properties");
    }

    public void save() {
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
            Properties props = new Properties();
            props.setProperty("apiKey", apiKey);
            props.setProperty("targetLanguage", targetLanguage);
            props.setProperty("enabled", String.valueOf(enabled));
            props.setProperty("maxWebSockets", String.valueOf(maxWebSockets));
            try (OutputStream out = Files.newOutputStream(configPath)) {
                props.store(out, "Live Voice Chat Translate Config");
            }
        } catch (IOException e) {
            LiveVoiceTranslate.LOGGER.error("Failed to save config to {}", configPath, e);
        }
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setMaxWebSockets(int maxWebSockets) {
        this.maxWebSockets = Math.min(maxWebSockets, 3);
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

    public boolean isLoadedFromEnv() {
        return loadedFromEnv;
    }
}
