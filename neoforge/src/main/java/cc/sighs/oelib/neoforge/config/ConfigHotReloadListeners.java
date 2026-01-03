package cc.sighs.oelib.neoforge.config;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.ClientConfigManager;
import cc.sighs.oelib.config.ServerConfigManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@EventBusSubscriber(modid = OELib.MODID)
public class ConfigHotReloadListeners {

    @SubscribeEvent
    public static void registerServerListener(AddReloadListenerEvent event) {
        event.addListener(new ServerConfigManager());
    }

    @EventBusSubscriber(modid = OELib.MODID, value = Dist.CLIENT)
    static class ClientConfigHotReloadListeners {
        @SubscribeEvent
        public static void registerClientListener(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener(new ClientConfigManager());
        }
    }
}