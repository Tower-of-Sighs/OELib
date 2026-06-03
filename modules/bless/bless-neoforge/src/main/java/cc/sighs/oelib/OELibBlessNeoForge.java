package cc.sighs.oelib;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(OELibBless.MOD_ID)
public class OELibBlessNeoForge {

    public OELibBlessNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        OELibBless.init();
    }
}
