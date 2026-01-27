package cc.sighs.oelib.network.codec;

public interface StreamDecoder<I, T> {
    T decode(I object);
}
