package cc.sighs.oelib.data.net;

import cc.sighs.oelib.OELibData;
import cc.sighs.oelib.data.util.CodecUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class DataSyncPacket<T> {

    private final Class<T> dataClass;
    private final Map<ResourceLocation, T> data;

    public DataSyncPacket(Class<T> dataClass, Map<ResourceLocation, T> data) {
        this.dataClass = dataClass;
        this.data = data;
    }


    public void sendTo(ServerPlayer player) {
        if (player == null) {
            OELibData.LOGGER.warn("Cannot send packet: player is null");
            return;
        }

        try {
            Optional<String> jsonOpt = CodecUtils.encodeToJson(dataClass, data);
            if (jsonOpt.isEmpty()) {
                OELibData.LOGGER.error("Failed to encode {} data to JSON", dataClass.getSimpleName());
                return;
            }

            byte[] dataBytes = jsonOpt.get().getBytes(StandardCharsets.UTF_8);
            OELibData.LOGGER.info("Sending {} data: {} entries, {} bytes",
                    dataClass.getSimpleName(), data.size(), dataBytes.length);

            DataSyncChunkPacket packet = new DataSyncChunkPacket(
                    dataClass.getName(), dataBytes);

            packet.sendTo(player);

        } catch (Exception e) {
            OELibData.LOGGER.error("Failed to send {} sync packet: {}", dataClass.getSimpleName(), e.getMessage(), e);
        }
    }

    public void sendToAll() {
        try {
            Optional<String> jsonOpt = CodecUtils.encodeToJson(dataClass, data);
            if (jsonOpt.isEmpty()) {
                OELibData.LOGGER.error("Failed to encode {} data to JSON", dataClass.getSimpleName());
                return;
            }

            byte[] dataBytes = jsonOpt.get().getBytes(StandardCharsets.UTF_8);
            OELibData.LOGGER.info("Sending {} data to all players: {} entries, {} bytes",
                    dataClass.getSimpleName(), data.size(), dataBytes.length);

            DataSyncChunkPacket packet = new DataSyncChunkPacket(
                    dataClass.getName(), dataBytes);

            packet.sendToAll();

        } catch (Exception e) {
            OELibData.LOGGER.error("Failed to send {} sync packet to all players: {}", dataClass.getSimpleName(), e.getMessage(), e);
        }
    }
}
