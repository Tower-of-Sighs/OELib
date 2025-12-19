package com.sighs.oelib.neoforge;

import com.sighs.oelib.OELib;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(OELib.MODID)
public final class OELibNeoForge {
    public OELibNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        OELib.init();
    }

}
