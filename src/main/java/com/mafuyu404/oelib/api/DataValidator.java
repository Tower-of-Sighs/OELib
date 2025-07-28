package com.mafuyu404.oelib.api;

import net.minecraft.resources.ResourceLocation;

/**
 * 数据验证器接口。
 * <p>
 * 实现此接口来为数据驱动类型提供自定义验证逻辑。
 * </p>
 *
 * @param <T> 数据类型
 * @author Flechazo
 * @since 1.0.0
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
     * 验证结果。
     */
    record ValidationResult(boolean valid, String message) {

        /**
         * 创建成功的验证结果。
         */
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        /**
         * 创建失败的验证结果。
         *
         * @param message 错误消息
         */
        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
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