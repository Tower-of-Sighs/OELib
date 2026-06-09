package cc.sighs.oelib.bless;

public class OELibBlessClient {

    public static void init() {
        FestivalToastConfig.register();
        OverlayRegistry.init();
        ShaderEvents.register();
    }
}