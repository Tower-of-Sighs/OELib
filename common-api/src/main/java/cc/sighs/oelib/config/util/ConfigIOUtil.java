package cc.sighs.oelib.config.util;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.ConfigManager;
import cc.sighs.oelib.config.ConfigUnit;
import cc.sighs.oelib.config.ServerConfigManager;
import cc.sighs.oelib.config.model.ConfigSide;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.net.ConfigSyncPacket;
import cc.sighs.oelib.network.api.NetworkManager;
import net.minecraft.server.level.ServerPlayer;

public final class ConfigIOUtil {

    private ConfigIOUtil() {
    }

    public static <T> void applyUpdate(ConfigUnit<T> unit, ServerPlayer player, String payload, ConfigStorageFormat format, boolean save) {
        if (unit.meta().side() != ConfigSide.SERVER) {
            OELib.LOGGER.warn("Rejected update for non-server config {}", unit.id());
            return;
        }
        var checkerOpt = ServerConfigManager.getPermissionChecker(unit.id());
        if (checkerOpt.isEmpty()) {
            OELib.LOGGER.warn("No permission checker for server config {}; rejecting client update", unit.id());
            return;
        }
        var checker = checkerOpt.get();
        if (!checker.canUpdate(player)) {
            OELib.LOGGER.warn("Player {} not permitted to update config {}", player.getGameProfile().getName(), unit.id());
            return;
        }

        try {
            var result = ConfigSerializationUtil.parse(payload, format, unit.codec().codec());
            if (result.error().isPresent()) {
                OELib.LOGGER.error("Failed to parse update for config {}: {}", unit.id(), result.error().get().message());
                return;
            }
            result.result().ifPresent(v -> {
                OELib.LOGGER.info("Server applying update for config {} requested by {} with format {} (save={})",
                        unit.id(), player.getGameProfile().getName(), format, save);
                unit.setValue(v);
                if (save) {
                    unit.save();
                }
                ConfigManager.encodeToString(unit.id()).ifPresent(encoded -> {
                    OELib.LOGGER.info("Broadcasting config {} to clients", unit.id());
                    NetworkManager.sendToAll(new ConfigSyncPacket(unit.id(), encoded.payload(), encoded.format()));
                });
            });
        } catch (Exception e) {
            OELib.LOGGER.error("Exception while applying server config update {}", unit.id(), e);
        }
    }
}
