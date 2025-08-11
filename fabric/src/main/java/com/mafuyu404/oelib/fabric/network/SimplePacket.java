package com.mafuyu404.oelib.fabric.network;

/**
 * Fabric简单网络包基类。
 * <p>
 * 继承自通用的SimplePacket基类。
 * </p>
 *
 * @param <T> 网络包类型
 */
public abstract class SimplePacket<T extends SimplePacket<T>> extends com.mafuyu404.oelib.api.net.SimplePacket<T> {
    // 继承所有功能，无需额外实现
}