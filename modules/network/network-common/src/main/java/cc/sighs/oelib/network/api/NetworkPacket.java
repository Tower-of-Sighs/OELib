package cc.sighs.oelib.network.api;

import org.jetbrains.annotations.Range;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Metadata annotation for custom network packets.
 * <p>
 * Types annotated with this are treated as custom packet payloads that can be
 * registered and sent through the shared networking API. Implementations are
 * expected to implement {@link INetworkPacket} or one of its sub-interfaces.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NetworkPacket {

    int MIN_CHUNK_SIZE = 1024;
    int MAX_SERVERBOUND_CHUNK_SIZE = 30_000;
    int MAX_CLIENTBOUND_CHUNK_SIZE = 1_000_000;
    int MAX_CHUNK_COUNT = 4096;
    int MAX_CHUNKED_PACKET_SIZE = 64 * 1024 * 1024;

    /**
     * Mod identifier of this packet.
     * <p>
     * This becomes the {@code namespace} part of the underlying
     * {@link net.minecraft.resources.ResourceLocation} used as the packet type.
     * </p>
     *
     * @return mod identifier, for example {@code "oelib"}
     */
    String modId();

    /**
     * Packet type path within the mod namespace.
     * <p>
     * This becomes the {@code path} part of the underlying
     * {@link net.minecraft.resources.ResourceLocation} used as the packet type.
     * </p>
     *
     * @return packet type path, for example {@code "data_sync_chunk"}
     */
    String id();

    /**
     * Target side for this packet.
     * <p>
     * Controls where a receiver is registered and what direction is expected
     * when the packet is sent.
     * </p>
     *
     * @return target side, default is {@link Side#BOTH}
     */
    Side side() default Side.BOTH;

    /**
     * Registration priority.
     * <p>
     * Lower values indicate higher priority and cause the packet to be
     * registered earlier than packets with a higher value.
     * </p>
     *
     * @return priority value, default is {@code 1000}
     */
    int priority() default 1000;

    /**
     * Chunking threshold in bytes.
     * <p>
     * On Fabric, when the encoded packet size exceeds this value, OELib splits
     * the payload into chunks for transfer. The OELib NeoForge implementation
     * intentionally ignores this value and sends the original payload directly;
     * NeoForge's negotiated {@code GenericPacketSplitter} transparently handles
     * complete packets that exceed the platform frame limit.
     * Zero disables chunking; negative values are invalid.
     * On Fabric, a positive value is also used as the maximum data size of each
     * chunk and must be at least {@link #MIN_CHUNK_SIZE}. Client-bound packets
     * may use up to {@link #MAX_CLIENTBOUND_CHUNK_SIZE}; server-bound and
     * bidirectional packets are limited to {@link #MAX_SERVERBOUND_CHUNK_SIZE}
     * so the chunk envelope remains below Minecraft's server-bound custom
     * payload limit. These constraints are validated on every loader so a
     * shared packet declaration remains safe when the same mod targets Fabric.
     * Runtime validation is authoritative; {@link Range} only supplies static
     * analysis metadata and cannot express the side-dependent constraints.
     * </p>
     *
     * @return Fabric chunk threshold, ignored by NeoForge; default is {@code 0}
     */
    @Range(from = 0, to = MAX_CLIENTBOUND_CHUNK_SIZE)
    int chunkThreshold() default 0;
}
