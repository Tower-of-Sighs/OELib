package cc.sighs.oelib.dev.example.event;

import cc.sighs.oelib.dev.DevConfig;
import cc.sighs.oelib.dev.example.net.OpenGuiPacket;
import cc.sighs.oelib.event.Subscribe;
import cc.sighs.oelib.event.events.ClientTickEvent;
import cc.sighs.oelib.registry.extra.KeyMappingRegister;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.BlockHitResult;
import org.lwjgl.glfw.GLFW;

public class FluidRenderExampleClient {
    private static KeyMapping openExample;

    public static void onRegisterKeys() {
        if (DevConfig.UNIT.get().enableExampleContent()) {
            openExample = new KeyMapping("key.oelib_dev.open_fluid_example", GLFW.GLFW_KEY_G, "key.categories.oelib_dev");
            KeyMappingRegister.register(openExample);
        }
    }

    @Subscribe
    public static void onClientTick(ClientTickEvent.Post event) {
        if (DevConfig.UNIT.get().enableExampleContent()) {
            if (openExample != null && openExample.consumeClick()) {
                var mc = Minecraft.getInstance();
                if (mc.level != null && mc.hitResult instanceof BlockHitResult blockHitResult) {
                    var pos = blockHitResult.getBlockPos();
                    var title = Component.literal("Fluid Example");
                    var enchantmentId = new ResourceLocation("minecraft", "efficiency");
                    var packet = new OpenGuiPacket(pos, title, enchantmentId);
                    packet.sendToServer();
                }
            }
        }
    }
}
