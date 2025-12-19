package com.sighs.oelib.neoforge.example;

import com.sighs.oelib.OELib;
import com.sighs.oelib.registry.extra.KeyMappingRegister;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = OELib.MODID, value = Dist.CLIENT)
public final class FluidRenderExampleClient {
    private static KeyMapping openExample;

    public static void onRegisterKeys() {
        if (!FMLLoader.isProduction()) {
            openExample = new KeyMapping("key.oelib.open_fluid_example", GLFW.GLFW_KEY_G, "key.categories.oelib");
            KeyMappingRegister.register(openExample);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (openExample != null && openExample.consumeClick()) {
            Minecraft.getInstance().setScreen(new FluidRenderExampleScreen());
        }
    }
}