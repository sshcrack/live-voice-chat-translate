package me.sshcrack.live_voice_translate;

//? < 1.17 {
/*import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;*/
//?} else {
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//?}

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
	}
}
