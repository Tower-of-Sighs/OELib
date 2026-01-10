package cc.sighs.oelib.config.field;

import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.function.Function;

public interface FieldBuilder<T> {

    FieldBuilder<T> comment(String text);

    FieldBuilder<T> tooltip();

    FieldBuilder<T> defaultValue(T value);

    <O> RecordCodecBuilder<O, T> forGetter(Function<O, T> getter);

}
