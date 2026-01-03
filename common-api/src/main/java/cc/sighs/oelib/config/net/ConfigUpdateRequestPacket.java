package cc.sighs.oelib.config.net;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.ConfigManager;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.config.util.ConfigIOUtil;
import cc.sighs.oelib.network.api.INetworkContext;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.api.Side;
import net.minecraft.resources.ResourceLocation;


@NetworkPacket(modId = OELib.MODID, id = "config_update_request", side = Side.SERVER)
public record ConfigUpdateRequestPacket(
        ResourceLocation configId,
        String payload,
        ConfigStorageFormat format,
        boolean save
) implements INetworkPacket<ConfigUpdateRequestPacket> {
    @Override
    public void handle(INetworkContext context) {
        if (!context.isServerSide()) {
            return;
        }
        var player = context.sender();
        if (player == null) {
            return;
        }
        var unitOpt = ConfigManager.get(configId);
        if (unitOpt.isEmpty()) {
            OELib.LOGGER.warn("Config {} not found for update request", configId);
            return;
        }
        ConfigIOUtil.applyUpdate(unitOpt.get(), player, payload, format, save);
    }
}
