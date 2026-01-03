package cc.sighs.oelib.neoforge.platform;

import cc.sighs.oelib.platform.IPlatform;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

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
}