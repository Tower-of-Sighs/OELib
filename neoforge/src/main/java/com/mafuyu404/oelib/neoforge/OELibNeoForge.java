package com.mafuyu404.oelib.neoforge;

import com.mafuyu404.oelib.OELib;
import com.mafuyu404.oelib.neoforge.data.DataRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(OELib.MODID)
public final class OELibNeoForge {
    public OELibNeoForge(IEventBus modEventBus) {
        OELib.init();
        modEventBus.addListener(this::commonSetup);
    }

    public void commonSetup(FMLCommonSetupEvent event) {
        DataRegistry.initialize();
    }
}
