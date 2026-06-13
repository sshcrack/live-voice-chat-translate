package me.sshcrack.live_voice_translate.config;

import com.mojang.blaze3d.platform.InputConstants;
import me.sshcrack.live_voice_translate.LiveVoiceTranslate;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

//? if fabric {
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//?}

public class ModKeybindings {
    private static final String CATEGORY = "key.categories." + LiveVoiceTranslate.MOD_ID;
    private static KeyMapping openConfigKey;

    public static void register() {
        openConfigKey = new KeyMapping(
            "key." + LiveVoiceTranslate.MOD_ID + ".open_config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F6,
            CATEGORY
        );

        //? if fabric {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.consumeClick()) {
                Minecraft.getInstance().setScreen(
                    new ConfigScreen(client.screen)
                );
            }
        });
        //?}
    }

    //? if forge || neoforge {
    /*public static KeyMapping getOpenConfigKey() {
        return openConfigKey;
    }*/
    //?}
}
