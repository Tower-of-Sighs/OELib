package cc.sighs.oelib.neoforge.example;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.example.ExampleMenus;
import cc.sighs.oelib.example.OpenGuiPacket;
import cc.sighs.oelib.registry.extra.KeyMappingRegister;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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
            var mc = Minecraft.getInstance();
            if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
                var pos = ((BlockHitResult) mc.hitResult).getBlockPos();
                var packet = new OpenGuiPacket(pos);
                packet.sendToServer();
            }
        }
    }

    @SubscribeEvent
    public static void registerMenus(RegisterMenuScreensEvent event) {
        event.register(
                ExampleMenus.FLUID_RENDER_EXAMPLE.get(),
                FluidRenderExampleScreen::new
        );
    }
}