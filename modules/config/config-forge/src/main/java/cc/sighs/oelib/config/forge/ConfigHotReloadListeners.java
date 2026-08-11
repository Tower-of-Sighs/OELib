package cc.sighs.oelib.config.forge;

import cc.sighs.oelib.config.OELibConfig;
import cc.sighs.oelib.config.ClientConfigManager;
import cc.sighs.oelib.config.ServerConfigManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OELibConfig.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ConfigHotReloadListeners {

    @SubscribeEvent
    public static void registerServerListener(AddReloadListenerEvent event) {
        event.addListener(new ServerConfigManager());
    }

    @Mod.EventBusSubscriber(modid = OELibConfig.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    static class ClientConfigHotReloadListeners {
        @SubscribeEvent
        public static void registerClientListener(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new ClientConfigManager());
        }
    }
}
