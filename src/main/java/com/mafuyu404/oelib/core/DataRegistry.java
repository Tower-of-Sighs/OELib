package com.mafuyu404.oelib.core;

import com.mafuyu404.oelib.OElib;
import com.mafuyu404.oelib.api.DataDriven;
import com.mafuyu404.oelib.functions.CoreFunctions;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据注册表。
 * <p>
 * 负责管理所有数据驱动类型的注册和初始化。
 * </p>
 *
 */
@Mod.EventBusSubscriber(modid = OElib.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DataRegistry {

    private static final Set<Class<?>> registeredTypes = ConcurrentHashMap.newKeySet();
    private static boolean initialized = false;

    /**
     * 注册数据驱动类型。
     *
     * @param dataClass 数据类型
     * @param <T>       数据类型泛型
     */
    public static <T> void register(Class<T> dataClass) {
        if (!dataClass.isAnnotationPresent(DataDriven.class)) {
            throw new IllegalArgumentException("Class " + dataClass.getSimpleName() + " must be annotated with @DataDriven");
        }

        registeredTypes.add(dataClass);
        DataManager.register(dataClass);

        OElib.LOGGER.debug("Registered data-driven type: {}", dataClass.getSimpleName());
    }

    /**
     * 初始化数据注册表。
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        ExpressionEngine.registerFunctionClass(CoreFunctions.class, OElib.MODID);

        ExpressionEngine.initialize();

        initialized = true;
        OElib.LOGGER.info("Data registry initialized with {} registered types", registeredTypes.size());
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        // 按优先级排序注册数据管理器
        List<Class<?>> sortedTypes = new ArrayList<>(registeredTypes);
        sortedTypes.sort(Comparator.comparingInt(clazz -> clazz.getAnnotation(DataDriven.class).priority()));

        for (Class<?> dataClass : sortedTypes) {
            DataManager<?> manager = DataManager.get(dataClass);
            if (manager != null) {
                event.addListener(manager);
                OElib.LOGGER.debug("Added reload listener for: {}", dataClass.getSimpleName());
            }
        }
    }

    /**
     * 获取所有已注册的数据类型。
     *
     * @return 已注册的数据类型集合
     */
    public static Set<Class<?>> getRegisteredTypes() {
        return Set.copyOf(registeredTypes);
    }

    /**
     * 检查类型是否已注册。
     *
     * @param dataClass 数据类型
     * @return 是否已注册
     */
    public static boolean isRegistered(Class<?> dataClass) {
        return registeredTypes.contains(dataClass);
    }
}