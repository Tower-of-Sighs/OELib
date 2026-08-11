package cc.sighs.oelib.data.mvel.gen;

import java.util.Set;

public interface ExpressionFunctionsRegistrar {
    void register(ExpressionFunctionRegistry registry, Set<String> requiredFunctions);
}