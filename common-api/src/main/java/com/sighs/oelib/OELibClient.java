package com.sighs.oelib;

import com.sighs.oelib.example.ClientRainbowBarComponent;
import com.sighs.oelib.example.ExampleRegistry;
import com.sighs.oelib.example.RainbowBarComponent;
import com.sighs.oelib.registry.extra.ClientTooltipComponentRegister;
import com.sighs.oelib.registry.extra.RenderTypeRegister;
import net.minecraft.client.renderer.RenderType;

public class OELibClient {
    public static void initClient() {
        RenderTypeRegister.registerBlocks(RenderType.cutout(), ExampleRegistry.EXAMPLE_BLOCK);
        ClientTooltipComponentRegister.register(
                RainbowBarComponent.class,
                ClientRainbowBarComponent::new
        );
    }
}
