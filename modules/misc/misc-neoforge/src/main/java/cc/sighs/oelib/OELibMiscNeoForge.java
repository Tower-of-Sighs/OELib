package cc.sighs.oelib;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(OELibMisc.MOD_ID)
public class OELibMiscNeoForge {

    public OELibMiscNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        OELibMisc.init();
    }
}
