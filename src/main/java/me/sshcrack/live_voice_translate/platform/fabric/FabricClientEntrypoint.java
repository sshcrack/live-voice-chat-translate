package me.sshcrack.live_voice_translate.platform.fabric;

//? fabric {

import me.sshcrack.live_voice_translate.LiveVoiceTranslate;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ClientModInitializer;

@Entrypoint("client")
public class FabricClientEntrypoint implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		LiveVoiceTranslate.onInitializeClient();
	}

}
//?}
