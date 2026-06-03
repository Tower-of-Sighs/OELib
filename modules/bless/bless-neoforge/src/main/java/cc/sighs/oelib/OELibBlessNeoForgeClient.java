package cc.sighs.oelib;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = OELibBless.MOD_ID, dist = Dist.CLIENT)
public class OELibBlessNeoForgeClient {
    public OELibBlessNeoForgeClient(IEventBus modEventBus, ModContainer modContainer) {
        OELibBlessClient.init();
    }
}
