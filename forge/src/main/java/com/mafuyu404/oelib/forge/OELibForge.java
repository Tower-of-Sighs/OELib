package com.mafuyu404.oelib.forge;

import com.mafuyu404.oelib.OELib;
import com.mafuyu404.oelib.forge.data.DataRegistry;
import com.mafuyu404.oelib.forge.network.NetworkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(OELib.MODID)
public final class OELibForge {
    public OELibForge() {
        OELib.init();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void commonSetup(FMLCommonSetupEvent event) {
        DataRegistry.initialize();
        NetworkManager.initialize();
    }
}
