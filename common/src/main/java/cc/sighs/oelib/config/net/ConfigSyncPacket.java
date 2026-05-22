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

/**
 * A server-to-client packet carrying a full configuration payload for
 * synchronization.
 *
 * <p>On receipt, the client applies the payload via
 * {@link ConfigManager#applyRemoteUpdate(ResourceLocation, String, ConfigStorageFormat)}
 * within a server-update context and records the known server state through
 * {@link ServerConfigManager#recordClientKnownServer(ResourceLocation, String)}.
 *
 * @param configId the configuration id
 * @param payload  the encoded configuration content
 * @param format   the serialization format of the payload
 */
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
