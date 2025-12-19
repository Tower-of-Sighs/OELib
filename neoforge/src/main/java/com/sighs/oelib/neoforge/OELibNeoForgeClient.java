package com.sighs.oelib.neoforge;

import com.sighs.oelib.OELib;
import com.sighs.oelib.OELibClient;
import com.sighs.oelib.neoforge.example.FluidRenderExampleClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = OELib.MODID, dist = Dist.CLIENT)
public class OELibNeoForgeClient {

    public OELibNeoForgeClient(IEventBus eventBus, ModContainer container) {
        OELibClient.initClient();
        FluidRenderExampleClient.onRegisterKeys();
    }
}
