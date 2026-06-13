package me.sshcrack.live_voice_translate.platform.forge;

//? forge {

/*import com.mojang.blaze3d.platform.InputConstants;
import me.sshcrack.live_voice_translate.LiveVoiceTranslate;
import me.sshcrack.live_voice_translate.config.ConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = LiveVoiceTranslate.MOD_ID, value = Dist.CLIENT)
public class ForgeClientEventSubscriber {

	private static KeyMapping openConfigKey;

	@SubscribeEvent
	public static void onClientSetup(final FMLClientSetupEvent event) {
		LiveVoiceTranslate.onInitializeClient();
		registerKeybindings();
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent event) {
		if (event.phase == ClientTickEvent.Phase.END) {
			while (openConfigKey.consumeClick()) {
				Minecraft.getInstance().setScreen(
					new ConfigScreen(Minecraft.getInstance().screen)
				);
			}
		}
	}

	private static void registerKeybindings() {
		openConfigKey = new KeyMapping(
			"key." + LiveVoiceTranslate.MOD_ID + ".open_config",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_F6,
			"key.categories." + LiveVoiceTranslate.MOD_ID
		);
		ClientRegistry.registerKeyBinding(openConfigKey);
	}
}
 *///?}