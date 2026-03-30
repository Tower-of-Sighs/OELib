package cc.sighs.oelib.fabric;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.OELibClient;
import cc.sighs.oelib.fabric.bless.FestivalToastManager;
import cc.sighs.oelib.fabric.bless.OverlayRenderer;
import cc.sighs.oelib.fabric.config.ClientConfigHotReloadListeners;
import cc.sighs.oelib.fabric.event.TickBridge;
import cc.sighs.oelib.fabric.network.NetworkManagerImpl;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

public final class OELibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TickBridge.registerClient();
        OELibClient.initClient();
        FestivalToastManager.init();
        OverlayRenderer.register();
        NetworkManagerImpl.initializeClient();
        var loader = ResourceLoader.get(PackType.CLIENT_RESOURCES);
        loader.registerReloadListener(Identifier.fromNamespaceAndPath(OELib.MODID, "config_hot_reload_client"), new ClientConfigHotReloadListeners());
    }
}
