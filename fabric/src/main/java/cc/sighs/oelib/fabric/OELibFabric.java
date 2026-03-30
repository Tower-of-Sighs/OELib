package cc.sighs.oelib.fabric;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.fabric.config.ConfigHotReloadListeners;
import cc.sighs.oelib.fabric.event.TickBridge;
import cc.sighs.oelib.fabric.network.NetworkManagerImpl;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

public class OELibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        TickBridge.registerServer();
        OELib.init();
        NetworkManagerImpl.initialize();
        var loader = ResourceLoader.get(PackType.SERVER_DATA);
        loader.registerReloadListener(Identifier.fromNamespaceAndPath(OELib.MODID, "config_hot_reload_server"), new ConfigHotReloadListeners());
    }
}
