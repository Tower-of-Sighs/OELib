package cc.sighs.oelib.platform;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.Collection;

public interface IPlatform {
    Path getConfigPath();

    boolean isDevelopmentEnv();

    boolean isClient();

    boolean isServer();

    boolean isFabric();

    boolean isNeoForge();

    boolean isModLoaded(String modId);

    Collection<ServerPlayer> getAllPlayers(MinecraftServer server);

    MinecraftServer getCurrentServer();
}
