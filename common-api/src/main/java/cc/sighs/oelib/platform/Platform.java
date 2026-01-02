package cc.sighs.oelib.platform;

import java.nio.file.Path;
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
}