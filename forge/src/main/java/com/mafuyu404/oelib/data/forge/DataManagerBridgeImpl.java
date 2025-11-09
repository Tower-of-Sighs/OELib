package com.mafuyu404.oelib.data.forge;

import com.mafuyu404.oelib.OELib;
import com.mafuyu404.oelib.api.data.DataValidator;
import com.mafuyu404.oelib.forge.data.DataManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = OELib.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DataManagerBridgeImpl {
    private static List<Class<?>> sortedTypes = Collections.emptyList();

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

    public static void attachReloadListenersSorted(List<Class<?>> types) {
        sortedTypes = new ArrayList<>(types);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        for (Class<?> dataClass : sortedTypes) {
            var manager = DataManager.get(dataClass);
            if (manager != null) {
                event.addListener(manager);
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