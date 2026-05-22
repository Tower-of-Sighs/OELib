package cc.sighs.oelib.config.api;

import net.minecraft.server.level.ServerPlayer;

/**
 * Per-config permission checker used for server-owned configurations.
 * <p>
 * Determines whether a given player is permitted to apply client-originated
 * updates to a server-side configuration. If no checker is provided at
 * registration time, all client updates are rejected by default.
 * </p>
 */
@FunctionalInterface
public interface IConfigPermissionChecker {
    /**
     * Returns whether the player is permitted to update the target configuration.
     *
     * @param player server player attempting the update
     * @return true if the update is allowed, false otherwise
     */
    boolean canUpdate(ServerPlayer player);
}

