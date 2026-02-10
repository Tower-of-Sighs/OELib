package cc.sighs.oelib.forge;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.OELibClient;
import cc.sighs.oelib.config.ui.screen.ConfigScreen;
import cc.sighs.oelib.forge.example.FluidRenderExampleClient;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

public final class OELibForgeClient {

    public static void onClientSetup() {
        OELibClient.initClient();
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent)
                        -> new ConfigScreen(parent, OELib.MODID)));
        FluidRenderExampleClient.onRegisterKeys();
    }
}
