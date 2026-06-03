package cc.sighs.oelib;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(OELibData.MOD_ID)
public class OELibDataNeoForge {

    public OELibDataNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        OELibData.init();
    }
}
