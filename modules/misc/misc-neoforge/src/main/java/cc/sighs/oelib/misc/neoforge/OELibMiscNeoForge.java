package cc.sighs.oelib.misc.neoforge;

import cc.sighs.oelib.misc.OELibMisc;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(cc.sighs.oelib.misc.OELibMisc.MOD_ID)
public class OELibMiscNeoForge {

    public OELibMiscNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        OELibMisc.init();
    }
}
