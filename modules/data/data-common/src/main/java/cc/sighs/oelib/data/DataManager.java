package cc.sighs.oelib.data;

import cc.sighs.oelib.data.api.DataValidator;
import cc.sighs.oelib.data.spi.IDataManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

public final class DataManager {
    private static final IDataManager IMPL;

    static {
        IMPL = ServiceLoader.load(IDataManager.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No IDataManager implementation found"));
    }

    private DataManager() {
    }

    @ApiStatus.Internal
    public static <T> void register(Class<T> dataClass) {
        IMPL.register(dataClass);
    }

    @ApiStatus.Internal
    public static <T> void registerNamespace(Class<T> dataClass, String namespace) {
        IMPL.registerNamespace(dataClass, namespace);
    }

    @ApiStatus.Internal
    public static <T> void registerNamespaceValidator(Class<T> dataClass, String namespace,
                                                      Class<? extends DataValidator<?>> validatorClass) {
        IMPL.registerNamespaceValidator(dataClass, namespace, validatorClass);
    }


    public static <T> Map<ResourceLocation, T> getAllData(Class<T> dataClass) {
        return IMPL.getAllData(dataClass);
    }


    public static <T> T getData(Class<T> dataClass, ResourceLocation location) {
        return IMPL.getData(dataClass, location);
    }


    public static <T> List<T> getDataList(Class<T> dataClass) {
        return IMPL.getDataList(dataClass);
    }


    public static <T> Set<T> getCachedData(Class<T> dataClass, String cacheKey) {
        return IMPL.getCachedData(dataClass, cacheKey);
    }


    public static <T> void addToCache(Class<T> dataClass, String cacheKey, T data) {
        IMPL.addToCache(dataClass, cacheKey, data);
    }


    public static <T> void clearCache(Class<T> dataClass) {
        IMPL.clearCache(dataClass);
    }


    public static <T> void updateClientData(Class<T> dataClass, Map<ResourceLocation, T> data) {
        IMPL.updateClientData(dataClass, data);
    }

    public static void updateClientDataRaw(Class<?> dataClass, Map<ResourceLocation, ?> data) {
        IMPL.updateClientDataRaw(dataClass, data);
    }
}