package cc.sighs.oelib.fabric.example;

import cc.sighs.oelib.example.OpenGuiPacket;
import cc.sighs.oelib.registry.extra.KeyMappingRegister;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

public final class FluidRenderExampleClient {
    private static KeyMapping openExampleKey;

    public static void init() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            openExampleKey = new KeyMapping("key.oelib.open_fluid_example", GLFW.GLFW_KEY_G, "key.categories.oelib");
            KeyMappingRegister.register(openExampleKey);
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (openExampleKey.consumeClick()) {
                    var mc = Minecraft.getInstance();
                    if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
                        var pos = ((BlockHitResult) mc.hitResult).getBlockPos();
                        var packet = new OpenGuiPacket(pos);
                        packet.sendToServer();
                    }
                }
            });
        }
    }
}