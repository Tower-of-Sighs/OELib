package cc.sighs.oelib.misc.neoforge.platform;

import cc.sighs.oelib.platform.IPlatform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public class PlatformImpl implements IPlatform {
    @Override
    public Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isDevelopmentEnv() {
        return !FMLEnvironment.production;
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    @Override
    public boolean isServer() {
        return FMLEnvironment.dist == Dist.DEDICATED_SERVER;
    }

    @Override
    public boolean isFabric() {
        return false;
    }

    @Override
    public boolean isNeoForge() {
        return true;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Collection<ServerPlayer> getAllPlayers(MinecraftServer server) {
        Objects.requireNonNull(server, "The server cannot be null");

        return Collections.unmodifiableCollection(server.getPlayerList().getPlayers());
    }

    @Override
    public MinecraftServer getCurrentServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }
}