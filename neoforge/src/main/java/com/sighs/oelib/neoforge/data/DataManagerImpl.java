package com.sighs.oelib.neoforge.data;

import com.sighs.oelib.OELib;
import com.sighs.oelib.data.DataRegistry;
import com.sighs.oelib.data.api.DataValidator;
import com.sighs.oelib.data.spi.IDataManager;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = OELib.MODID)
public final class DataManagerImpl implements IDataManager {

    public <T> void register(Class<T> dataClass) {
        DataManager.register(dataClass);
    }

    public <T> void registerNamespace(Class<T> dataClass, String namespace) {
        DataManager.registerNamespace(dataClass, namespace);
    }

    public <T> void registerNamespaceValidator(Class<T> dataClass, String namespace,
                                               Class<? extends DataValidator<?>> validatorClass) {
        DataManager.registerNamespaceValidator(dataClass, namespace, validatorClass);
    }

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

    public <T> T getData(Class<T> dataClass, ResourceLocation location) {
        var manager = DataManager.get(dataClass);
        return manager != null ? manager.getData(location) : null;
    }

    public <T> List<T> getDataList(Class<T> dataClass) {
        var manager = DataManager.get(dataClass);
        return manager != null ? manager.getDataList() : List.of();
    }

    public <T> Set<T> getCachedData(Class<T> dataClass, String cacheKey) {
        var manager = DataManager.get(dataClass);
        return manager != null ? manager.getCachedData(cacheKey) : Set.of();
    }

    public <T> void addToCache(Class<T> dataClass, String cacheKey, T data) {
        var manager = DataManager.get(dataClass);
        if (manager != null) {
            manager.addToCache(cacheKey, data);
        }
    }

    public <T> void clearCache(Class<T> dataClass) {
        var manager = DataManager.get(dataClass);
        if (manager != null) {
            manager.clearCache();
        }
    }

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