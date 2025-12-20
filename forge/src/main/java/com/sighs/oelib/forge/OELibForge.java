package com.sighs.oelib.forge;

import com.sighs.oelib.OELib;
import com.sighs.oelib.example.ExampleRegistry;
import com.sighs.oelib.forge.network.NetworkManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(OELib.MODID)
public class OELibForge {
    public OELibForge() {
        var eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        OELib.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> OELibForgeClient::onClientSetup);
        eventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void commonSetup(FMLCommonSetupEvent event) {
        ExampleRegistry.registerFuel();
        NetworkManager.initialize();
    }
}
