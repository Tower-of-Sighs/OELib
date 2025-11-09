package com.mafuyu404.oelib.data;

import com.mafuyu404.oelib.api.data.DataValidator;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DataManagerBridge {
    private DataManagerBridge() {}

    @ExpectPlatform
    public static <T> void register(Class<T> dataClass) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T> void registerNamespace(Class<T> dataClass, String namespace) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T> void registerNamespaceValidator(Class<T> dataClass, String namespace,
                                                      Class<? extends DataValidator<?>> validatorClass) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T> Map<ResourceLocation, T> getAllData(Class<T> dataClass) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void attachReloadListenersSorted(List<Class<?>> sortedTypes) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T> T getData(Class<T> dataClass, ResourceLocation location) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T> List<T> getDataList(Class<T> dataClass) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T> Set<T> getCachedData(Class<T> dataClass, String cacheKey) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T> void addToCache(Class<T> dataClass, String cacheKey, T data) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T> void clearCache(Class<T> dataClass) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T> void updateClientData(Class<T> dataClass, Map<ResourceLocation, T> data) {
        throw new AssertionError();
    }
}