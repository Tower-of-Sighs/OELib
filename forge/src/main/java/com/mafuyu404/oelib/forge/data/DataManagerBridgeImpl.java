package com.mafuyu404.oelib.forge.data;

import com.mafuyu404.oelib.OELib;
import com.mafuyu404.oelib.api.data.DataManagerBridgeSPI;
import com.mafuyu404.oelib.api.data.DataValidator;
import com.mafuyu404.oelib.data.DataRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = OELib.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DataManagerBridgeImpl implements DataManagerBridgeSPI {

    @Override
    public <T> void register(Class<T> dataClass) {
        DataManager.register(dataClass);
    }

    @Override
    public <T> void registerNamespace(Class<T> dataClass, String namespace) {
        DataManager.registerNamespace(dataClass, namespace);
    }

    @Override
    public <T> void registerNamespaceValidator(Class<T> dataClass, String namespace,
                                               Class<? extends DataValidator<?>> validatorClass) {
        DataManager.registerNamespaceValidator(dataClass, namespace, validatorClass);
    }

    @Override
    public <T> Map<ResourceLocation, T> getAllData(Class<T> dataClass) {
        var manager = DataManager.get(dataClass);
        return manager != null ? manager.getAllData() : Map.of();
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        List<Class<?>> types = DataRegistry.getRegisteredTypesByPriority();
        for (Class<?> dataClass : types) {
            var manager = DataManager.get(dataClass);
            if (manager != null) {
                event.addListener(manager);
            }
        }
    }

    @Override
    public <T> T getData(Class<T> dataClass, ResourceLocation location) {
        var manager = DataManager.get(dataClass);
        return manager != null ? manager.getData(location) : null;
    }

    @Override
    public <T> List<T> getDataList(Class<T> dataClass) {
        var manager = DataManager.get(dataClass);
        return manager != null ? manager.getDataList() : List.of();
    }

    @Override
    public <T> Set<T> getCachedData(Class<T> dataClass, String cacheKey) {
        var manager = DataManager.get(dataClass);
        return manager != null ? manager.getCachedData(cacheKey) : Set.of();
    }

    @Override
    public <T> void addToCache(Class<T> dataClass, String cacheKey, T data) {
        var manager = DataManager.get(dataClass);
        if (manager != null) {
            manager.addToCache(cacheKey, data);
        }
    }

    @Override
    public <T> void clearCache(Class<T> dataClass) {
        var manager = DataManager.get(dataClass);
        if (manager != null) {
            manager.clearCache();
        }
    }

    @Override
    public <T> void updateClientData(Class<T> dataClass, Map<ResourceLocation, T> data) {
        var manager = DataManager.get(dataClass);
        if (manager != null) {
            manager.updateClientData(data);
        }
    }

    @Override
    public boolean isModLoaded(String modid) {
        return ModList.get().isLoaded(modid);
    }
}