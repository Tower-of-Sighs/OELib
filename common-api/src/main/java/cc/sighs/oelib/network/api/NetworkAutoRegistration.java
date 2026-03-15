package cc.sighs.oelib.network.api;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.util.AnnotationScanUtil;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

public final class NetworkAutoRegistration {

    private static final Set<String> BASE_PACKAGES = new LinkedHashSet<>();

    static {
        BASE_PACKAGES.add("cc.sighs.oelib.config.net");
        BASE_PACKAGES.add("cc.sighs.oelib.data.net");
        BASE_PACKAGES.add("cc.sighs.oelib.dev.example.net");
        BASE_PACKAGES.add("cc.sighs.oelib.network.chunk");
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
        OELib.LOGGER.debug("Scanning for annotated packets in base packages: {}", BASE_PACKAGES);
        Predicate<Class<?>> filter = AnnotationScanUtil.nonAbstractNonInterface()
                .and(INetworkPacket.class::isAssignableFrom)
                .and(CustomPacketPayload.class::isAssignableFrom);
        Set<Class<?>> classes = AnnotationScanUtil.findAnnotatedClasses(NetworkPacket.class, Set.copyOf(BASE_PACKAGES), filter);
        for (Class<?> clazz : classes) {
            @SuppressWarnings("unchecked")
            Class<? extends INetworkPacket<?>> packetClass = (Class<? extends INetworkPacket<?>>) clazz;
            result.add(packetClass);
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
