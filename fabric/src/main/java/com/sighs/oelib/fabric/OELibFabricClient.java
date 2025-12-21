package com.sighs.oelib.fabric;

import com.sighs.oelib.OELibClient;
import com.sighs.oelib.example.ExampleMenus;
import com.sighs.oelib.fabric.example.FluidRenderExampleClient;
import com.sighs.oelib.fabric.example.FluidRenderExampleScreen;
import com.sighs.oelib.fabric.network.NetworkManagerImpl;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public final class OELibFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OELibClient.initClient();
        NetworkManagerImpl.initializeClient();
        MenuScreens.register(
                ExampleMenus.FLUID_RENDER_EXAMPLE.get(),
                FluidRenderExampleScreen::new
        );
        FluidRenderExampleClient.init();
    }
}
