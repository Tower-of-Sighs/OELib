package com.mafuyu404.oelib.network;

import com.mafuyu404.oelib.OElib;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(OElib.MODID)
                .versioned(PROTOCOL_VERSION);

        registrar.playToClient(
                DataSyncChunkPacket.TYPE,
                DataSyncChunkPacket.STREAM_CODEC,
                DataSyncChunkPacket::handle
        );

        OElib.LOGGER.info("Registered network packets for data synchronization");
    }
}