package cc.sighs.oelib;

import cc.sighs.oelib.bless.BlessToastCommand;
import cc.sighs.oelib.bless.OverlayRegistry;
import cc.sighs.oelib.bless.ShaderEvents;
import cc.sighs.oelib.example.ClientRainbowBarComponent;
import cc.sighs.oelib.example.ExampleRegistry;
import cc.sighs.oelib.example.RainbowBarComponent;
import cc.sighs.oelib.registry.extra.ClientTooltipComponentRegister;
import cc.sighs.oelib.registry.extra.RenderTypeRegister;
import net.minecraft.client.renderer.RenderType;

public class OELibClient {
    public static void initClient() {
        OverlayRegistry.init();
        BlessToastCommand.register();
        ShaderEvents.register();
        RenderTypeRegister.registerBlocks(RenderType.cutout(), ExampleRegistry.EXAMPLE_BLOCK);
        ClientTooltipComponentRegister.register(
                RainbowBarComponent.class,
                ClientRainbowBarComponent::new
        );
    }
}