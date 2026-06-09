package cc.sighs.oelib.bless.neoforge;

import cc.sighs.oelib.bless.OELibBless;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(OELibBless.MOD_ID)
public class OELibBlessNeoForge {

    public OELibBlessNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        OELibBless.init();
    }
}
