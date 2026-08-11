package cc.sighs.oelib.config.serialization;

import com.flechazo.hkt.Try;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Reads and writes TOML-compatible object trees through the {@link DynamicOps} contract.
 *
 * <p>{@link #INSTANCE} preserves standard TOML value categories. {@link #COMPRESSED} additionally
 * accepts compatible numeric and string representations during conversion.
 */
public class TomlOps implements DynamicOps<Object> {
    /** Standard TOML operations instance. */
    public static final TomlOps INSTANCE = new TomlOps(false);
    /** Compressed-mode TOML operations with relaxed type conversions. */
    public static final TomlOps COMPRESSED = new TomlOps(true);

    private static final Object EMPTY = new Object();

    private final boolean compressed;

    private TomlOps(boolean compressed) {
        this.compressed = compressed;
    }

    private static List<Object> asList(Object value) {
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) value;
        return list;
    }

    private static Map<String, Object> asMap(Object value) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        return map;
    }

    @Override
    public Object empty() {
        return EMPTY;
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, Object input) {
        if (input instanceof Map) {
            return convertMap(outOps, input);
        }
        if (input instanceof List) {
            return convertList(outOps, input);
        }
        if (input == EMPTY) {
            return outOps.empty();
        }
        if (input instanceof String) {
            return outOps.createString((String) input);
        }
        if (input instanceof Boolean) {
            return outOps.createBoolean((Boolean) input);
        }
        if (input instanceof Number) {
            var value = input instanceof BigDecimal ? (BigDecimal) input : new BigDecimal(input.toString());
            return Try.of(value::longValueExact).fold(error -> {
                double d = value.doubleValue();
                if ((float) d == d) {
                    return outOps.createFloat((float) d);
                }
                return outOps.createDouble(d);
            }, l -> {
                long integer = l;
                if ((byte) integer == integer) {
                    return outOps.createByte((byte) integer);
                }
                if ((short) integer == integer) {
                    return outOps.createShort((short) integer);
                }
                if ((int) integer == integer) {
                    return outOps.createInt((int) integer);
                }
                return outOps.createLong(integer);
            });
        }
        return outOps.empty();
    }

    @Override
    public DataResult<Number> getNumberValue(Object input) {
        if (input instanceof Number) {
            return DataResult.success((Number) input);
        }
        if (compressed && input instanceof String) {
            return Try.of(() -> Integer.parseInt((String) input)).fold(
                    error -> DataResult.error(() -> "Not a number: " + error + " " + input),
                    DataResult::success);
        }
        return DataResult.error(() -> "Not a number: " + input);
    }

    @Override
    public Object createNumeric(Number i) {
        return i;
    }

    @Override
    public DataResult<Boolean> getBooleanValue(Object input) {
        if (input instanceof Boolean) {
            return DataResult.success((Boolean) input);
        }
        return DataResult.error(() -> "Not a boolean: " + input);
    }

    @Override
    public Object createBoolean(boolean value) {
        return value;
    }

    @Override
    public DataResult<String> getStringValue(Object input) {
        if (input instanceof String) {
            return DataResult.success((String) input);
        }
        if (compressed && input instanceof Number) {
            return DataResult.success(input.toString());
        }
        return DataResult.error(() -> "Not a string: " + input);
    }

    @Override
    public Object createString(String value) {
        return value;
    }

    @Override
    public DataResult<Object> mergeToList(Object list, Object value) {
        if (!(list instanceof List) && list != empty()) {
            return DataResult.error(() -> "mergeToList called with not a list: " + list, list);
        }
        List<Object> result = new ArrayList<>();
        if (list != empty()) {
            result.addAll(asList(list));
        }
        result.add(value);
        return DataResult.success(result);
    }

    @Override
    public DataResult<Object> mergeToList(Object list, List<Object> values) {
        if (!(list instanceof List) && list != empty()) {
            return DataResult.error(() -> "mergeToList called with not a list: " + list, list);
        }
        List<Object> result = new ArrayList<>();
        if (list != empty()) {
            result.addAll(asList(list));
        }
        result.addAll(values);
        return DataResult.success(result);
    }

    @Override
    public DataResult<Object> mergeToMap(Object map, Object key, Object value) {
        if (!(map instanceof Map) && map != empty()) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + map, map);
        }
        String keyString;
        if (key instanceof String) {
            keyString = (String) key;
        } else if (compressed) {
            keyString = Objects.toString(key);
        } else {
            return DataResult.error(() -> "key is not a string: " + key, map);
        }
        Map<String, Object> output = new LinkedHashMap<>();
        if (map != empty()) {
            output.putAll(asMap(map));
        }
        output.put(keyString, value);
        return DataResult.success(output);
    }

    @Override
    public DataResult<Object> mergeToMap(Object map, MapLike<Object> values) {
        if (!(map instanceof Map) && map != empty()) {
            return DataResult.error(() -> "mergeToMap called with not a map: " + map, map);
        }
        Map<String, Object> output = new LinkedHashMap<>();
        if (map != empty()) {
            output.putAll(asMap(map));
        }
        List<Object> missed = new ArrayList<>();
        values.entries().forEach(entry -> {
            var key = entry.getFirst();
            String keyString;
            if (key instanceof String) {
                keyString = (String) key;
            } else if (compressed) {
                keyString = Objects.toString(key);
            } else {
                missed.add(key);
                return;
            }
            output.put(keyString, entry.getSecond());
        });
        if (!missed.isEmpty()) {
            return DataResult.error(() -> "some keys are not strings: " + missed, output);
        }
        return DataResult.success(output);
    }

    @Override
    public DataResult<Stream<Pair<Object, Object>>> getMapValues(Object input) {
        if (!(input instanceof Map)) {
            return DataResult.error(() -> "Not a map: " + input);
        }
        var object = asMap(input);
        return DataResult.success(object.entrySet().stream().map(entry -> {
            var value = entry.getValue();
            if (value == EMPTY) {
                value = null;
            }
            return Pair.of(entry.getKey(), value);
        }));
    }

    @Override
    public DataResult<Consumer<BiConsumer<Object, Object>>> getMapEntries(Object input) {
        if (!(input instanceof Map)) {
            return DataResult.error(() -> "Not a map: " + input);
        }
        var object = asMap(input);
        return DataResult.success(c -> {
            for (Map.Entry<String, Object> entry : object.entrySet()) {
                var value = entry.getValue();
                if (value == EMPTY) {
                    value = null;
                }
                c.accept(entry.getKey(), value);
            }
        });
    }

    @Override
    public DataResult<MapLike<Object>> getMap(Object input) {
        if (!(input instanceof Map)) {
            return DataResult.error(() -> "Not a map: " + input);
        }
        var object = asMap(input);
        return DataResult.success(new MapLike<>() {
            @Override
            public Object get(Object key) {
                var element = object.get(Objects.toString(key));
                if (element == EMPTY) {
                    return null;
                }
                return element;
            }

            @Override
            public Object get(String key) {
                var element = object.get(key);
                if (element == EMPTY) {
                    return null;
                }
                return element;
            }

            @Override
            public Stream<Pair<Object, Object>> entries() {
                return object.entrySet().stream().map(e -> Pair.of(e.getKey(), e.getValue()));
            }

            @Override
            public String toString() {
                return "MapLike[" + object + "]";
            }
        });
    }

    @Override
    public Object createMap(Stream<Pair<Object, Object>> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach(p -> result.put(Objects.toString(p.getFirst()), p.getSecond()));
        return result;
    }

    @Override
    public DataResult<Stream<Object>> getStream(Object input) {
        if (input instanceof List) {
            var list = asList(input);
            return DataResult.success(list.stream().map(e -> e == EMPTY ? null : e));
        }
        return DataResult.error(() -> "Not a list: " + input);
    }

    @Override
    public DataResult<Consumer<Consumer<Object>>> getList(Object input) {
        if (input instanceof List) {
            var list = asList(input);
            return DataResult.success(c -> {
                for (Object element : list) {
                    c.accept(element == EMPTY ? null : element);
                }
            });
        }
        return DataResult.error(() -> "Not a list: " + input);
    }

    @Override
    public Object createList(Stream<Object> input) {
        List<Object> result = new ArrayList<>();
        input.forEach(result::add);
        return result;
    }

    @Override
    public Object remove(Object input, String key) {
        if (input instanceof Map) {
            Map<String, Object> result = new LinkedHashMap<>();
            asMap(input).entrySet().stream()
                    .filter(entry -> !Objects.equals(entry.getKey(), key))
                    .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
            return result;
        }
        return input;
    }

    @Override
    public String toString() {
        return "TOML";
    }

    @Override
    public ListBuilder<Object> listBuilder() {
        return new ArrayBuilder();
    }

    @Override
    public boolean compressMaps() {
        return compressed;
    }

    @Override
    public RecordBuilder<Object> mapBuilder() {
        return new TomlRecordBuilder();
    }

    private static final class ArrayBuilder implements ListBuilder<Object> {
        private DataResult<List<Object>> builder = DataResult.success(new ArrayList<>(), Lifecycle.stable());

        @Override
        public DynamicOps<Object> ops() {
            return INSTANCE;
        }

        @Override
        public ListBuilder<Object> add(Object value) {
            builder = builder.map(b -> {
                b.add(value);
                return b;
            });
            return this;
        }

        @Override
        public ListBuilder<Object> add(DataResult<Object> value) {
            builder = builder.apply2stable((b, element) -> {
                b.add(element);
                return b;
            }, value);
            return this;
        }

        @Override
        public ListBuilder<Object> withErrorsFrom(DataResult<?> result) {
            builder = builder.flatMap(r -> result.map(v -> r));
            return this;
        }

        @Override
        public ListBuilder<Object> mapError(UnaryOperator<String> onError) {
            builder = builder.mapError(onError);
            return this;
        }

        @Override
        public DataResult<Object> build(Object prefix) {
            var result = builder.flatMap(b -> {
                if (!(prefix instanceof List) && prefix != ops().empty()) {
                    return DataResult.error(() -> "Cannot append a list to not a list: " + prefix, prefix);
                }
                List<Object> array = new ArrayList<>();
                if (prefix != ops().empty()) {
                    array.addAll(asList(prefix));
                }
                array.addAll(b);
                return DataResult.success(array, Lifecycle.stable());
            });
            builder = DataResult.success(new ArrayList<>(), Lifecycle.stable());
            return result;
        }
    }

    private class TomlRecordBuilder extends RecordBuilder.AbstractStringBuilder<Object, Map<String, Object>> {
        protected TomlRecordBuilder() {
            super(TomlOps.this);
        }

        @Override
        protected Map<String, Object> initBuilder() {
            return new LinkedHashMap<>();
        }

        @Override
        protected Map<String, Object> append(String key, Object value, Map<String, Object> builder) {
            builder.put(key, value);
            return builder;
        }

        @Override
        protected DataResult<Object> build(Map<String, Object> builder, Object prefix) {
            if (prefix == null || prefix == EMPTY) {
                return DataResult.success(builder);
            }
            if (prefix instanceof Map) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.putAll(asMap(prefix));
                result.putAll(builder);
                return DataResult.success(result);
            }
            return DataResult.error(() -> "mergeToMap called with not a map: " + prefix, prefix);
        }
    }
}
