package cc.sighs.oelib;

import cc.sighs.oelib.data.net.DataSyncChunkPacket;
import cc.sighs.oelib.example.ClientRainbowBarComponent;
import cc.sighs.oelib.example.ExampleRegistry;
import cc.sighs.oelib.example.RainbowBarComponent;
import cc.sighs.oelib.network.api.NetworkManager;
import cc.sighs.oelib.registry.extra.ClientTooltipComponentRegister;
import cc.sighs.oelib.registry.extra.RenderTypeRegister;
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