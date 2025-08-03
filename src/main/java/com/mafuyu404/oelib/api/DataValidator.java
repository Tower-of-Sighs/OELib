package com.mafuyu404.oelib.api;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * 数据验证器接口。
 * <p>
 * 实现此接口来为数据驱动类型提供自定义验证逻辑。
 * </p>
 *
 * @param <T> 数据类型
 */
public interface DataValidator<T> {

    /**
     * 验证数据的有效性。
     *
     * @param data   要验证的数据
     * @param source 数据来源文件位置
     * @return 验证结果
     */
    ValidationResult validate(T data, ResourceLocation source);

    /**
     * 上下文感知的数据验证器接口。
     * <p>
     * 实现此接口的验证器可以访问服务器实例，从而获取注册表查找器等上下文信息。
     * </p>
     *
     * @param <T> 数据类型
     */
    interface ServerContextAware<T> extends DataValidator<T> {

        /**
         * 使用服务器上下文验证数据的有效性。
         *
         * @param data   要验证的数据
         * @param source 数据来源文件位置
         * @param server 服务器实例，可能为null（如在客户端）
         * @return 验证结果
         */
        ValidationResult validateWithContext(T data, ResourceLocation source, MinecraftServer server);

        /**
         * 默认实现，如果服务器实例可用则使用上下文验证，否则使用基础验证。
         */
        @Override
        default ValidationResult validate(T data, ResourceLocation source) {
            return validateWithContext(data, source, null);
        }
    }

    /**
     * 验证结果。
     */
    record ValidationResult(boolean valid, String message, boolean deferrable) {

        /**
         * 创建成功的验证结果。
         */
        public static ValidationResult success() {
            return new ValidationResult(true, null, false);
        }

        /**
         * 创建失败的验证结果。
         *
         * @param message 错误消息
         */
        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message, false);
        }

        /**
         * 创建可延迟验证的结果。
         * <p>
         * 用于处理依赖于运行时状态（如tag系统）的验证。
         * 数据会被加载，但标记为需要延迟验证。
         * </p>
         *
         * @param message 延迟原因消息
         */
        public static ValidationResult deferred(String message) {
            return new ValidationResult(true, message, true);
        }
    }

    /**
     * 默认的无验证器实现。
     */
    class NoValidator implements DataValidator<Object> {
        @Override
        public ValidationResult validate(Object data, ResourceLocation source) {
            return ValidationResult.success();
        }
    }
}