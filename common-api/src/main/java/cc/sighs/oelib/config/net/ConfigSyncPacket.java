package cc.sighs.oelib.config.net;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.config.ConfigManager;
import cc.sighs.oelib.config.ServerConfigManager;
import cc.sighs.oelib.config.model.ConfigStorageFormat;
import cc.sighs.oelib.network.api.INetworkContext;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.api.Side;
import net.minecraft.resources.ResourceLocation;

@NetworkPacket(modId = OELib.MODID, id = "config_sync", side = Side.CLIENT)
public record ConfigSyncPacket(
        ResourceLocation configId,
        String payload,
        ConfigStorageFormat format
) implements INetworkPacket<ConfigSyncPacket> {
    @Override
    public void handle(INetworkContext context) {
        context.enqueueWork(() -> {
            ConfigManager.runWithServerUpdate(() -> ConfigManager.applyRemoteUpdate(configId, payload, format));
            ServerConfigManager.recordClientKnownServer(configId, payload);
        });
    }
}
