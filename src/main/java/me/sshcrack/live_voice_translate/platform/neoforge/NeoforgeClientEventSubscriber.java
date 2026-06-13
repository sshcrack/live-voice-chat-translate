package me.sshcrack.live_voice_translate.platform.neoforge;

//? neoforge {

/*import com.mojang.blaze3d.platform.InputConstants;
import me.sshcrack.live_voice_translate.LiveVoiceTranslate;
import me.sshcrack.live_voice_translate.config.ConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = LiveVoiceTranslate.MOD_ID, value = Dist.CLIENT)
public class NeoforgeClientEventSubscriber {
	private static KeyMapping openConfigKey;

	@SubscribeEvent
	public static void onClientSetup(final FMLClientSetupEvent event) {
		LiveVoiceTranslate.onInitializeClient();
		registerKeybindings();
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		while (openConfigKey.consumeClick()) {
			Minecraft.getInstance().setScreen(
				new ConfigScreen(Minecraft.getInstance().screen)
			);
		}
	}

	private static void registerKeybindings() {
		openConfigKey = new KeyMapping(
			"key." + LiveVoiceTranslate.MOD_ID + ".open_config",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_F6,
			"key.categories." + LiveVoiceTranslate.MOD_ID
		);
	}
}
 *///?}