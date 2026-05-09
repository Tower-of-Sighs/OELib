package cc.sighs.oelib.config.jmh;

import cc.sighs.oelib.platform.IPlatform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public final class JmhPlatform implements IPlatform {
    private static final Path CONFIG_PATH = Path.of("build", "jmh-config").toAbsolutePath().normalize();

    @Override
    public Path getConfigPath() {
        return CONFIG_PATH;
    }

    @Override
    public boolean isDevelopmentEnv() {
        return true;
    }

    @Override
    public boolean isClient() {
        return false;
    }

    @Override
    public boolean isServer() {
        return true;
    }

    @Override
    public boolean isFabric() {
        return false;
    }

    @Override
    public boolean isNeoForge() {
        return false;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return false;
    }

    @Override
    public Collection<ServerPlayer> getAllPlayers(MinecraftServer server) {
        return List.of();
    }

    @Override
    public MinecraftServer getCurrentServer() {
        return null;
    }
}
