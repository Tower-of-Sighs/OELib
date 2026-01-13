package cc.sighs.oelib.platform;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.Collection;
import java.util.ServiceLoader;

public class Platform {
    private static final IPlatform INSTANCE = ServiceLoader.load(IPlatform.class)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No IPlatform implementation found"));

    public static Path getConfigPath() {
        return INSTANCE.getConfigPath();
    }

    public static boolean isDevelopmentEnv() {
        return INSTANCE.isDevelopmentEnv();
    }

    public static boolean isClient() {
        return INSTANCE.isClient();
    }

    public static boolean isServer() {
        return INSTANCE.isServer();
    }

    public static boolean isFabric() {
        return INSTANCE.isFabric();
    }

    public static boolean isNeoForge() {
        return INSTANCE.isNeoForge();
    }

    public static boolean isModLoaded(String modId) {
        return INSTANCE.isModLoaded(modId);
    }

    public static Collection<ServerPlayer> getAllPlayers(MinecraftServer server) {
        return INSTANCE.getAllPlayers(server);
    }
}