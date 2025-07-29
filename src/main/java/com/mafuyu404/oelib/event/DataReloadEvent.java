package com.mafuyu404.oelib.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * 数据重载事件。
 * <p>
 * 当数据驱动类型的数据重载完成时触发此事件。
 * 外部模组可以监听此事件来更新自己的缓存或执行其他操作。
 * </p>
 */
public interface DataReloadEvent {

    Event<DataReloadEvent> EVENT = EventFactory.createArrayBacked(DataReloadEvent.class,
            (listeners) -> (dataClass, loadedCount, invalidCount) -> {
                for (DataReloadEvent listener : listeners) {
                    listener.onDataReload(dataClass, loadedCount, invalidCount);
                }
            });

    /**
     * 数据重载回调。
     *
     * @param dataClass    重载的数据类型
     * @param loadedCount  成功加载的数据条目数量
     * @param invalidCount 无效的数据条目数量
     */
    void onDataReload(Class<?> dataClass, int loadedCount, int invalidCount);
}