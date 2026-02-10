package cc.sighs.oelib.forge.example;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.dev.DevConfig;
import cc.sighs.oelib.dev.example.net.OpenGuiPacket;
import cc.sighs.oelib.registry.extra.KeyMappingRegister;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = OELib.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FluidRenderExampleClient {
    private static KeyMapping openExample;

    public static void onRegisterKeys() {
        if (DevConfig.UNIT.get().enableExampleContent()) {
            openExample = new KeyMapping("key.oelib.open_fluid_example", GLFW.GLFW_KEY_G, "key.categories.oelib");
            KeyMappingRegister.register(openExample);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (DevConfig.UNIT.get().enableExampleContent()) {
            if (openExample != null && openExample.consumeClick()) {
                var mc = Minecraft.getInstance();
                if (mc.level != null && mc.hitResult instanceof BlockHitResult blockHitResult) {
                    var pos = blockHitResult.getBlockPos();
                    var title = Component.literal("Fluid Example");
                    var enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(Enchantments.BLOCK_EFFICIENCY);

                    if (enchantmentId != null) {
                        var packet = new OpenGuiPacket(pos, title, enchantmentId);
                        packet.sendToServer();
                    }
                }
            }
        }
    }
}
