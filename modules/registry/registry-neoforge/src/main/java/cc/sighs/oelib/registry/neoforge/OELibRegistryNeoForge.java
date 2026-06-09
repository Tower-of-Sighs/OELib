package cc.sighs.oelib.registry.neoforge;

import cc.sighs.oelib.registry.OELibRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(OELibRegistry.MOD_ID)
public class OELibRegistryNeoForge {

    public OELibRegistryNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        OELibRegistry.init();
    }
}
