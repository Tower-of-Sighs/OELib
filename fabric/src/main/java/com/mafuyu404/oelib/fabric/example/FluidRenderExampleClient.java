package com.mafuyu404.oelib.fabric.example;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class FluidRenderExampleClient {
    private static KeyMapping openExampleKey;

    public static void init() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            openExampleKey = KeyBindingHelper.registerKeyBinding(
                    new KeyMapping("key.oelib.open_fluid_example", GLFW.GLFW_KEY_G, "key.categories.oelib")
            );
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                while (openExampleKey.consumeClick()) {
                    Minecraft.getInstance().setScreen(new FluidRenderExampleScreen());
                }
            });
        }
    }
}