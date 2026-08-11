package cc.sighs.oelib.event;

import cc.sighs.oelib.misc.util.AnnotationScanUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class EventAutoRegistration {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Set<Class<?>> REGISTERED_LISTENER_CLASSES = ConcurrentHashMap.newKeySet();
    private static final Object REGISTRATION_LOCK = new Object();

    private EventAutoRegistration() {
    }

    /**
     * @deprecated OELib now scans each mod file once and queries its global annotation index.
     * Package registration is no longer required and this method has no effect.
     */
    @Deprecated(forRemoval = true)
    public static void registerBasePackage(String basePackage) {
    }

    public static int registerAllListeners() {
        synchronized (REGISTRATION_LOCK) {
            Set<Class<?>> classes = AnnotationScanUtil.findAnnotatedClasses(
                    Subscribe.class,
                    AnnotationScanUtil.nonAbstractNonInterface()
            );

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
                    if (subscribe == null || !void.class.equals(method.getReturnType())) {
                        continue;
                    }
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length != 1 || !Event.class.isAssignableFrom(parameterTypes[0])) {
                        continue;
                    }

                    boolean isStatic = Modifier.isStatic(method.getModifiers());
                    if (isStatic) {
                        validStaticMethods++;
                    } else {
                        validInstanceMethods++;
                    }

                    if (debugDetails != null) {
                        if (!debugDetails.isEmpty()) {
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
                        Constructor<?> constructor = clazz.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        EventBus.register(constructor.newInstance());
                    } else {
                        EventBus.register(clazz);
                    }
                    registeredThisClass = true;
                } catch (Throwable instanceError) {
                    try {
                        if (validStaticMethods > 0) {
                            EventBus.register(clazz);
                            registeredThisClass = true;
                            LOGGER.warn(
                                    "[EventAutoReg] Listener {} has instance handlers but no usable no-arg constructor; registered static handlers only.",
                                    clazz.getName()
                            );
                        } else {
                            LOGGER.error("[EventAutoReg] Failed to register listener class: {}", clazz.getName(), instanceError);
                        }
                    } catch (Throwable fallbackError) {
                        LOGGER.error("[EventAutoReg] Failed to register listener class: {}", clazz.getName(), fallbackError);
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
                    LOGGER.debug(
                            "[EventAutoReg] Registered listener {} (methods: {}) => {}",
                            clazz.getName(), validMethods, debugDetails
                    );
                }
            }

            LOGGER.info(
                    "[EventAutoReg] Candidates: {} | registered: {} | skipped: {} | failed: {} | methods: {}",
                    classes.size(), registeredClasses, skippedAlreadyRegistered, failed, handlerMethodCount
            );
            return registeredClasses;
        }
    }
}
