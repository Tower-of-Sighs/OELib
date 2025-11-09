package com.mafuyu404.oelib.forge;

import com.mafuyu404.oelib.OELib;
import com.mafuyu404.oelib.api.net.NetworkManager;
import com.mafuyu404.oelib.forge.data.net.DataSyncChunkPacket;
import com.mafuyu404.oelib.forge.icon.DynamicIconForgeSubscriber;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = OELib.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class OELibForgeClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        NetworkManager.registerPackets(DataSyncChunkPacket.class);
        DynamicIconForgeSubscriber.onClientSetup();
    }
}
