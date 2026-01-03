package cc.sighs.oelib;

import cc.sighs.oelib.bless.BlessToastCommand;
import cc.sighs.oelib.bless.OverlayRegistry;
import cc.sighs.oelib.bless.ShaderEvents;
import cc.sighs.oelib.dev.ExampleInit;

public class OELibClient {
    public static void initClient() {
        OverlayRegistry.init();
        BlessToastCommand.register();
        ShaderEvents.register();
        ExampleInit.initClient();
    }
}