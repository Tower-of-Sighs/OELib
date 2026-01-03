package cc.sighs.oelib.network.api;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.network.spi.INetworkAutoRegistration;

import java.util.LinkedHashSet;
import java.util.ServiceLoader;
import java.util.Set;

public final class NetworkAutoRegistration {

    private static final Set<String> BASE_PACKAGES = new LinkedHashSet<>();

    static {
        BASE_PACKAGES.add("cc.sighs.oelib.config.net");
        BASE_PACKAGES.add("cc.sighs.oelib.data.net");
        BASE_PACKAGES.add("cc.sighs.oelib.dev.example.net");
    }

    private NetworkAutoRegistration() {
    }

    public static void registerBasePackage(String basePackage) {
        if (basePackage == null || basePackage.isEmpty()) {
            return;
        }
        BASE_PACKAGES.add(basePackage);
    }

    public static Set<Class<? extends INetworkPacket<?>>> findAllAnnotatedPackets() {
        Set<Class<? extends INetworkPacket<?>>> result = new LinkedHashSet<>();
        ServiceLoader<INetworkAutoRegistration> loader = ServiceLoader.load(INetworkAutoRegistration.class);
        for (INetworkAutoRegistration impl : loader) {
            try {
                result.addAll(impl.findAnnotatedPackets(Set.copyOf(BASE_PACKAGES)));
            } catch (Throwable t) {
                OELib.LOGGER.error("Network auto registration provider {} failed", impl.getClass().getName(), t);
            }
        }
        return result;
    }

    public static int getChunkThreshold(Class<?> clazz) {
        if (clazz.isAnnotationPresent(NetworkPacket.class)) {
            return clazz.getAnnotation(NetworkPacket.class).chunkThreshold();
        }
        return 0;
    }

}
