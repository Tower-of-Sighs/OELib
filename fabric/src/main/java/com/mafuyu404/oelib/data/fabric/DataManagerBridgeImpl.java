package com.mafuyu404.oelib.data.fabric;

import com.mafuyu404.oelib.api.data.DataValidator;
import com.mafuyu404.oelib.fabric.data.DataManager;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DataManagerBridgeImpl {
    public static <T> void register(Class<T> dataClass) {
        DataManager.register(dataClass);
    }

    public static <T> void registerNamespace(Class<T> dataClass, String namespace) {
        DataManager.registerNamespace(dataClass, namespace);
    }

    public static <T> void registerNamespaceValidator(Class<T> dataClass, String namespace,
                                                      Class<? extends DataValidator<?>> validatorClass) {
        DataManager.registerNamespaceValidator(dataClass, namespace, validatorClass);
    }

    public static <T> Map<ResourceLocation, T> getAllData(Class<T> dataClass) {
        var manager = DataManager.get(dataClass);
        return manager != null ? manager.getAllData() : Map.of();
    }

    public static void attachReloadListenersSorted(List<Class<?>> sortedTypes) {
        var helper = ResourceManagerHelper.get(PackType.SERVER_DATA);
        for (Class<?> dataClass : sortedTypes) {
            var manager = DataManager.get(dataClass);
            if (manager != null) {
                helper.registerReloadListener(manager);
            }
        }
    }

    public static <T> T getData(Class<T> dataClass, ResourceLocation location) {
        var manager = DataManager.get(dataClass);
        return manager != null ? manager.getData(location) : null;
    }

    public static <T> List<T> getDataList(Class<T> dataClass) {
        var manager = DataManager.get(dataClass);
        return manager != null ? manager.getDataList() : List.of();
    }

    public static <T> Set<T> getCachedData(Class<T> dataClass, String cacheKey) {
        var manager = DataManager.get(dataClass);
        return manager != null ? manager.getCachedData(cacheKey) : Set.of();
    }

    public static <T> void addToCache(Class<T> dataClass, String cacheKey, T data) {
        var manager = DataManager.get(dataClass);
        if (manager != null) {
            manager.addToCache(cacheKey, data);
        }
    }

    public static <T> void clearCache(Class<T> dataClass) {
        var manager = DataManager.get(dataClass);
        if (manager != null) {
            manager.clearCache();
        }
    }

    public static <T> void updateClientData(Class<T> dataClass, Map<ResourceLocation, T> data) {
        var manager = DataManager.get(dataClass);
        if (manager != null) {
            manager.updateClientData(data);
        }
    }
}