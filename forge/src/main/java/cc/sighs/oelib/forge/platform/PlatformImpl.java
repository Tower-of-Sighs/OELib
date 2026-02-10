package cc.sighs.oelib.forge.platform;

import cc.sighs.oelib.platform.IPlatform;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;

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

        if (server.getPlayerList() != null) {
            return Collections.unmodifiableCollection(server.getPlayerList().getPlayers());
        }

        return Collections.emptyList();
    }

    @Override
    public MinecraftServer getCurrentServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }
}