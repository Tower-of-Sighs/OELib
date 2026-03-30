package cc.sighs.oelib.neoforge.config;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.ClientConfigManager;
import cc.sighs.oelib.config.ServerConfigManager;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

@EventBusSubscriber(modid = OELib.MODID)
public class ConfigHotReloadListeners {

    @SubscribeEvent
    public static void registerServerListener(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath(OELib.MODID, "config_hot_reload_server"), new ServerConfigManager());
    }

    @EventBusSubscriber(modid = OELib.MODID, value = Dist.CLIENT)
    static class ClientConfigHotReloadListeners {
        @SubscribeEvent
        public static void registerClientListener(AddClientReloadListenersEvent event) {
            event.addListener(Identifier.fromNamespaceAndPath(OELib.MODID, "config_hot_reload_client"), new ClientConfigManager());
        }
    }
}
