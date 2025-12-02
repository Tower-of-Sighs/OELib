package com.mafuyu404.oelib.neoforge;

import com.mafuyu404.oelib.OELib;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(OELib.MODID)
public final class OELibNeoForge {
    public OELibNeoForge(IEventBus modEventBus) {
        OELib.init();
    }
}
