package cc.sighs.oelib.neoforge.platform;

import cc.sighs.oelib.platform.IPlatform;
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
}