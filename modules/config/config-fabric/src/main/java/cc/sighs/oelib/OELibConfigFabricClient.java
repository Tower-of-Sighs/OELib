package cc.sighs.oelib;

import cc.sighs.oelib.config.fabric.ClientConfigHotReloadListeners;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;

public class OELibConfigFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new ClientConfigHotReloadListeners());
    }
}
