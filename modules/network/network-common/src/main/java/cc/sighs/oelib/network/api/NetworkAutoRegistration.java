package cc.sighs.oelib.network.api;

import cc.sighs.oelib.misc.util.AnnotationScanUtil;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class NetworkAutoRegistration {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Set<Class<? extends INetworkPacket<?>>> REGISTERED_PACKET_CLASSES = ConcurrentHashMap.newKeySet();
    private static final Object REGISTRATION_LOCK = new Object();
    private static volatile boolean collected;

    private NetworkAutoRegistration() {
    }

    /**
     * @deprecated OELib now scans each mod file once and queries its global annotation index.
     * Package registration is no longer required and this method has no effect.
     */
    @Deprecated(forRemoval = true)
    public static void registerBasePackage(String basePackage) {
    }

    public static Set<Class<? extends INetworkPacket<?>>> findAllAnnotatedPackets() {
        if (!collected) {
            synchronized (REGISTRATION_LOCK) {
                if (!collected) {
                    collectPackets();
                    collected = true;
                }
            }
        }
        return Set.copyOf(REGISTERED_PACKET_CLASSES);
    }

    private static void collectPackets() {
        Predicate<Class<?>> filter = AnnotationScanUtil.nonAbstractNonInterface()
                .and(INetworkPacket.class::isAssignableFrom)
                .and(CustomPacketPayload.class::isAssignableFrom);
        Set<Class<?>> classes = AnnotationScanUtil.findAnnotatedClasses(NetworkPacket.class, filter);

        int added = 0;
        int skipped = 0;
        for (Class<?> clazz : classes) {
            @SuppressWarnings("unchecked")
            Class<? extends INetworkPacket<?>> packetClass = (Class<? extends INetworkPacket<?>>) clazz;
            if (!REGISTERED_PACKET_CLASSES.add(packetClass)) {
                skipped++;
                continue;
            }
            added++;
            LOGGER.debug(
                    "[NetworkAutoReg] Found packet: {} (chunkThreshold={})",
                    packetClass.getName(), getChunkThreshold(packetClass)
            );
        }

        LOGGER.info(
                "[NetworkAutoReg] Found: {} | added: {} | skipped: {}",
                classes.size(), added, skipped
        );
    }

    public static int getChunkThreshold(Class<?> clazz) {
        NetworkPacket annotation = clazz.getAnnotation(NetworkPacket.class);
        if (annotation == null) {
            return 0;
        }

        int threshold = annotation.chunkThreshold();
        if (threshold == 0) {
            return 0;
        }
        if (threshold < NetworkPacket.MIN_CHUNK_SIZE) {
            throw new IllegalArgumentException("Packet " + clazz.getName() + " has chunkThreshold=" + threshold
                    + ", but positive chunk thresholds must be at least " + NetworkPacket.MIN_CHUNK_SIZE + " bytes");
        }

        int maximum = annotation.side() == Side.CLIENT
                ? NetworkPacket.MAX_CLIENTBOUND_CHUNK_SIZE
                : NetworkPacket.MAX_SERVERBOUND_CHUNK_SIZE;
        if (threshold > maximum) {
            throw new IllegalArgumentException("Packet " + clazz.getName() + " has chunkThreshold=" + threshold
                    + ", exceeding the safe maximum of " + maximum + " bytes for side " + annotation.side());
        }
        return threshold;
    }
}
