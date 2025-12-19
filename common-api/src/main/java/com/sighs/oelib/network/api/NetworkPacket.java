package com.sighs.oelib.network.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 网络包注解。
 * <p>
 * 使用此注解标记的类将被自动注册到网络系统中。
 * 被标记的类必须实现 {@link INetworkPacket} 接口。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @NetworkPacket(side = Side.CLIENT, chunkThreshold = 30000)
 * public record MyPacket(String message) implements INetworkPacket<MyPacket> {
 *     @Override
 *     public void encode(FriendlyByteBuf buf) {
 *         buf.writeUtf(message);
 *     }
 *
 *     public static MyPacket decode(FriendlyByteBuf buf) {
 *         return new MyPacket(buf.readUtf());
 *     }
 *
 *     @Override
 *     public void handle(INetworkContext context) {
 *         // 处理逻辑
 *     }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NetworkPacket {

    /**
     * 网络包的目标端。
     * <p>
     * 指定这个包应该在哪一端注册接收器。
     * </p>
     *
     * @return 目标端，默认为 BOTH（双端）
     */
    Side side() default Side.BOTH;

    /**
     * 网络包的优先级。
     * <p>
     * 数值越小优先级越高，用于控制注册顺序。
     * </p>
     *
     * @return 优先级，默认为 1000
     */
    int priority() default 1000;

    /**
     * 网络包分片阈值（字节）。
     * <p>
     * 当网络包的数据大小超过此阈值时，将自动进行分片传输。
     * 如果设置为 0 或负数，则不进行分片。
     * </p>
     *
     * @return 分片阈值，默认为 0（不分片）
     */
    int chunkThreshold() default 0;
}