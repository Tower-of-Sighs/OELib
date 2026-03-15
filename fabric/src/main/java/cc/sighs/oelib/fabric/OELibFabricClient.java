package cc.sighs.oelib.fabric;

import cc.sighs.oelib.OELibClient;
import cc.sighs.oelib.fabric.bless.FestivalToastManager;
import cc.sighs.oelib.fabric.bless.OverlayRenderer;
import cc.sighs.oelib.fabric.config.ClientConfigHotReloadListeners;
import cc.sighs.oelib.fabric.event.TickBridge;
import cc.sighs.oelib.fabric.network.NetworkManagerImpl;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;

public final class OELibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OELibClient.initClient();
        FestivalToastManager.init();
        OverlayRenderer.register();
        NetworkManagerImpl.initializeClient();
        TickBridge.registerClient();
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new ClientConfigHotReloadListeners());
    }
}
