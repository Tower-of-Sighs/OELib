package cc.sighs.oelib.dev.neoforge;

import cc.sighs.oelib.dev.OELibDev;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(OELibDev.MOD_ID)
public class OELibDevNeoForge {

    public OELibDevNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        OELibDev.init();
    }
}
