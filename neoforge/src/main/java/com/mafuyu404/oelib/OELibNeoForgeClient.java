package com.mafuyu404.oelib;

import com.mafuyu404.oelib.neoforge.icon.DynamicIconForgeSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = OELib.MODID, value = Dist.CLIENT)
public class OELibNeoForgeClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        DynamicIconForgeSubscriber.onClientSetup();
    }
}
