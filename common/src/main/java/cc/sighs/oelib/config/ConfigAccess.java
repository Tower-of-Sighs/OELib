package cc.sighs.oelib.config;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.util.Objects;
import java.util.function.Function;

public class ConfigAccess<T> {
    private final ConfigUnit<T> unit;

    public ConfigAccess(ConfigUnit<T> unit) {
        this.unit = unit;
    }

    public <V> void set(@NotNull Accessor<T, V> getter, @NotNull V value) {
        Objects.requireNonNull(value, "Config value cannot be null");
        T current = unit.get();
        String property = extractPropertyName(getter);
        T updated = updateRecord(current, property, value);
        unit.setValue(updated);
        unit.save();
    }

    private String extractPropertyName(Accessor<T, ?> getter) {
        try {
            var m = getter.getClass().getDeclaredMethod("writeReplace");
            m.setAccessible(true);
            var sl = (SerializedLambda) m.invoke(getter);
            return sl.getImplMethodName();
        } catch (Exception e) {
            throw new RuntimeException("If the resolving field name cannot be referenced by a method, check if it is the Record class", e);
        }
    }

    @SuppressWarnings("unchecked")
    private <V> T updateRecord(T current, String property, V value) {
        var cls = current.getClass();
        if (!cls.isRecord()) {
            throw new IllegalArgumentException("ConfigAccess only the Record type is supported:" + cls.getName());
        }

        try {
            var components = cls.getRecordComponents();
            Object[] args = new Object[components.length];
            Class<?>[] types = new Class<?>[components.length];

            for (int i = 0; i < components.length; i++) {
                var comp = components[i];
                types[i] = comp.getType();
                if (comp.getName().equals(property)) {
                    args[i] = value;
                } else {
                    args[i] = comp.getAccessor().invoke(current);
                }
            }

            var ctor = cls.getDeclaredConstructor(types);
            ctor.setAccessible(true);
            return (T) ctor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException("Unable to build a new Record instance to update fields:" + property, e);
        }
    }

    @FunctionalInterface
    public interface Accessor<T, R> extends Function<T, R>, Serializable {
    }
}