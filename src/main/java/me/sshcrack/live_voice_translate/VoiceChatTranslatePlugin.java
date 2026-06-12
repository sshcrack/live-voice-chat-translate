package me.sshcrack.live_voice_translate;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;

import java.util.UUID;

//? if devtools {
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent;
import me.sshcrack.live_voice_translate.devtools.DevTranslateRunner;
//?}

//? if neoforge || forge {
/*import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
*///?}

//? if neoforge || forge {
/*@ForgeVoicechatPlugin
*///?}
public class VoiceChatTranslatePlugin implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return LiveVoiceTranslate.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientReceiveSoundEvent.StaticSound.class, this::handleIncomingSound);
        registration.registerEvent(ClientReceiveSoundEvent.LocationalSound.class, this::handleIncomingSound);
        //? if devtools {
        registration.registerEvent(ClientVoicechatConnectionEvent.class, event -> {
            if (event.isConnected()) {
                DevTranslateRunner.get().onVoicechatConnected(event.getVoicechat());
            } else {
                DevTranslateRunner.get().onVoicechatDisconnected();
            }
        });
        //?}
    }

    private void handleIncomingSound(ClientReceiveSoundEvent event) {
        if (!ModConfig.get().isEnabled()) return;

        TranslationManager tm = TranslationManager.get();
        if (tm == null) return;

        UUID senderId = event.getId();
        short[] rawAudio = event.getRawAudio();

        if (rawAudio.length == 0) {
            tm.onPlayerSilence(senderId);
            return;
        }

        tm.feedAudio(senderId, rawAudio);

        short[] translated = tm.getTranslatedAudio(senderId);
        if (translated != null) {
            event.setRawAudio(translated);
        }
    }
}
