package cc.sighs.oelib.platform;

import java.nio.file.Path;

public interface IPlatform {
    Path getConfigPath();

    boolean isDevelopmentEnv();

    boolean isClient();

    boolean isServer();

    boolean isFabric();

    boolean isNeoForge();
}
