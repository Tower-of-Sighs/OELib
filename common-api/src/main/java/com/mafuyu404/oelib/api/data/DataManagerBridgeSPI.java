package com.mafuyu404.oelib.api.data;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DataManagerBridgeSPI {
    <T> void register(Class<T> dataClass);

    <T> void registerNamespace(Class<T> dataClass, String namespace);

    <T> void registerNamespaceValidator(Class<T> dataClass, String namespace, Class<? extends DataValidator<?>> validatorClass);

    <T> Map<ResourceLocation, T> getAllData(Class<T> dataClass);

    <T> T getData(Class<T> dataClass, ResourceLocation location);

    <T> List<T> getDataList(Class<T> dataClass);

    <T> Set<T> getCachedData(Class<T> dataClass, String cacheKey);

    <T> void addToCache(Class<T> dataClass, String cacheKey, T data);

    <T> void clearCache(Class<T> dataClass);

    <T> void updateClientData(Class<T> dataClass, Map<ResourceLocation, T> data);

    boolean isModLoaded(String modid);
}