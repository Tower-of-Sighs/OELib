package cc.sighs.oelib;

import cc.sighs.oelib.bless.fabric.FestivalToastManager;
import cc.sighs.oelib.bless.fabric.OverlayRenderer;
import net.fabricmc.api.ClientModInitializer;

public class OELibBlessFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OELibBlessClient.init();
        FestivalToastManager.init();
        OverlayRenderer.register();
    }
}
