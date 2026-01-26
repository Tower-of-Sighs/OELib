package cc.sighs.oelib.fabric.example;

import cc.sighs.oelib.registry.extra.KeyMappingRegister;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class FluidRenderExampleClient {
    private static KeyMapping openExampleKey;

    public static void init() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            openExampleKey = new KeyMapping("key.oelib.open_fluid_example", GLFW.GLFW_KEY_G, "key.categories.oelib");
            KeyMappingRegister.register(openExampleKey);
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                while (openExampleKey.consumeClick()) {
                    Minecraft.getInstance().setScreen(new FluidRenderExampleScreen());
                }
            });
        }
    }
}