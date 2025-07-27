package com.mafuyu404.oelib.network;

import com.mafuyu404.oelib.OElib;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 网络处理器。
 * <p>
 * 负责注册和管理所有网络数据包。
 * </p>
 *
 * @author Flechazo
 * @since 1.0.0
 */
public class NetworkHandler {
    
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(OElib.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    /**
     * 注册所有网络数据包。
     */
    public static void register() {
        int id = 0;
        
        INSTANCE.registerMessage(id++, DataSyncChunkPacket.class,
                DataSyncChunkPacket::encode,
                DataSyncChunkPacket::decode,
                DataSyncChunkPacket::handle);
        
        OElib.LOGGER.info("Registered network packets for data synchronization");
    }
}