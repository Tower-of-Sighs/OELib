package com.mafuyu404.oelib.neoforge.example;

import com.mafuyu404.oelib.OELib;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = OELib.MODID, value = Dist.CLIENT)
public final class FluidRenderExampleClient {
    private static KeyMapping openExample;

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        if (!FMLLoader.isProduction()) {
            openExample = new KeyMapping("key.oelib.open_fluid_example", GLFW.GLFW_KEY_G, "key.categories.oelib");
            event.register(openExample);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (openExample != null && openExample.consumeClick()) {
            Minecraft.getInstance().setScreen(new FluidRenderExampleScreen());
        }
    }
}