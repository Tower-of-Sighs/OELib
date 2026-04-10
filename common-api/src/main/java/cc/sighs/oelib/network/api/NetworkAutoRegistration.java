package cc.sighs.oelib.network.api;

import cc.sighs.oelib.util.AnnotationScanUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class NetworkAutoRegistration {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Set<String> BASE_PACKAGES = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final Set<String> SCANNED_PACKAGES = new HashSet<>();
    private static final Set<Class<? extends INetworkPacket<?>>> REGISTERED_PACKET_CLASSES = ConcurrentHashMap.newKeySet();
    private static final Object REGISTRATION_LOCK = new Object();
    private static volatile boolean registrationStarted = false;

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

        boolean added = BASE_PACKAGES.add(basePackage);
        if (!added) {
            return;
        }

        if (registrationStarted) {
            LOGGER.info("[NetworkAutoReg] Late base package registered: {}. Scanning now.", basePackage);
            scanAndCollect(Set.of(basePackage));
            return;
        }

        LOGGER.debug("[NetworkAutoReg] Registered base package: {}", basePackage);
    }

    public static Set<Class<? extends INetworkPacket<?>>> findAllAnnotatedPackets() {
        registrationStarted = true;

        Set<String> packagesSnapshot = Set.copyOf(BASE_PACKAGES);
        if (packagesSnapshot.isEmpty()) {
            LOGGER.debug("[NetworkAutoReg] No base packages registered; skipping scan.");
            return Set.of();
        }

        Set<String> unscanned = new LinkedHashSet<>();
        synchronized (REGISTRATION_LOCK) {
            for (String pkg : packagesSnapshot) {
                if (!SCANNED_PACKAGES.contains(pkg)) {
                    unscanned.add(pkg);
                }
            }
        }

        if (!unscanned.isEmpty()) {
            LOGGER.info("[NetworkAutoReg] Scanning {} package(s) for packets...", unscanned.size());
            scanAndCollect(unscanned);
        } else {
            LOGGER.debug("[NetworkAutoReg] All base packages already scanned; skipping scan.");
        }

        return Set.copyOf(REGISTERED_PACKET_CLASSES);
    }

    private static void scanAndCollect(Set<String> packagesToScan) {
        synchronized (REGISTRATION_LOCK) {

            Set<String> newPackages = new LinkedHashSet<>();
            for (String pkg : packagesToScan) {
                if (pkg != null && !pkg.isEmpty() && !SCANNED_PACKAGES.contains(pkg)) {
                    newPackages.add(pkg);
                }
            }

            if (newPackages.isEmpty()) {
                return;
            }

            Predicate<Class<?>> filter = AnnotationScanUtil.nonAbstractNonInterface()
                    .and(INetworkPacket.class::isAssignableFrom)
                    .and(CustomPacketPayload.class::isAssignableFrom);

            Set<Class<?>> classes;
            try {
                classes = AnnotationScanUtil.findAnnotatedClasses(
                        NetworkPacket.class,
                        Set.copyOf(newPackages),
                        filter
                );
            } catch (Throwable t) {
                LOGGER.error("[NetworkAutoReg] Failed to scan packages: {}", newPackages, t);
                return;
            }

            SCANNED_PACKAGES.addAll(newPackages);

            int added = 0;
            int skipped = 0;

            for (Class<?> clazz : classes) {
                @SuppressWarnings("unchecked")
                Class<? extends INetworkPacket<?>> packetClass =
                        (Class<? extends INetworkPacket<?>>) clazz;

                if (!REGISTERED_PACKET_CLASSES.add(packetClass)) {
                    skipped++;
                    continue;
                }

                added++;

                LOGGER.debug("[NetworkAutoReg] Found packet: {} (chunkThreshold={})",
                        packetClass.getName(),
                        getChunkThreshold(packetClass));
            }

            LOGGER.info("[NetworkAutoReg] Scan packages: {} | found: {} | added: {} | skipped: {}",
                    newPackages, classes.size(), added, skipped);
        }
    }

    public static int getChunkThreshold(Class<?> clazz) {
        NetworkPacket annotation = clazz.getAnnotation(NetworkPacket.class);
        return annotation != null ? annotation.chunkThreshold() : 0;
    }
}