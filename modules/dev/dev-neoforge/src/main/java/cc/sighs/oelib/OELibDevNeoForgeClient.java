package cc.sighs.oelib;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = OELibDev.MOD_ID, dist = Dist.CLIENT)
public class OELibDevNeoForgeClient {

    public OELibDevNeoForgeClient(IEventBus modEventBus, ModContainer modContainer) {
        OELibDevClient.init();
    }
}
