package com.mafuyu404.oelib;

import com.mafuyu404.oelib.core.DataRegistry;
import com.mafuyu404.oelib.network.NetworkHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(OElib.MODID)
public class OElib {

    public static final String MODID = "oelib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public OElib(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
    }

    public void commonSetup(FMLCommonSetupEvent event) {
        DataRegistry.initialize();
    }
    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        NetworkHandler.register(event);
    }
}
