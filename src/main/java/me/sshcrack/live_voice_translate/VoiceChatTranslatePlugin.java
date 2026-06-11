package me.sshcrack.live_voice_translate;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;

//? if neoforge || forge {
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
//?}

//? if neoforge || forge {
@ForgeVoicechatPlugin
//?}
public class VoiceChatTranslatePlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return LiveVoiceTranslate.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
    }
}
