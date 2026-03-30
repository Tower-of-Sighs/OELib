package cc.sighs.oelib.data.net;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.data.DataManager;
import cc.sighs.oelib.network.api.INetworkContext;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.api.Side;
import cc.sighs.oelib.util.CodecUtils;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@NetworkPacket(modId = OELib.MODID, id = "data_sync_chunk", side = Side.BOTH, chunkThreshold = 30000)
public record DataSyncChunkPacket(String dataClassName,
                                  byte[] dataBytes) implements INetworkPacket<DataSyncChunkPacket> {

    @Override
    public void handle(INetworkContext context) {
        try {
            String json = new String(dataBytes, StandardCharsets.UTF_8);
            Class<?> dataClass = Class.forName(dataClassName);
            @SuppressWarnings("unchecked")
            Optional<Map<Identifier, ?>> dataOpt =
                    (Optional<Map<Identifier, ?>>) (Object)
                            CodecUtils.decodeFromJson((Class<Object>) dataClass, json);
            if (dataOpt.isPresent()) {
                Map<Identifier, ?> data = dataOpt.get();
                DataManager.updateClientDataRaw(dataClass, data);
                OELib.LOGGER.info("Processed {} {} data entries", data.size(), dataClass.getSimpleName());
            } else {
                OELib.LOGGER.error("Failed to parse JSON data for {}", dataClassName);
            }
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to handle data sync packet: {}", e.getMessage(), e);
        }
    }
}