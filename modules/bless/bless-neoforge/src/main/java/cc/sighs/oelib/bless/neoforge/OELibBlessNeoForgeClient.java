package cc.sighs.oelib.bless.neoforge;

import cc.sighs.oelib.bless.OELibBless;
import cc.sighs.oelib.bless.OELibBlessClient;
import cc.sighs.oelib.config.ui.screen.ConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = OELibBless.MOD_ID, dist = Dist.CLIENT)
public class OELibBlessNeoForgeClient {
    public OELibBlessNeoForgeClient(IEventBus modEventBus, ModContainer modContainer) {
        OELibBlessClient.init();
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> (minecraft, parent) -> new ConfigScreen(parent, "oelib"));
    }
}
