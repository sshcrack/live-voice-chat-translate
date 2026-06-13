package me.sshcrack.live_voice_translate;

//? < 1.17 {
/*import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;*/
//?} else {
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class LiveVoiceTranslate {

	public static final String MOD_ID = /*$ mod_id*/ "live_voice_translate";
	public static final String MOD_VERSION = /*$ mod_version*/ "0.1.0";
	public static final String MOD_FRIENDLY_NAME = /*$ mod_name*/ "Live Voice Chat Translate";

	//? < 1.17 {
	/*public static final Logger LOGGER = LogManager.getLogger(MOD_ID);*/
	//?} else {
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	//?}

	public static void onInitialize() {
		LOGGER.info("Initializing {}", MOD_ID);
	}

	public static void onInitializeClient() {
		LOGGER.info("Initializing {} Client", MOD_ID);
		//? if devtools {
		/*LOGGER.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		LOGGER.warn("!!!               DEVTOOLS MODE ENABLED                !!!");
		LOGGER.warn("!!!    Test audio files will play around you in-game    !!!");
		LOGGER.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		me.sshcrack.live_voice_translate.devtools.DevAutoQuit.init();
		*///?}
		ModConfig config = ModConfig.load();
		if (config.isEnabled()) {
			TranslationManager.initialize(config.getApiKey(), config.getTargetLanguage(), config.getMaxWebSockets());
			LOGGER.info("Translation enabled, target language: {}", config.getTargetLanguage());
		} else {
			LOGGER.warn("Translation disabled (no valid API key configured)");
		}
	}

	public static void openConfigScreen(Screen parent) {
		Minecraft.getInstance().setScreen(new me.sshcrack.live_voice_translate.config.ConfigScreen(parent));
	}
}
