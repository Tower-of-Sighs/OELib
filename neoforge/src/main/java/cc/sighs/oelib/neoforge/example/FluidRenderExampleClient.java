package cc.sighs.oelib.neoforge.example;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.dev.DevConfig;
import cc.sighs.oelib.dev.example.ExampleMenus;
import cc.sighs.oelib.dev.example.net.OpenGuiPacket;
import cc.sighs.oelib.registry.extra.KeyMappingRegister;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = OELib.MODID, value = Dist.CLIENT)
public final class FluidRenderExampleClient {
    private static KeyMapping openExample;

    public static void onRegisterKeys() {
        if (DevConfig.UNIT.get().enableExampleContent) {
            openExample = new KeyMapping("key.oelib.open_fluid_example", GLFW.GLFW_KEY_G, "key.categories.oelib");
            KeyMappingRegister.register(openExample);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (DevConfig.UNIT.get().enableExampleContent) {
            if (openExample != null && openExample.consumeClick()) {
                var mc = Minecraft.getInstance();
                if (mc.level != null && mc.hitResult instanceof BlockHitResult blockHitResult) {
                    var pos = blockHitResult.getBlockPos();
                    var title = Component.literal("Fluid Example");

                    var registry = mc.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);

                    var holder = registry.getHolder(Enchantments.EFFICIENCY);

                    if (holder.isPresent()) {
                        var packet = new OpenGuiPacket(pos, title, holder.get());
                        packet.sendToServer();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void registerMenus(RegisterMenuScreensEvent event) {
        if (DevConfig.UNIT.get().enableExampleContent) {
            event.register(
                    ExampleMenus.FLUID_RENDER_EXAMPLE.get(),
                    FluidRenderExampleScreen::new
            );
        }
    }
}