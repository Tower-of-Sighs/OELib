package cc.sighs.oelib;

import cc.sighs.oelib.bless.FestivalToastConfig;
import cc.sighs.oelib.bless.OverlayRegistry;
import cc.sighs.oelib.bless.ShaderEvents;

public class OELibBlessClient {

    public static void init() {
        FestivalToastConfig.register();
        OverlayRegistry.init();
        ShaderEvents.register();
    }
}