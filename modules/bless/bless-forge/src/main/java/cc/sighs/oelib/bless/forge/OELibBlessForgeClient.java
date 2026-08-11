package cc.sighs.oelib.bless.forge;

import cc.sighs.oelib.bless.OELibBlessClient;
import cc.sighs.oelib.config.ui.screen.ConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

public final class OELibBlessForgeClient {

    public static void onClientSetup() {
        OELibBlessClient.init();
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent)
                        -> new ConfigScreen(parent, "oelib")));
    }
}