//? if devtools {
/*package me.sshcrack.live_voice_translate.devtools;

import me.sshcrack.live_voice_translate.LiveVoiceTranslate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DevAutoQuit {
    private static boolean quitting = false;

    public static void init() {
        if (!"true".equals(System.getProperty("live_voice_translate.autoQuit"))) return;
        LiveVoiceTranslate.LOGGER.warn("[DevAutoQuit] Enabled — will quit on title screen");
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DevAutoQuit");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(DevAutoQuit::check, 0, 1, TimeUnit.SECONDS);
    }

    private static void check() {
        if (quitting) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TitleScreen) {
            quitting = true;
            LiveVoiceTranslate.LOGGER.info("[DevAutoQuit] Title screen reached, shutting down");
            mc.execute(mc::stop);
        }
    }
}

*///?}
