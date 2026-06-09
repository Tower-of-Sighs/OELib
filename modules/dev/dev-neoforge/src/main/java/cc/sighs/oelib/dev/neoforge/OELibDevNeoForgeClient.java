package cc.sighs.oelib.dev.neoforge;

import cc.sighs.oelib.dev.OELibDev;
import cc.sighs.oelib.dev.OELibDevClient;
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
