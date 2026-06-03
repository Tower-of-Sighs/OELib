package cc.sighs.oelib;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(OELibConfig.MOD_ID)
public class OELibConfigNeoForge {

    public OELibConfigNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        OELibConfig.init();
    }

}
