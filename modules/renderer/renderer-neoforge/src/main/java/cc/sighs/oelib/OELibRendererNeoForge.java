package cc.sighs.oelib;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(OELibRenderer.MOD_ID)
public class OELibRendererNeoForge {

    public OELibRendererNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        OELibRenderer.init();
    }
}
