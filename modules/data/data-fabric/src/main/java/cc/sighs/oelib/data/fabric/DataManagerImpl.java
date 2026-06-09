package cc.sighs.oelib.data.fabric;

import cc.sighs.oelib.data.OELibData;
import cc.sighs.oelib.data.api.DataValidator;
import cc.sighs.oelib.data.spi.IDataManager;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DataManagerImpl implements IDataManager {
    private static final Set<Class<?>> registeredReloadListeners = ConcurrentHashMap.newKeySet();

    @Override
    public <T> void register(Class<T> dataClass) {
        DataManager.register(dataClass);
        if (registeredReloadListeners.add(dataClass)) {
            var helper = ResourceManagerHelper.get(PackType.SERVER_DATA);
            var manager = DataManager.get(dataClass);
            if (manager != null) {
                helper.registerReloadListener(manager);
            }
        }
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
    public void updateClientDataRaw(Class<?> dataClass, Map<ResourceLocation, ?> data) {
        @SuppressWarnings("unchecked")
        DataManager<Object> manager = (DataManager<Object>) DataManager.get(dataClass);
        if (manager != null) {
            @SuppressWarnings("unchecked")
            Map<ResourceLocation, Object> castedData = (Map<ResourceLocation, Object>) data;
            manager.updateClientData(castedData);
        } else {
            OELibData.LOGGER.warn("No DataManager found for class {} during raw client data update", dataClass.getName());
        }
    }
}