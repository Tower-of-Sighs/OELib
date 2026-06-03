package cc.sighs.oelib.data.net;

import cc.sighs.oelib.OELibData;
import cc.sighs.oelib.data.DataManager;
import cc.sighs.oelib.data.util.CodecUtils;
import cc.sighs.oelib.network.api.INetworkContext;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.api.Side;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@NetworkPacket(modId = OELibData.MOD_ID, id = "data_sync_chunk", side = Side.BOTH, chunkThreshold = 30000)
public record DataSyncChunkPacket(String dataClassName,
                                  byte[] dataBytes) implements INetworkPacket<DataSyncChunkPacket> {

    @Override
    public void handle(INetworkContext context) {
        try {
            String json = new String(dataBytes, StandardCharsets.UTF_8);
            Class<?> dataClass = Class.forName(dataClassName);
            @SuppressWarnings("unchecked")
            Optional<Map<ResourceLocation, ?>> dataOpt =
                    (Optional<Map<ResourceLocation, ?>>) (Object)
                            CodecUtils.decodeFromJson((Class<Object>) dataClass, json);
            if (dataOpt.isPresent()) {
                Map<ResourceLocation, ?> data = dataOpt.get();
                DataManager.updateClientDataRaw(dataClass, data);
                OELibData.LOGGER.info("Processed {} {} data entries", data.size(), dataClass.getSimpleName());
            } else {
                OELibData.LOGGER.error("Failed to parse JSON data for {}", dataClassName);
            }
        } catch (Exception e) {
            OELibData.LOGGER.error("Failed to handle data sync packet: {}", e.getMessage(), e);
        }
    }
}