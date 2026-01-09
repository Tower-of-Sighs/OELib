package cc.sighs.oelib.fabric;

import cc.sighs.oelib.OELibClient;
import cc.sighs.oelib.bless.FestivalToastConfig;
import cc.sighs.oelib.dev.DevConfig;
import cc.sighs.oelib.dev.example.ExampleMenus;
import cc.sighs.oelib.fabric.bless.FestivalToastManager;
import cc.sighs.oelib.fabric.bless.OverlayRenderer;
import cc.sighs.oelib.fabric.config.ClientConfigHotReloadListeners;
import cc.sighs.oelib.fabric.example.FluidRenderExampleClient;
import cc.sighs.oelib.fabric.example.FluidRenderExampleScreen;
import cc.sighs.oelib.fabric.network.NetworkManagerImpl;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.server.packs.PackType;

public final class OELibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OELibClient.initClient();
        FestivalToastConfig.get();
        FestivalToastManager.init();
        OverlayRenderer.register();
        NetworkManagerImpl.initializeClient();
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new ClientConfigHotReloadListeners());
        if (DevConfig.UNIT.get().enableExampleContent) {
            MenuScreens.register(
                    ExampleMenus.FLUID_RENDER_EXAMPLE.get(),
                    FluidRenderExampleScreen::new
            );
        }
        FluidRenderExampleClient.init();
    }
}
