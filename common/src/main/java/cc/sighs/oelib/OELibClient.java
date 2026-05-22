package cc.sighs.oelib;

import cc.sighs.oelib.bless.FestivalToastConfig;
import cc.sighs.oelib.bless.OverlayRegistry;
import cc.sighs.oelib.bless.ShaderEvents;
import cc.sighs.oelib.dev.ExampleInit;

public class OELibClient {
    public static void initClient() {
        FestivalToastConfig.register();
        OverlayRegistry.init();
        ShaderEvents.register();
        ExampleInit.initClient();

    }
}