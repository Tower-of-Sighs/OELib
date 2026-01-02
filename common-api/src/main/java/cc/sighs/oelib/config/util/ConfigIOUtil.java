package cc.sighs.oelib.config.util;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.ConfigManager;
import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.net.ConfigSyncPacket;
import cc.sighs.oelib.network.api.NetworkManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.DataResult;
import de.marhali.json5.Json5;
import net.minecraft.server.level.ServerPlayer;

public final class ConfigIOUtil {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Json5 JSON5 = Json5.builder(builder -> builder.parseComments().build());

    private ConfigIOUtil() {
    }

    public static <T> void applyUpdate(ConfigUnit<T> unit, ServerPlayer player, String payload, ConfigStorageFormat format, boolean save) {
        if (unit.meta().side() != ConfigSide.SERVER) {
            OELib.LOGGER.warn("Rejected update for non-server config {}", unit.id());
            return;
        }
        int required = unit.meta().permissionLevel();
        var fields = unit.codec().fields();
        if (fields != null) {
            int fieldMax = fields.stream().mapToInt(f -> f.permissionLevel()).max().orElse(0);
            if (fieldMax > 0) {
                required = fieldMax;
            }
        }
        if (required > 0 && !player.hasPermissions(required)) {
            OELib.LOGGER.warn("Player {} lacks permission {} to update config {}", player.getGameProfile().getName(), required, unit.id());
            return;
        }

        try {
            DataResult<T> result = ConfigSerializationUtil.parse(payload, format, unit.codec().codec());
            if (result.error().isPresent()) {
                OELib.LOGGER.error("Failed to parse update for config {}: {}", unit.id(), result.error().get().message());
                return;
            }
            result.result().ifPresent(v -> {
                unit.setValue(v);
                if (save) {
                    unit.save();
                }
                ConfigManager.encodeToString(unit.id()).ifPresent(encoded -> {
                    NetworkManager.sendToAll(new ConfigSyncPacket(unit.id(), encoded.payload(), encoded.format()));
                });
            });
        } catch (Exception e) {
            OELib.LOGGER.error("Exception while applying server config update {}", unit.id(), e);
        }
    }
}
