package cc.sighs.oelib.config.api;

import net.minecraft.server.level.ServerPlayer;

/**
 * Determines whether a player is permitted to submit updates to a
 * server-side configuration from the client.
 *
 * <p>If no checker is provided when a server configuration is registered,
 * all client-originated updates are rejected. A checker that returns
 * {@code true} for a player grants that player the ability to modify
 * the configuration through the in-game GUI.
 */
@FunctionalInterface
public interface IConfigPermissionChecker {
    /**
     * Returns whether the given player is permitted to update the
     * associated server configuration.
     *
     * @param player the server player attempting the update
     * @return {@code true} if the update is permitted
     */
    boolean canUpdate(ServerPlayer player);
}
