package cc.sighs.oelib.data;

import cc.sighs.oelib.data.OELibData;
import cc.sighs.oelib.data.api.DataDriven;
import cc.sighs.oelib.data.api.DataValidator;
import cc.sighs.oelib.data.mvel.ExpressionEngine;
import cc.sighs.oelib.data.mvel.FunctionUsageAnalyzer;
import cc.sighs.oelib.data.util.ClassUtil;
import cc.sighs.oelib.data.util.CodecUtils;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class DataRegistry {
    private static final Map<Class<?>, Integer> registeredTypes = new ConcurrentHashMap<>();
    private static final Map<Class<?>, FunctionUsageAnalyzer.DataExpressionExtractor<?>> extractors = new ConcurrentHashMap<>();
    private static boolean initialized = false;
    private static boolean expressionEngineInitialized = false;

    private DataRegistry() {
    }

    public static <T> void register(Class<T> dataClass) {
        registerInternal(dataClass, null, (String[]) null);
    }

    public static <T> void register(Class<T> dataClass, Codec<T> codec) {
        registerInternal(dataClass, codec, (String[]) null);
    }

    @ApiStatus.Internal
    public static <T> void registerWithNamespaces(Class<T> dataClass, String... namespaces) {
        registerInternal(dataClass, null, namespaces);
    }

    @ApiStatus.Internal
    public static <T> void registerWithNamespaces(Class<T> dataClass, Codec<T> codec, String... namespaces) {
        registerInternal(dataClass, codec, namespaces);
    }

    @ApiStatus.Internal
    public static <T> void registerNamespaceValidator(
            Class<T> dataClass,
            String namespace,
            Class<? extends DataValidator<?>> validatorClass) {

        if (!isRegistered(dataClass)) {
            throw new IllegalStateException(
                    "Cannot register namespace validator for unregistered data class: " + dataClass.getSimpleName() +
                            ". Please register the data type first using DataRegistry.register(...)."
            );
        }

        DataManager.registerNamespaceValidator(dataClass, namespace, validatorClass);
        OELibData.LOGGER.debug(
                "Registered namespace validator {} for {} in namespace '{}'",
                validatorClass.getSimpleName(), dataClass.getSimpleName(), namespace
        );
    }

    private static <T> void registerInternal(Class<T> dataClass, @Nullable Codec<T> codec, @Nullable String... namespaces) {
        if (!dataClass.isAnnotationPresent(DataDriven.class)) {
            throw new IllegalArgumentException("Class " + dataClass.getSimpleName() + " must be annotated with @DataDriven");
        }

        DataDriven annotation = dataClass.getAnnotation(DataDriven.class);
        int priority = annotation.priority();

        registeredTypes.put(dataClass, priority);

        String codecInfo = "none";
        if (codec != null) {
            CodecUtils.registerCodec(dataClass, codec);
            codecInfo = codec.getClass().getSimpleName();
        }

        DataManager.register(dataClass);

        List<String> registeredNamespaces = new ArrayList<>();
        if (namespaces != null) {
            for (String ns : namespaces) {
                if (ns != null && !ns.isBlank()) {
                    DataManager.registerNamespace(dataClass, ns);
                    registeredNamespaces.add(ns);
                }
            }
        }

        if (!registeredNamespaces.isEmpty()) {
            OELibData.LOGGER.debug(
                    "Registered data-driven type: {} | Priority: {} | Codec: {} | Namespaces: {}",
                    dataClass.getSimpleName(), priority, codecInfo, registeredNamespaces
            );
        } else {
            OELibData.LOGGER.debug(
                    "Registered data-driven type: {} | Priority: {} | Codec: {}",
                    dataClass.getSimpleName(), priority, codecInfo
            );
        }
    }


    public static <T> void registerExtractor(Class<T> dataClass, FunctionUsageAnalyzer.DataExpressionExtractor<T> extractor) {
        extractors.put(dataClass, extractor);
        OELibData.LOGGER.debug("Registered expression extractor for: {}", dataClass.getSimpleName());
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        OELibData.LOGGER.info("Data registry initialized with {} registered types", registeredTypes.size());
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public static void initializeExpressionEngine() {
        if (!ClassUtil.isClassPresent("org.mvel2.MVEL")) {
            OELibData.LOGGER.warn("MVEL2 not found in classpath. Expression engine will be disabled.");
            expressionEngineInitialized = false;
            return;
        }
        ExpressionEngine.clear();
        expressionEngineInitialized = false;

        Set<String> allUsedFunctions = new HashSet<>(FunctionUsageAnalyzer.getCoreRequiredFunctions());
        boolean hasAnyData = false;

        List<Class<?>> sortedTypes = getRegisteredTypesByPriority();
        OELibData.LOGGER.debug("Processing data types in priority order: {}",
                sortedTypes.stream()
                        .map(clazz -> clazz.getSimpleName() + "(priority:" + registeredTypes.get(clazz) + ")")
                        .collect(Collectors.joining(", ")));

        for (Class<?> dataClass : sortedTypes) {
            FunctionUsageAnalyzer.DataExpressionExtractor<Object> extractor =
                    (FunctionUsageAnalyzer.DataExpressionExtractor<Object>) extractors.get(dataClass);

            if (extractor != null) {
                Map<ResourceLocation, Object> data =
                        (Map<ResourceLocation, Object>) DataManager.getAllData(dataClass);
                if (data != null && !data.isEmpty()) {
                    hasAnyData = true;
                    Set<String> usedFunctions = FunctionUsageAnalyzer.analyzeUsedFunctions(data, extractor);
                    allUsedFunctions.addAll(usedFunctions);
                    OELibData.LOGGER.debug("Found {} functions in {} (priority:{}): {}",
                            usedFunctions.size(), dataClass.getSimpleName(), registeredTypes.get(dataClass), usedFunctions);
                }
            }
        }

        if (!hasAnyData) {
            OELibData.LOGGER.info("No datapack data loaded yet; deferring expression engine initialization");
            return;
        }

        OELibData.LOGGER.info("Smart registration: found {} total used functions: {}", allUsedFunctions.size(), allUsedFunctions);

        ExpressionEngine.initialize(allUsedFunctions);

        expressionEngineInitialized = true;
    }

    public static Set<Class<?>> getRegisteredTypes() {
        return Set.copyOf(registeredTypes.keySet());
    }

    public static List<Class<?>> getRegisteredTypesByPriority() {
        return registeredTypes.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public static Integer getPriority(Class<?> dataClass) {
        return registeredTypes.get(dataClass);
    }

    public static boolean isRegistered(Class<?> dataClass) {
        return registeredTypes.containsKey(dataClass);
    }

    public static boolean isExpressionEngineInitialized() {
        return expressionEngineInitialized;
    }

    public static void resetExpressionEngine() {
        expressionEngineInitialized = false;
        ExpressionEngine.clear();
    }
}