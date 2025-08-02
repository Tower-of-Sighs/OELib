package com.mafuyu404.oelib.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mafuyu404.oelib.OElib;
import com.mafuyu404.oelib.api.DataDriven;
import com.mafuyu404.oelib.api.DataValidator;
import com.mafuyu404.oelib.event.DataReloadEvent;
import com.mafuyu404.oelib.network.DataSyncPacket;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用数据管理器。
 * <p>
 * 负责管理所有带有 {@link DataDriven} 注解的数据类型，
 * 提供数据加载、验证、缓存、网络同步等功能。
 * </p>
 *
 * @param <T> 数据类型
 */
@Mod.EventBusSubscriber(modid = OElib.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DataManager<T> extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final Map<Class<?>, DataManager<?>> managers = new ConcurrentHashMap<>();
    private static boolean serverStarted = false;
    private final Class<T> dataClass;
    private final DataDriven annotation;
    private final Codec<T> codec;
    private final DataValidator<T> validator;
    private final Map<ResourceLocation, T> loadedData = new ConcurrentHashMap<>();
    private final Map<String, Set<T>> cache = new ConcurrentHashMap<>();

    private DataManager(Class<T> dataClass) {
        super(GSON, getFolder(dataClass));
        this.dataClass = dataClass;
        this.annotation = dataClass.getAnnotation(DataDriven.class);
        this.codec = getCodec(dataClass);
        this.validator = createValidator(annotation.validator());
    }

    /**
     * 注册数据驱动类型。
     *
     * @param dataClass 数据类型
     * @param <T>       数据类型泛型
     * @return 数据管理器实例
     */
    @SuppressWarnings("unchecked")
    public static <T> DataManager<T> register(Class<T> dataClass) {
        if (!dataClass.isAnnotationPresent(DataDriven.class)) {
            throw new IllegalArgumentException("Class " + dataClass.getSimpleName() + " must be annotated with @DataDriven");
        }

        return (DataManager<T>) managers.computeIfAbsent(dataClass, DataManager::new);
    }

    /**
     * 获取数据管理器实例。
     *
     * @param dataClass 数据类型
     * @param <T>       数据类型泛型
     * @return 数据管理器实例，如果未注册则返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> DataManager<T> get(Class<T> dataClass) {
        return (DataManager<T>) managers.get(dataClass);
    }

    /**
     * 获取所有已加载的数据。
     *
     * @return 数据映射的副本
     */
    public Map<ResourceLocation, T> getAllData() {
        return new HashMap<>(loadedData);
    }

    /**
     * 根据资源位置获取数据。
     *
     * @param location 资源位置
     * @return 数据，如果不存在则返回 null
     */
    public T getData(ResourceLocation location) {
        return loadedData.get(location);
    }

    /**
     * 获取所有数据的列表。
     *
     * @return 数据列表
     */
    public List<T> getDataList() {
        return new ArrayList<>(loadedData.values());
    }

    /**
     * 根据缓存键获取数据。
     * <p>
     * 仅在启用缓存时有效。
     * </p>
     *
     * @param cacheKey 缓存键
     * @return 数据集合
     */
    public Set<T> getCachedData(String cacheKey) {
        if (!annotation.enableCache()) {
            throw new UnsupportedOperationException("Cache is disabled for " + dataClass.getSimpleName());
        }
        return cache.getOrDefault(cacheKey, Collections.emptySet());
    }

    /**
     * 添加数据到缓存。
     *
     * @param cacheKey 缓存键
     * @param data     数据
     */
    public void addToCache(String cacheKey, T data) {
        if (annotation.enableCache()) {
            cache.computeIfAbsent(cacheKey, k -> ConcurrentHashMap.newKeySet()).add(data);
        }
    }

    /**
     * 清空缓存。
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * 更新客户端数据。
     * <p>
     * 此方法仅在客户端调用。
     * </p>
     *
     * @param data 新数据
     */
    public void updateClientData(Map<ResourceLocation, T> data) {
        loadedData.clear();
        loadedData.putAll(data);
        clearCache();

        // 重建缓存
        if (annotation.enableCache()) {
            for (Map.Entry<ResourceLocation, T> entry : data.entrySet()) {
                buildCache(entry.getValue());
            }
        }

        OElib.LOGGER.debug("Updated client data for {}: {} entries", dataClass.getSimpleName(), data.size());

        MinecraftForge.EVENT_BUS.post(new DataReloadEvent(dataClass, data.size(), 0));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        loadedData.clear();
        clearCache();

        OElib.LOGGER.info("Loading {} data from {} files", dataClass.getSimpleName(), object.size());

        int validCount = 0;
        int invalidCount = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            ResourceLocation location = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                if (annotation.supportArray() && json.isJsonArray()) {
                    // 处理数组格式
                    var jsonArray = json.getAsJsonArray();
                    OElib.LOGGER.debug("Processing array with {} elements from {}", jsonArray.size(), location);

                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonElement element = jsonArray.get(i);
                        ResourceLocation elementLocation = new ResourceLocation(
                                location.getNamespace(),
                                location.getPath() + "_" + i
                        );

                        var result = codec.parse(JsonOps.INSTANCE, element);
                        if (result.result().isPresent()) {
                            T data = result.result().get();

                            // 验证数据
                            var validationResult = validator.validate(data, elementLocation);
                            if (validationResult.valid()) {
                                loadedData.put(elementLocation, data);

                                // 构建缓存
                                if (annotation.enableCache()) {
                                    buildCache(data);
                                }

                                validCount++;
                                OElib.LOGGER.debug("Loaded {} from array[{}]: {}", dataClass.getSimpleName(), i, elementLocation);
                            } else {
                                invalidCount++;
                                OElib.LOGGER.warn("Invalid {} data in array[{}] of {}: {}",
                                        dataClass.getSimpleName(), i, location, validationResult.message());
                            }
                        } else {
                            invalidCount++;
                            OElib.LOGGER.error("Failed to parse {} data from array[{}] of {}: {}",
                                    dataClass.getSimpleName(), i, location, result.error().orElse(null));
                        }
                    }
                } else {
                    // 处理单个对象格式
                    var result = codec.parse(JsonOps.INSTANCE, json);
                    if (result.result().isPresent()) {
                        T data = result.result().get();

                        // 验证数据
                        var validationResult = validator.validate(data, location);
                        if (validationResult.valid()) {
                            loadedData.put(location, data);

                            // 构建缓存
                            if (annotation.enableCache()) {
                                buildCache(data);
                            }

                            validCount++;
                            OElib.LOGGER.debug("Loaded {}: {}", dataClass.getSimpleName(), location);
                        } else {
                            invalidCount++;
                            OElib.LOGGER.warn("Invalid {} data in {}: {}", dataClass.getSimpleName(), location, validationResult.message());
                        }
                    } else {
                        invalidCount++;
                        OElib.LOGGER.error("Failed to parse {} data from {}: {}", dataClass.getSimpleName(), location, result.error().orElse(null));
                    }
                }
            } catch (Exception e) {
                invalidCount++;
                OElib.LOGGER.error("Error loading {} data from {}", dataClass.getSimpleName(), location, e);
            }
        }

        OElib.LOGGER.info("Loaded {} valid {} entries, {} invalid entries were skipped",
                validCount, dataClass.getSimpleName(), invalidCount);

        if (annotation.syncToClient() && serverStarted) {
            syncToAllPlayers();
        }

        MinecraftForge.EVENT_BUS.post(new DataReloadEvent(dataClass, validCount, invalidCount));
    }

    /**
     * 构建缓存。
     * <p>
     * 子类可以重写此方法来实现自定义的缓存逻辑。
     * </p>
     *
     * @param data 数据
     */
    protected void buildCache(T data) {
        // 默认实现，将数据添加到 "all" 缓存键
        addToCache("all", data);
    }

    private void syncToAllPlayers() {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !loadedData.isEmpty()) {
                DataSyncPacket<T> packet = new DataSyncPacket<>(dataClass, new HashMap<>(loadedData));
                packet.sendToAll();
                OElib.LOGGER.debug("Synced {} data to all players", dataClass.getSimpleName());
            }
        } catch (Exception e) {
            OElib.LOGGER.error("Failed to sync {} data to all players", dataClass.getSimpleName(), e);
        }
    }

    /**
     * 同步数据到指定玩家。
     *
     * @param player 玩家
     */
    public void syncToPlayer(ServerPlayer player) {
        if (annotation.syncToClient() && !loadedData.isEmpty()) {
            try {
                DataSyncPacket<T> packet = new DataSyncPacket<>(dataClass, new HashMap<>(loadedData));
                packet.sendTo(player);
                OElib.LOGGER.debug("Synced {} data to player: {}", dataClass.getSimpleName(), player.getName().getString());
            } catch (Exception e) {
                OElib.LOGGER.error("Failed to sync {} data to player {}", dataClass.getSimpleName(), player.getName().getString(), e);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        serverStarted = true;
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MinecraftServer server = player.getServer();
            if (server != null) {
                scheduleDelayedSync(server, player, 100);
            }
        }
    }

    private static void scheduleDelayedSync(MinecraftServer server, ServerPlayer player, int delayTicks) {
        final int ticks = Math.max(delayTicks, 0);
        Runnable[] task = new Runnable[1];
        task[0] = () -> {
            if (ticks <= 0) {
                executeDataSync(server, player);
            } else {
                server.execute(() -> scheduleDelayedSync(server, player, ticks - 1));
            }
        };
        server.execute(task[0]);
    }

    private static void executeDataSync(MinecraftServer server, ServerPlayer player) {
        if (server.getPlayerList().getPlayer(player.getUUID()) != null) {
            for (DataManager<?> manager : managers.values()) {
                if (manager.annotation.syncToClient()) {
                    manager.syncToPlayer(player);
                }
            }
            OElib.LOGGER.debug("Executed data sync for player: {}", player.getName().getString());
        }
    }


    private static String getFolder(Class<?> dataClass) {
        DataDriven annotation = dataClass.getAnnotation(DataDriven.class);
        String folder = annotation.folder();
        String modid = annotation.modid();

        // 如果指定了modid，则在文件夹路径前加上modid
        if (!modid.isEmpty()) {
            return modid + "/" + folder;
        }

        return folder;
    }

    @SuppressWarnings("unchecked")
    private static <T> Codec<T> getCodec(Class<T> dataClass) {
        try {
            Field codecField = dataClass.getDeclaredField("CODEC");
            codecField.setAccessible(true);
            return (Codec<T>) codecField.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get CODEC field from " + dataClass.getSimpleName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private DataValidator<T> createValidator(Class<? extends DataValidator<?>> validatorClass) {
        if (validatorClass == DataValidator.NoValidator.class) {
            return (DataValidator<T>) new DataValidator.NoValidator();
        }

        try {
            return (DataValidator<T>) validatorClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            OElib.LOGGER.warn("Failed to create validator {}, using no validator", validatorClass.getSimpleName(), e);
            return (DataValidator<T>) new DataValidator.NoValidator();
        }
    }
}