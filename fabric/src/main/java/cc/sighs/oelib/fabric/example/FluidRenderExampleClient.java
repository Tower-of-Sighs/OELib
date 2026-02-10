package cc.sighs.oelib.fabric.example;

import cc.sighs.oelib.dev.DevConfig;
import cc.sighs.oelib.dev.example.net.OpenGuiPacket;
import cc.sighs.oelib.registry.extra.KeyMappingRegister;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.BlockHitResult;
import org.lwjgl.glfw.GLFW;

public final class FluidRenderExampleClient {
    private static KeyMapping openExampleKey;

    public static void init() {
        if (DevConfig.UNIT.get().enableExampleContent()) {
            openExampleKey = new KeyMapping("key.oelib.open_fluid_example", GLFW.GLFW_KEY_G, "key.categories.oelib");
            KeyMappingRegister.register(openExampleKey);
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (openExampleKey.consumeClick()) {
                    var mc = Minecraft.getInstance();
                    if (mc.level != null && mc.hitResult instanceof BlockHitResult blockHitResult) {
                        var pos = blockHitResult.getBlockPos();
                        var title = Component.literal("Fluid Example");
                        var enchantmentId = BuiltInRegistries.ENCHANTMENT.getKey(Enchantments.BLOCK_EFFICIENCY);

                        if (enchantmentId != null) {
                            var packet = new OpenGuiPacket(pos, title, enchantmentId);
                            packet.sendToServer();
                        }
                    }
                }
            });
        }
    }
}