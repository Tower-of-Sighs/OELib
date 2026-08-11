package cc.sighs.oelib.dev;

import cc.sighs.oelib.dev.example.BlessToastCommand;
import cc.sighs.oelib.dev.example.*;
import cc.sighs.oelib.dev.example.event.FluidRenderExampleClient;
import cc.sighs.oelib.registry.extra.ClientTooltipComponentRegister;
import cc.sighs.oelib.registry.extra.RenderTypeRegister;
import net.minecraft.client.renderer.RenderType;

public class ExampleInit {
    public static void init() {
        if (DevConfig.UNIT.get().enableExampleContent()) {
            TestPacketCommand.register();
            ExampleRegistry.init();
            ExampleCreativeTab.init();
            ExampleCreativeTab.registerCreativeTabEntries();
            ExampleCreativeTab.modifyCreativeTab();
            ExampleRegistry.registerFuel();
            ExampleMenus.init();
        }
    }

    public static void initClient() {
        if (DevConfig.UNIT.get().enableExampleContent()) {
            FluidRenderExampleClient.onRegisterKeys();
            BlessToastCommand.register();
            RenderTypeRegister.registerBlocks(RenderType.cutout(), ExampleRegistry.EXAMPLE_BLOCK);
            ClientTooltipComponentRegister.register(
                    RainbowBarComponent.class,
                    ClientRainbowBarComponent::new
            );
            ExampleMenus.registerScreens();
        }
    }
}
