package com.sighs.oelib.fabric.platform;

import com.sighs.oelib.platform.IPlatform;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class PlatformImpl implements IPlatform {
    @Override
    public Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isDevelopmentEnv() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
