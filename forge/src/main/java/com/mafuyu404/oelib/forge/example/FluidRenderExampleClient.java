package com.mafuyu404.oelib.forge.example;

import com.mafuyu404.oelib.OELib;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = OELib.MODID, value = Dist.CLIENT)
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
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && openExample != null && openExample.consumeClick()) {
            Minecraft.getInstance().setScreen(new FluidRenderExampleScreen());
        }
    }
}