package com.mafuyu404.oelib.data.mvel.gen;

public interface ExpressionFunctionRegistry {
    void register(String name, Class<?> ownerClass, String methodName, Class<?>... parameterTypes);
}