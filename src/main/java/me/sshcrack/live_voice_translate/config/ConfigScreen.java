package me.sshcrack.live_voice_translate.config;

import me.sshcrack.live_voice_translate.LiveVoiceTranslate;
import me.sshcrack.live_voice_translate.ModConfig;
import me.sshcrack.live_voice_translate.TranslationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private static final Component TITLE = Component.literal("Live Voice Chat Translate Settings");

    private final Screen parent;

    private Button enableToggle;
    private EditBox apiKeyField;
    private Button apiKeyToggleBtn;
    private EditBox languageField;
    private Button socketDecBtn;
    private Button socketIncBtn;

    private boolean enabled;
    private String apiKeyValue;
    private boolean apiKeyVisible;
    private String targetLanguage;
    private int maxSockets;
    private boolean programmaticUpdate;

    public ConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;

        ModConfig config = ModConfig.get();
        this.enabled = config.isEnabled();
        this.apiKeyValue = config.getApiKey();
        this.apiKeyVisible = false;
        this.targetLanguage = config.getTargetLanguage();
        this.maxSockets = config.getMaxWebSockets();
    }

    @Override
    protected void init() {
        super.init();

        int cx = width / 2;
        int y = 40;

        enableToggle = Button.builder(
            Component.literal("Enabled: " + (enabled ? "ON" : "OFF")),
            btn -> {
                enabled = !enabled;
                btn.setMessage(Component.literal("Enabled: " + (enabled ? "ON" : "OFF")));
            }
        ).bounds(cx - 100, y, 200, 20).build();
        addRenderableWidget(enableToggle);
        y += 28;

        apiKeyField = new EditBox(font, cx - 100, y, 170, 20, Component.literal("API Key"));
        apiKeyField.setMaxLength(512);
        apiKeyField.setResponder(this::onApiKeyChanged);
        addRenderableWidget(apiKeyField);

        apiKeyToggleBtn = Button.builder(
            Component.literal(apiKeyVisible ? "Hide" : "Show"),
            btn -> {
                apiKeyVisible = !apiKeyVisible;
                btn.setMessage(Component.literal(apiKeyVisible ? "Hide" : "Show"));
                updateApiKeyDisplay();
            }
        ).bounds(cx + 75, y, 25, 20).build();
        addRenderableWidget(apiKeyToggleBtn);
        updateApiKeyDisplay();
        y += 28;

        languageField = new EditBox(font, cx - 100, y, 200, 20, Component.literal("Target Language"));
        languageField.setMaxLength(10);
        languageField.setValue(targetLanguage);
        languageField.setResponder(text -> targetLanguage = text);
        addRenderableWidget(languageField);
        y += 28;

        socketDecBtn = Button.builder(
            Component.literal("-"),
            btn -> {
                maxSockets = Math.max(1, maxSockets - 1);
                updateSocketButtons();
            }
        ).bounds(cx - 30, y, 20, 20).build();
        addRenderableWidget(socketDecBtn);

        socketIncBtn = Button.builder(
            Component.literal("+"),
            btn -> {
                maxSockets = Math.min(3, maxSockets + 1);
                updateSocketButtons();
            }
        ).bounds(cx + 10, y, 20, 20).build();
        addRenderableWidget(socketIncBtn);
        updateSocketButtons();
        y += 30;

        Button saveBtn = Button.builder(
            Component.literal("Save"),
            btn -> onSave()
        ).bounds(cx - 105, y, 100, 20).build();
        addRenderableWidget(saveBtn);

        Button cancelBtn = Button.builder(
            Component.literal("Cancel"),
            btn -> onClose()
        ).bounds(cx + 5, y, 100, 20).build();
        addRenderableWidget(cancelBtn);
    }

    private void updateApiKeyDisplay() {
        programmaticUpdate = true;
        if (apiKeyVisible) {
            apiKeyField.setValue(apiKeyValue);
        } else {
            apiKeyField.setValue("*".repeat(apiKeyValue.length()));
        }
        programmaticUpdate = false;
    }

    private void onApiKeyChanged(String text) {
        if (!programmaticUpdate) {
            apiKeyValue = text;
        }
    }

    private void updateSocketButtons() {
        socketDecBtn.active = maxSockets > 1;
        socketIncBtn.active = maxSockets < 3;
    }

    private void onSave() {
        ModConfig config = ModConfig.get();
        config.setEnabled(enabled);
        config.setApiKey(apiKeyValue);
        config.setTargetLanguage(languageField.getValue());
        config.setMaxWebSockets(maxSockets);
        config.save();

        TranslationManager tm = TranslationManager.get();
        if (tm != null) {
            tm.hotReload();
        }

        onClose();
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int cx = width / 2;
        int y = 40;

        guiGraphics.drawCenteredString(font, TITLE, cx, 15, 0xFFFFFF);

        y += 28;
        guiGraphics.drawString(font, Component.literal("API Key:"), cx - 100, y - 12, 0xA0A0A0, false);

        y += 28;
        guiGraphics.drawString(font, Component.literal("Target Language:"), cx - 100, y - 12, 0xA0A0A0, false);
        guiGraphics.drawString(font,
            Component.literal("Common: en, es, fr, de, ja, zh..."),
            cx - 95, y + 2, 0x606060, false);

        y += 28;
        guiGraphics.drawString(font, Component.literal("Max Connections:"), cx - 100, y - 12, 0xA0A0A0, false);
        guiGraphics.drawCenteredString(font, Component.literal(String.valueOf(maxSockets)), cx, y + 10, 0xFFFFFF);

        if (ModConfig.get().isLoadedFromEnv()) {
            guiGraphics.drawString(font,
                Component.literal("API key loaded from GEMINI_API_KEY env var"),
                cx - 95, height - 40, 0xFFAA00, false);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
