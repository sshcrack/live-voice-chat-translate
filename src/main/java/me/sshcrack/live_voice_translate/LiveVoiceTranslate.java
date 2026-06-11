package me.sshcrack.live_voice_translate;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiveVoiceTranslate implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("live_voice_translate");
    public static final String VERSION = "0.1.0";
    public static final String MINECRAFT = "26.1.1";

    @Override
    public void onInitialize() {
        LOGGER.info("Live Voice Chat Translate initialized!");

        //? if !release
        LOGGER.warn("Running in development mode!");

        //? if fapi: <0.100
        /*LOGGER.info("Fabric API is old on this version");*/
    }

    public static Identifier id(String namespace, String path) {
        //? if <1.21 {
        /*return new Identifier(namespace, path);
        *///?} else
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
