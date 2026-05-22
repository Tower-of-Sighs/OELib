package cc.sighs.oelib.config.model;

/**
 * Designates whether a configuration resides on the logical client or
 * logical server.
 *
 * <p>Client-side configurations are stored per-client and reloaded with
 * resource packs. Server-side configurations reside in the server's
 * config directory and can be synchronized to connected clients.
 */
public enum ConfigSide {
    /** Per-client configuration, stored in the client config directory. */
    CLIENT,
    /** Per-world/server configuration, stored in the server config directory. */
    SERVER
}
