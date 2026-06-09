package cc.sighs.oelib.bless.fabric;

import cc.sighs.oelib.bless.OELibBlessClient;
import net.fabricmc.api.ClientModInitializer;

public class OELibBlessFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OELibBlessClient.init();
        FestivalToastManager.init();
        OverlayRenderer.register();
    }
}
