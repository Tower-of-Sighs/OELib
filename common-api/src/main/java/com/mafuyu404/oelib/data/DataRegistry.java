package com.mafuyu404.oelib.data;

import com.mafuyu404.oelib.OELib;
import com.mafuyu404.oelib.api.data.DataDriven;
import com.mafuyu404.oelib.api.data.DataValidator;
import com.mafuyu404.oelib.data.mvel.ExpressionEngine;
import com.mafuyu404.oelib.data.mvel.FunctionUsageAnalyzer;
import com.mafuyu404.oelib.util.CodecUtils;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class DataRegistry {
    private static final Map<Class<?>, Integer> registeredTypes = new ConcurrentHashMap<>();
    private static final Map<Class<?>, FunctionUsageAnalyzer.DataExpressionExtractor<?>> extractors = new ConcurrentHashMap<>();
    private static boolean initialized = false;
    private static boolean expressionEngineInitialized = false;

    private DataRegistry() {}

    public static <T> void register(Class<T> dataClass) {
        registerInternal(dataClass, null, (String[]) null);
    }

    public static <T> void register(Class<T> dataClass, Codec<T> codec) {
        registerInternal(dataClass, codec, (String[]) null);
    }

    public static <T> void registerWithNamespaces(Class<T> dataClass, String... namespaces) {
        registerInternal(dataClass, null, namespaces);
    }

    public static <T> void registerWithNamespaces(Class<T> dataClass, Codec<T> codec, String... namespaces) {
        registerInternal(dataClass, codec, namespaces);
    }

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

        DataManagerBridge.registerNamespaceValidator(dataClass, namespace, validatorClass);
        OELib.LOGGER.debug(
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

        DataManagerBridge.register(dataClass);

        List<String> registeredNamespaces = new ArrayList<>();
        if (namespaces != null) {
            for (String ns : namespaces) {
                if (ns != null && !ns.isBlank()) {
                    DataManagerBridge.registerNamespace(dataClass, ns);
                    registeredNamespaces.add(ns);
                }
            }
        }

        if (!registeredNamespaces.isEmpty()) {
            OELib.LOGGER.debug(
                    "Registered data-driven type: {} | Priority: {} | Codec: {} | Namespaces: {}",
                    dataClass.getSimpleName(), priority, codecInfo, registeredNamespaces
            );
        } else {
            OELib.LOGGER.debug(
                    "Registered data-driven type: {} | Priority: {} | Codec: {}",
                    dataClass.getSimpleName(), priority, codecInfo
            );
        }
    }


    public static <T> void registerExtractor(Class<T> dataClass, FunctionUsageAnalyzer.DataExpressionExtractor<T> extractor) {
        extractors.put(dataClass, extractor);
        OELib.LOGGER.debug("Registered expression extractor for: {}", dataClass.getSimpleName());
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        OELib.LOGGER.info("Data registry initialized with {} registered types", registeredTypes.size());
    }

    @SuppressWarnings("unchecked")
    public static void initializeExpressionEngine() {
        if (expressionEngineInitialized) return;

        Set<String> allUsedFunctions = new HashSet<>(FunctionUsageAnalyzer.getCoreRequiredFunctions());

        List<Class<?>> sortedTypes = getRegisteredTypesByPriority();
        OELib.LOGGER.debug("Processing data types in priority order: {}",
                sortedTypes.stream()
                        .map(clazz -> clazz.getSimpleName() + "(priority:" + registeredTypes.get(clazz) + ")")
                        .collect(Collectors.joining(", ")));

        for (Class<?> dataClass : sortedTypes) {
            FunctionUsageAnalyzer.DataExpressionExtractor<Object> extractor =
                    (FunctionUsageAnalyzer.DataExpressionExtractor<Object>) extractors.get(dataClass);

            if (extractor != null) {
                Map<ResourceLocation, Object> data = (Map<ResourceLocation, Object>) DataManagerBridge.getAllData(dataClass);
                if (data != null) {
                    Set<String> usedFunctions = FunctionUsageAnalyzer.analyzeUsedFunctions(data, extractor);
                    allUsedFunctions.addAll(usedFunctions);
                    OELib.LOGGER.debug("Found {} functions in {} (priority:{}): {}",
                            usedFunctions.size(), dataClass.getSimpleName(), registeredTypes.get(dataClass), usedFunctions);
                }
            }
        }

        OELib.LOGGER.info("Smart registration: found {} total used functions: {}", allUsedFunctions.size(), allUsedFunctions);
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

    public static void attachReloadListeners() {
        DataManagerBridge.attachReloadListenersSorted(getRegisteredTypesByPriority());
    }
}