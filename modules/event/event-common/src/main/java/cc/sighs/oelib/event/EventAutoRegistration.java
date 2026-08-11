package cc.sighs.oelib.event;

import cc.sighs.oelib.misc.util.AnnotationScanUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class EventAutoRegistration {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Set<String> BASE_PACKAGES = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final Set<String> SCANNED_PACKAGES = new HashSet<>();
    private static final Set<Class<?>> REGISTERED_LISTENER_CLASSES = ConcurrentHashMap.newKeySet();
    private static final Object REGISTRATION_LOCK = new Object();
    private static volatile boolean registrationStarted = false;

    private EventAutoRegistration() {
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
            LOGGER.info("[EventAutoReg] Late base package registered: {}. Scanning now.", basePackage);
            scanAndRegister(Set.of(basePackage));
            return;
        }
        LOGGER.debug("[EventAutoReg] Registered base package: {}", basePackage);
    }

    public static int registerAllListeners() {
        registrationStarted = true;

        Set<String> packagesSnapshot = Set.copyOf(BASE_PACKAGES);
        if (packagesSnapshot.isEmpty()) {
            LOGGER.debug("[EventAutoReg] No base packages registered; skipping scan.");
            return 0;
        }

        Set<String> unscanned = new LinkedHashSet<>();
        synchronized (REGISTRATION_LOCK) {
            for (String pkg : packagesSnapshot) {
                if (!SCANNED_PACKAGES.contains(pkg)) {
                    unscanned.add(pkg);
                }
            }
        }
        if (unscanned.isEmpty()) {
            LOGGER.debug("[EventAutoReg] All base packages already scanned; skipping scan.");
            return 0;
        }

        LOGGER.info("[EventAutoReg] Scanning {} package(s) for event listeners...", unscanned.size());
        int registered = scanAndRegister(unscanned);
        LOGGER.info("[EventAutoReg] Registered {} listener class(es).", registered);
        return registered;
    }

    private static int scanAndRegister(Set<String> packagesToScan) {
        synchronized (REGISTRATION_LOCK) {
            Set<String> newPackages = new LinkedHashSet<>();
            for (String pkg : packagesToScan) {
                if (pkg != null && !pkg.isEmpty() && !SCANNED_PACKAGES.contains(pkg)) {
                    newPackages.add(pkg);
                }
            }
            if (newPackages.isEmpty()) {
                return 0;
            }

            Set<Class<?>> classes;
            try {
                classes = AnnotationScanUtil.findAnnotatedClasses(
                        Subscribe.class,
                        Set.copyOf(newPackages),
                        AnnotationScanUtil.nonAbstractNonInterface()
                );
            } catch (Throwable t) {
                LOGGER.error("[EventAutoReg] Failed to scan packages: {}", newPackages, t);
                return 0;
            }
            SCANNED_PACKAGES.addAll(newPackages);

            int registeredClasses = 0;
            int skippedAlreadyRegistered = 0;
            int failed = 0;
            int handlerMethodCount = 0;

            for (Class<?> clazz : classes) {
                int validStaticMethods = 0;
                int validInstanceMethods = 0;
                StringBuilder debugDetails = LOGGER.isDebugEnabled() ? new StringBuilder() : null;

                for (Method method : clazz.getDeclaredMethods()) {
                    Subscribe subscribe = method.getAnnotation(Subscribe.class);
                    if (subscribe == null) {
                        continue;
                    }
                    if (!void.class.equals(method.getReturnType())) {
                        continue;
                    }
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length != 1) {
                        continue;
                    }
                    if (!Event.class.isAssignableFrom(parameterTypes[0])) {
                        continue;
                    }

                    boolean isStatic = Modifier.isStatic(method.getModifiers());
                    if (isStatic) {
                        validStaticMethods++;
                    } else {
                        validInstanceMethods++;
                    }

                    if (debugDetails != null) {
                        if (debugDetails.length() != 0) {
                            debugDetails.append(", ");
                        }
                        debugDetails.append(isStatic ? "static " : "")
                                .append(method.getName())
                                .append("(")
                                .append(parameterTypes[0].getSimpleName())
                                .append(") [")
                                .append(subscribe.side())
                                .append("]");
                    }
                }

                int validMethods = validStaticMethods + validInstanceMethods;
                if (validMethods == 0) {
                    continue;
                }
                if (!REGISTERED_LISTENER_CLASSES.add(clazz)) {
                    skippedAlreadyRegistered++;
                    continue;
                }

                boolean registeredThisClass = false;
                try {
                    if (validInstanceMethods > 0) {
                        Constructor<?> ctor = clazz.getDeclaredConstructor();
                        ctor.setAccessible(true);
                        Object instance = ctor.newInstance();
                        EventBus.register(instance);
                        registeredThisClass = true;
                    } else {
                        EventBus.register(clazz);
                        registeredThisClass = true;
                    }
                } catch (Throwable instanceErr) {
                    try {
                        if (validStaticMethods > 0) {
                            EventBus.register(clazz);
                            registeredThisClass = true;
                            LOGGER.warn("[EventAutoReg] Listener {} has instance handlers but no usable no-arg constructor; registered static handlers only.",
                                    clazz.getName());
                        } else {
                            LOGGER.error("[EventAutoReg] Failed to register listener class: {}", clazz.getName(), instanceErr);
                        }
                    } catch (Throwable fallbackErr) {
                        LOGGER.error("[EventAutoReg] Failed to register listener class: {}", clazz.getName(), fallbackErr);
                    }
                }

                if (!registeredThisClass) {
                    REGISTERED_LISTENER_CLASSES.remove(clazz);
                    failed++;
                    continue;
                }

                registeredClasses++;
                handlerMethodCount += validMethods;
                if (debugDetails != null) {
                    LOGGER.debug("[EventAutoReg] Registered listener {} (methods: {}) => {}",
                            clazz.getName(), validMethods, debugDetails);
                }
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("[EventAutoReg] Scan packages: {} | candidates: {} | registered: {} | skipped: {} | failed: {} | methods: {}",
                        newPackages, classes.size(), registeredClasses, skippedAlreadyRegistered, failed, handlerMethodCount);
            }

            return registeredClasses;
        }
    }
}
