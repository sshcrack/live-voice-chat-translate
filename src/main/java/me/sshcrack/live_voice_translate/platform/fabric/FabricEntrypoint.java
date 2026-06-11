package me.sshcrack.live_voice_translate.platform.fabric;

//? fabric {

import me.sshcrack.live_voice_translate.LiveVoiceTranslate;
import dev.kikugie.fletching_table.annotation.fabric.Entrypoint;
import net.fabricmc.api.ModInitializer;

@Entrypoint("main")
public class FabricEntrypoint implements ModInitializer {

	@Override
	public void onInitialize() {
		LiveVoiceTranslate.onInitialize();
	}
}
//?}
