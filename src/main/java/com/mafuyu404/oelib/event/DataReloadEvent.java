package com.mafuyu404.oelib.event;

import net.minecraftforge.eventbus.api.Event;

/**
 * 数据重载事件。
 * <p>
 * 当数据驱动类型的数据重载完成时触发此事件。
 * 外部模组可以监听此事件来更新自己的缓存或执行其他操作。
 * </p>
 *
 * @author Flechazo
 * @since 1.0.0
 */
public class DataReloadEvent extends Event {
    
    private final Class<?> dataClass;
    private final int loadedCount;
    private final int invalidCount;
    
    public DataReloadEvent(Class<?> dataClass, int loadedCount, int invalidCount) {
        this.dataClass = dataClass;
        this.loadedCount = loadedCount;
        this.invalidCount = invalidCount;
    }
    
    /**
     * 获取重载的数据类型。
     *
     * @return 数据类型
     */
    public Class<?> getDataClass() {
        return dataClass;
    }
    
    /**
     * 获取成功加载的数据条目数量。
     *
     * @return 加载的数据数量
     */
    public int getLoadedCount() {
        return loadedCount;
    }
    
    /**
     * 获取无效的数据条目数量。
     *
     * @return 无效数据数量
     */
    public int getInvalidCount() {
        return invalidCount;
    }
    
    /**
     * 检查是否为指定的数据类型。
     *
     * @param clazz 要检查的数据类型
     * @return 如果是指定类型则返回 true
     */
    public boolean isDataType(Class<?> clazz) {
        return dataClass.equals(clazz);
    }
}