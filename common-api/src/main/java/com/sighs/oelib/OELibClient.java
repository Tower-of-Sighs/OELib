package com.sighs.oelib;

import com.sighs.oelib.data.net.DataSyncChunkPacket;
import com.sighs.oelib.example.ClientRainbowBarComponent;
import com.sighs.oelib.example.ExampleRegistry;
import com.sighs.oelib.example.RainbowBarComponent;
import com.sighs.oelib.network.api.NetworkManager;
import com.sighs.oelib.registry.extra.ClientTooltipComponentRegister;
import com.sighs.oelib.registry.extra.RenderTypeRegister;
import net.minecraft.client.renderer.RenderType;

public final class OELibClient {
    public static void initClient() {
        NetworkManager.registerPackets(DataSyncChunkPacket.class);
        RenderTypeRegister.registerBlocks(RenderType.cutout(), ExampleRegistry.EXAMPLE_BLOCK);
        ClientTooltipComponentRegister.register(
                RainbowBarComponent.class,
                ClientRainbowBarComponent::new
        );
    }
}