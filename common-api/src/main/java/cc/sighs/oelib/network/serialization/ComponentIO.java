package cc.sighs.oelib.network.serialization;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.*;

/**
 * Low-level I/O operations for common component types.
 * <p>
 * Handles encoding and decoding of primitive wrappers, strings, UUIDs, byte arrays, enums,
 * and a wide range of frequently used Minecraft types directly to/from {@link RegistryFriendlyByteBuf}.
 * </p>
 */
@SuppressWarnings({"unchecked", "rawtypes", "unused"})
final class ComponentIO {
    static final int MAX_DEPTH = 64;

    private ComponentIO() {
    }

    /**
     * Decodes a value using a precomputed plan.
     * Uses pre-bound MethodHandles for direct types to minimize branching overhead.
     */
    static Object decodeWithPlan(RegistryFriendlyByteBuf buf, ComponentPlan plan, int depth, String name, Class<?> owner) {
        if (depth > MAX_DEPTH) {
            throw new IllegalStateException("Decoding depth exceeded for record " + owner.getName() + "#" + name);
        }
        if (plan.readHandle != null) {
            try {
                return plan.readHandle.invoke(buf);
            } catch (Throwable t) {
                throw new IllegalStateException("Failed to decode component for record " + owner.getName() + "#" + name, t);
            }
        }
        switch (plan.kind) {
            case INT               -> { return buf.readVarInt(); }
            case LONG              -> { return buf.readVarLong(); }
            case BOOLEAN           -> { return buf.readBoolean(); }
            case FLOAT             -> { return buf.readFloat(); }
            case DOUBLE            -> { return buf.readDouble(); }
            case BYTE              -> { return buf.readByte(); }
            case SHORT             -> { return buf.readShort(); }
            case STRING            -> { return buf.readUtf(); }
            case UUID              -> { return buf.readUUID(); }
            case BYTE_ARRAY        -> { return buf.readByteArray(); }
            case INT_ARRAY         -> { return buf.readVarIntArray(); }
            case LONG_ARRAY        -> { return buf.readLongArray(); }
            case DATE              -> { return buf.readDate(); }
            case INSTANT           -> { return buf.readInstant(); }
            case BITSET            -> { return buf.readBitSet(); }
            case INT_LIST          -> { return buf.readIntIdList(); }
            case BLOCK_POS         -> { return buf.readBlockPos(); }
            case CHUNK_POS         -> { return buf.readChunkPos(); }
            case SECTION_POS       -> { return buf.readSectionPos(); }
            case GLOBAL_POS        -> { return buf.readGlobalPos(); }
            case VEC3              -> { return buf.readVec3(); }
            case VECTOR3F          -> { return buf.readVector3f(); }
            case QUATERNIONF       -> { return buf.readQuaternion(); }
            case RESOURCE_LOCATION -> { return buf.readResourceLocation(); }
            case BLOCK_HIT_RESULT  -> { return buf.readBlockHitResult(); }
            case COMPOUND_TAG      -> { return buf.readNbt(); }
            case TAG               -> { return buf.readNbt(NbtAccounter.unlimitedHeap()); }
            case ENUM              -> { return buf.readEnum(plan.enumClass); }
            case OPTIONAL          -> {
                boolean present = buf.readBoolean();
                if (!present) return Optional.empty();
                Object v = decodeWithPlan(buf, plan.element, depth + 1, name, owner);
                return Optional.of(v);
            }
            case LIST              -> { return readListGeneric(buf, plan.element, depth, name, owner); }
            case SET               -> { return readSetGeneric(buf, plan.element, depth, name, owner); }
            case MAP               -> { return readMapGeneric(buf, plan.key, plan.value, depth, name, owner); }
            case ENUM_SET          -> { return readEnumSetGeneric(buf, plan.enumClass); }
            default                -> throw new IllegalStateException("Unsupported plan for record " + owner.getName() + "#" + name);
        }
    }

    /**
     * Encodes a value using a precomputed plan.
     * Uses pre-bound MethodHandles for direct types to minimize branching overhead.
     */
    static void encodeWithPlan(RegistryFriendlyByteBuf buf, ComponentPlan plan, Object value, int depth, String name, Class<?> owner) {
        if (depth > MAX_DEPTH) {
            throw new IllegalStateException("Encoding depth exceeded for record " + owner.getName() + "#" + name);
        }
        if (plan.writeHandle != null) {
            try {
                plan.writeHandle.invoke(buf, value);
                return;
            } catch (Throwable t) {
                throw new IllegalStateException("Failed to encode component for record " + owner.getName() + "#" + name, t);
            }
        }
        switch (plan.kind) {
            case INT               -> buf.writeVarInt((Integer) value);
            case LONG              -> buf.writeVarLong((Long) value);
            case BOOLEAN           -> buf.writeBoolean((Boolean) value);
            case FLOAT             -> buf.writeFloat((Float) value);
            case DOUBLE            -> buf.writeDouble((Double) value);
            case BYTE              -> buf.writeByte((Byte) value);
            case SHORT             -> buf.writeShort((Short) value);
            case STRING            -> buf.writeUtf((String) value);
            case UUID              -> buf.writeUUID((UUID) value);
            case BYTE_ARRAY        -> buf.writeByteArray((byte[]) value);
            case INT_ARRAY         -> buf.writeVarIntArray((int[]) value);
            case LONG_ARRAY        -> buf.writeLongArray((long[]) value);
            case DATE              -> buf.writeDate((Date) value);
            case INSTANT           -> buf.writeInstant((Instant) value);
            case BITSET            -> buf.writeBitSet((BitSet) value);
            case INT_LIST          -> buf.writeIntIdList((IntList) value);
            case BLOCK_POS         -> buf.writeBlockPos((BlockPos) value);
            case CHUNK_POS         -> buf.writeChunkPos((ChunkPos) value);
            case SECTION_POS       -> buf.writeSectionPos((SectionPos) value);
            case GLOBAL_POS        -> buf.writeGlobalPos((GlobalPos) value);
            case VEC3              -> buf.writeVec3((Vec3) value);
            case VECTOR3F          -> buf.writeVector3f((Vector3f) value);
            case QUATERNIONF       -> buf.writeQuaternion((Quaternionf) value);
            case RESOURCE_LOCATION -> buf.writeResourceLocation((ResourceLocation) value);
            case BLOCK_HIT_RESULT  -> buf.writeBlockHitResult((BlockHitResult) value);
            case COMPOUND_TAG      -> buf.writeNbt((CompoundTag) value);
            case TAG               -> buf.writeNbt((Tag) value);
            case ENUM              -> buf.writeEnum((Enum<?>) value);
            case OPTIONAL          -> {
                Optional<?> opt = (Optional<?>) value;
                boolean present = opt != null && opt.isPresent();
                buf.writeBoolean(present);
                if (present) {
                    encodeWithPlan(buf, plan.element, opt.get(), depth + 1, name, owner);
                }
            }
            case LIST, SET         -> writeCollectionGeneric(buf, (Collection<?>) value, plan.element, depth, name, owner);
            case MAP               -> writeMapGeneric(buf, (Map<?, ?>) value, plan.key, plan.value, depth, name, owner);
            case ENUM_SET          -> writeEnumSetGeneric(buf, (EnumSet<?>) value, plan.enumClass);
            default                -> throw new IllegalStateException("Unsupported plan for record " + owner.getName() + "#" + name);
        }
    }

    /**
     * Builds a plan and binds direct read/write MethodHandles at initialization.
     */
    static ComponentPlan planOf(Class<?> rawType, Type genericType) {
        if (rawType == int.class || rawType == Integer.class) return bindStatic(new ComponentPlan(Kind.INT), "readVarIntW", int.class, "writeVarIntW", int.class);
        if (rawType == long.class || rawType == Long.class) return bindStatic(new ComponentPlan(Kind.LONG), "readVarLongW", long.class, "writeVarLongW", long.class);
        if (rawType == boolean.class || rawType == Boolean.class) return bindStatic(new ComponentPlan(Kind.BOOLEAN), "readBooleanW", boolean.class, "writeBooleanW", boolean.class);
        if (rawType == float.class || rawType == Float.class) return bindStatic(new ComponentPlan(Kind.FLOAT), "readFloatW", float.class, "writeFloatW", float.class);
        if (rawType == double.class || rawType == Double.class) return bindStatic(new ComponentPlan(Kind.DOUBLE), "readDoubleW", double.class, "writeDoubleW", double.class);
        if (rawType == byte.class || rawType == Byte.class) return bindStatic(new ComponentPlan(Kind.BYTE), "readByteW", byte.class, "writeByteW", byte.class);
        if (rawType == short.class || rawType == Short.class) return bindStatic(new ComponentPlan(Kind.SHORT), "readShortW", short.class, "writeShortW", short.class);
        if (rawType == String.class) return bindStatic(new ComponentPlan(Kind.STRING), "readUtfW", String.class, "writeUtfW", String.class);
        if (rawType == UUID.class) return bindStatic(new ComponentPlan(Kind.UUID), "readUUIDW", UUID.class, "writeUUIDW", UUID.class);
        if (rawType == byte[].class) return bindStatic(new ComponentPlan(Kind.BYTE_ARRAY), "readByteArrayW", byte[].class, "writeByteArrayW", byte[].class);
        if (rawType == int[].class) return bindStatic(new ComponentPlan(Kind.INT_ARRAY), "readVarIntArrayW", int[].class, "writeVarIntArrayW", int[].class);
        if (rawType == long[].class) return bindStatic(new ComponentPlan(Kind.LONG_ARRAY), "readLongArrayW", long[].class, "writeLongArrayW", long[].class);
        if (rawType == Date.class) return bindStatic(new ComponentPlan(Kind.DATE), "readDateW", Date.class, "writeDateW", Date.class);
        if (rawType == Instant.class) return bindStatic(new ComponentPlan(Kind.INSTANT), "readInstantW", Instant.class, "writeInstantW", Instant.class);
        if (rawType == BitSet.class) return bindStatic(new ComponentPlan(Kind.BITSET), "readBitSetW", BitSet.class, "writeBitSetW", BitSet.class);

        if (rawType == IntList.class) return bindStatic(new ComponentPlan(Kind.INT_LIST), "readIntIdListW", IntList.class, "writeIntIdListW", IntList.class);
        if (rawType == ResourceKey.class) return bindStatic(new ComponentPlan(Kind.RESOURCE_KEY), "readRegistryKeyW", ResourceKey.class, "writeResourceKeyW", ResourceKey.class);

        if (rawType == BlockPos.class) return bindStatic(new ComponentPlan(Kind.BLOCK_POS), "readBlockPosW", BlockPos.class, "writeBlockPosW", BlockPos.class);
        if (rawType == ChunkPos.class) return bindStatic(new ComponentPlan(Kind.CHUNK_POS), "readChunkPosW", ChunkPos.class, "writeChunkPosW", ChunkPos.class);
        if (rawType == SectionPos.class) return bindStatic(new ComponentPlan(Kind.SECTION_POS), "readSectionPosW", SectionPos.class, "writeSectionPosW", SectionPos.class);
        if (rawType == GlobalPos.class) return bindStatic(new ComponentPlan(Kind.GLOBAL_POS), "readGlobalPosW", GlobalPos.class, "writeGlobalPosW", GlobalPos.class);
        if (rawType == Vec3.class) return bindStatic(new ComponentPlan(Kind.VEC3), "readVec3W", Vec3.class, "writeVec3W", Vec3.class);
        if (rawType == Vector3f.class) return bindStatic(new ComponentPlan(Kind.VECTOR3F), "readVector3fW", Vector3f.class, "writeVector3fW", Vector3f.class);
        if (rawType == Quaternionf.class) return bindStatic(new ComponentPlan(Kind.QUATERNIONF), "readQuaternionW", Quaternionf.class, "writeQuaternionW", Quaternionf.class);
        if (rawType == ResourceLocation.class) return bindStatic(new ComponentPlan(Kind.RESOURCE_LOCATION), "readResourceLocationW", ResourceLocation.class, "writeResourceLocationW", ResourceLocation.class);
        if (rawType == BlockHitResult.class) return bindStatic(new ComponentPlan(Kind.BLOCK_HIT_RESULT), "readBlockHitResultW", BlockHitResult.class, "writeBlockHitResultW", BlockHitResult.class);

        if (rawType == CompoundTag.class) return bindStatic(new ComponentPlan(Kind.COMPOUND_TAG), "readCompoundTagW", CompoundTag.class, "writeCompoundTagW", CompoundTag.class);
        if (rawType == Tag.class) return bindStatic(new ComponentPlan(Kind.TAG), "readTagW", Tag.class, "writeTagW", Tag.class);

        if (rawType.isEnum()) {
            ComponentPlan p = new ComponentPlan(Kind.ENUM);
            p.enumClass = (Class<? extends Enum>) rawType;
            try {
                var lookup = MethodHandles.lookup();
                var r = lookup.findStatic(ComponentIO.class, "readEnumW", MethodType.methodType(Enum.class, RegistryFriendlyByteBuf.class, Class.class));
                p.readHandle = MethodHandles.insertArguments(r, 1, p.enumClass);
                p.writeHandle = lookup.findStatic(ComponentIO.class, "writeEnumW", MethodType.methodType(void.class, RegistryFriendlyByteBuf.class, Enum.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
            return p;
        }

        if (genericType instanceof ParameterizedType pt) {
            var raw = pt.getRawType();
            if (raw == Optional.class) {
                var arg = pt.getActualTypeArguments()[0];
                var argRaw = erasureOf(arg);
                ComponentPlan p = new ComponentPlan(Kind.OPTIONAL);
                p.element = planOf(argRaw, arg);
                return p;
            }
            if (raw instanceof Class<?> rawClass) {
                if (List.class.isAssignableFrom(rawClass)) {
                    var elemType = pt.getActualTypeArguments()[0];
                    var elemRaw = erasureOf(elemType);
                    ComponentPlan p = new ComponentPlan(Kind.LIST);
                    p.element = planOf(elemRaw, elemType);
                    return p;
                }
                if (Set.class.isAssignableFrom(rawClass)) {
                    var elemType = pt.getActualTypeArguments()[0];
                    var elemRaw = erasureOf(elemType);
                    ComponentPlan p = new ComponentPlan(Kind.SET);
                    p.element = planOf(elemRaw, elemType);
                    return p;
                }
                if (Map.class.isAssignableFrom(rawClass)) {
                    var args = pt.getActualTypeArguments();
                    if (args.length == 2) {
                        var keyType = args[0];
                        var valueType = args[1];
                        var keyRaw = erasureOf(keyType);
                        var valueRaw = erasureOf(valueType);
                        ComponentPlan p = new ComponentPlan(Kind.MAP);
                        p.key = planOf(keyRaw, keyType);
                        p.value = planOf(valueRaw, valueType);
                        return p;
                    }
                }
                if (rawClass == EnumSet.class) {
                    var arg = pt.getActualTypeArguments()[0];
                    var argRaw = erasureOf(arg);
                    if (!Enum.class.isAssignableFrom(argRaw)) {
                        throw new IllegalStateException("EnumSet element type must be an enum");
                    }
                    ComponentPlan p = new ComponentPlan(Kind.ENUM_SET);
                    p.enumClass = (Class<? extends Enum>) argRaw;
                    return p;
                }
            }
        }

        throw new IllegalStateException("Unsupported type " + rawType.getName());
    }

    static Class<?> erasureOf(Type type) {
        if (type instanceof Class<?> c) {
            return c;
        }
        if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> c) {
            return c;
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    static <E extends Enum<E>> EnumSet<E> readEnumSetGeneric(RegistryFriendlyByteBuf buf, Class<? extends Enum> enumClassRaw) {
        Class<E> ec = (Class<E>) enumClassRaw;
        return buf.readEnumSet(ec);
    }

    static <E extends Enum<E>> void writeEnumSetGeneric(RegistryFriendlyByteBuf buf, EnumSet<?> setRaw, Class<? extends Enum> enumClassRaw) {
        EnumSet<E> set = (EnumSet<E>) setRaw;
        Class<E> ec = (Class<E>) enumClassRaw;
        buf.writeEnumSet(set, ec);
    }

    static <T> List<T> readListGeneric(RegistryFriendlyByteBuf buf, ComponentPlan elemPlan, int depth, String name, Class<?> owner) {
        return buf.readList(
                b -> (T) decodeWithPlan((RegistryFriendlyByteBuf) b, elemPlan, depth + 1, name, owner)
        );
    }

    static <T> Set<T> readSetGeneric(RegistryFriendlyByteBuf buf, ComponentPlan elemPlan, int depth, String name, Class<?> owner) {
        return buf.readCollection(
                LinkedHashSet::new,
                b -> (T) decodeWithPlan((RegistryFriendlyByteBuf) b, elemPlan, depth + 1, name, owner)
        );
    }

    static <K, V> Map<K, V> readMapGeneric(RegistryFriendlyByteBuf buf, ComponentPlan keyPlan, ComponentPlan valuePlan, int depth, String name, Class<?> owner) {
        return buf.readMap(
                b -> (K) decodeWithPlan((RegistryFriendlyByteBuf) b, keyPlan, depth + 1, name, owner),
                b -> (V) decodeWithPlan((RegistryFriendlyByteBuf) b, valuePlan, depth + 1, name, owner)
        );
    }

    static void writeCollectionGeneric(RegistryFriendlyByteBuf buf, Collection<?> collection, ComponentPlan elemPlan, int depth, String name, Class<?> owner) {
        buf.writeCollection(collection,
                (b, e) -> encodeWithPlan((RegistryFriendlyByteBuf) b, elemPlan, e, depth + 1, name, owner)
        );
    }

    static void writeMapGeneric(RegistryFriendlyByteBuf buf, Map<?, ?> map, ComponentPlan keyPlan, ComponentPlan valuePlan, int depth, String name, Class<?> owner) {
        buf.writeMap(map,
                (b, k) -> encodeWithPlan((RegistryFriendlyByteBuf) b, keyPlan, k, depth + 1, name, owner),
                (b, v) -> encodeWithPlan((RegistryFriendlyByteBuf) b, valuePlan, v, depth + 1, name, owner)
        );
    }

    enum Kind {
        INT, LONG, BOOLEAN, FLOAT, DOUBLE, BYTE, SHORT,
        STRING, UUID, BYTE_ARRAY, INT_ARRAY, LONG_ARRAY, DATE, INSTANT, BITSET,
        BLOCK_POS, CHUNK_POS, SECTION_POS, GLOBAL_POS, VEC3, VECTOR3F, QUATERNIONF, RESOURCE_LOCATION, BLOCK_HIT_RESULT,
        COMPOUND_TAG, TAG, ENUM,
        OPTIONAL, LIST, SET, MAP, ENUM_SET,
        INT_LIST, RESOURCE_KEY
    }

    static final class ComponentPlan {
        final Kind kind;
        ComponentPlan element;
        ComponentPlan key;
        ComponentPlan value;
        Class<? extends Enum> enumClass;
        MethodHandle readHandle;
        MethodHandle writeHandle;

        ComponentPlan(Kind k) {
            this.kind = k;
        }
    }

    private static ComponentPlan bindStatic(ComponentPlan p, String readName, Class<?> readType, String writeName, Class<?> writeArgType) {
        try {
            var lookup = MethodHandles.lookup();
            p.readHandle = lookup.findStatic(ComponentIO.class, readName, MethodType.methodType(readType, RegistryFriendlyByteBuf.class));
            p.writeHandle = lookup.findStatic(ComponentIO.class, writeName, MethodType.methodType(void.class, RegistryFriendlyByteBuf.class, writeArgType));
            return p;
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    static int           readVarIntW         (RegistryFriendlyByteBuf buf) { return buf.readVarInt(); }
    static void          writeVarIntW        (RegistryFriendlyByteBuf buf, int v) { buf.writeVarInt(v); }
    static long          readVarLongW        (RegistryFriendlyByteBuf buf) { return buf.readVarLong(); }
    static void          writeVarLongW       (RegistryFriendlyByteBuf buf, long v) { buf.writeVarLong(v); }
    static boolean       readBooleanW        (RegistryFriendlyByteBuf buf) { return buf.readBoolean(); }
    static void          writeBooleanW       (RegistryFriendlyByteBuf buf, boolean v) { buf.writeBoolean(v); }
    static float         readFloatW          (RegistryFriendlyByteBuf buf) { return buf.readFloat(); }
    static void          writeFloatW         (RegistryFriendlyByteBuf buf, float v) { buf.writeFloat(v); }
    static double        readDoubleW         (RegistryFriendlyByteBuf buf) { return buf.readDouble(); }
    static void          writeDoubleW        (RegistryFriendlyByteBuf buf, double v) { buf.writeDouble(v); }
    static byte          readByteW           (RegistryFriendlyByteBuf buf) { return buf.readByte(); }
    static void          writeByteW          (RegistryFriendlyByteBuf buf, byte v) { buf.writeByte(v); }
    static short         readShortW          (RegistryFriendlyByteBuf buf) { return buf.readShort(); }
    static void          writeShortW         (RegistryFriendlyByteBuf buf, short v) { buf.writeShort(v); }
    static String        readUtfW            (RegistryFriendlyByteBuf buf) { return buf.readUtf(); }
    static void          writeUtfW           (RegistryFriendlyByteBuf buf, String v) { buf.writeUtf(v); }
    static UUID          readUUIDW           (RegistryFriendlyByteBuf buf) { return buf.readUUID(); }
    static void          writeUUIDW          (RegistryFriendlyByteBuf buf, UUID v) { buf.writeUUID(v); }
    static byte[]        readByteArrayW      (RegistryFriendlyByteBuf buf) { return buf.readByteArray(); }
    static void          writeByteArrayW     (RegistryFriendlyByteBuf buf, byte[] v) { buf.writeByteArray(v); }
    static int[]         readVarIntArrayW    (RegistryFriendlyByteBuf buf) { return buf.readVarIntArray(); }
    static void          writeVarIntArrayW   (RegistryFriendlyByteBuf buf, int[] v) { buf.writeVarIntArray(v); }
    static long[]        readLongArrayW      (RegistryFriendlyByteBuf buf) { return buf.readLongArray(); }
    static void          writeLongArrayW     (RegistryFriendlyByteBuf buf, long[] v) { buf.writeLongArray(v); }
    static Date          readDateW           (RegistryFriendlyByteBuf buf) { return buf.readDate(); }
    static void          writeDateW          (RegistryFriendlyByteBuf buf, Date v) { buf.writeDate(v); }
    static Instant       readInstantW        (RegistryFriendlyByteBuf buf) { return buf.readInstant(); }
    static void          writeInstantW       (RegistryFriendlyByteBuf buf, Instant v) { buf.writeInstant(v); }
    static BitSet        readBitSetW         (RegistryFriendlyByteBuf buf) { return buf.readBitSet(); }
    static void          writeBitSetW        (RegistryFriendlyByteBuf buf, BitSet v) { buf.writeBitSet(v); }
    static BlockPos      readBlockPosW       (RegistryFriendlyByteBuf buf) { return buf.readBlockPos(); }
    static void          writeBlockPosW      (RegistryFriendlyByteBuf buf, BlockPos v) { buf.writeBlockPos(v); }
    static ChunkPos      readChunkPosW       (RegistryFriendlyByteBuf buf) { return buf.readChunkPos(); }
    static void          writeChunkPosW      (RegistryFriendlyByteBuf buf, ChunkPos v) { buf.writeChunkPos(v); }
    static SectionPos    readSectionPosW     (RegistryFriendlyByteBuf buf) { return buf.readSectionPos(); }
    static void          writeSectionPosW    (RegistryFriendlyByteBuf buf, SectionPos v) { buf.writeSectionPos(v); }
    static GlobalPos     readGlobalPosW      (RegistryFriendlyByteBuf buf) { return buf.readGlobalPos(); }
    static void          writeGlobalPosW     (RegistryFriendlyByteBuf buf, GlobalPos v) { buf.writeGlobalPos(v); }
    static Vec3          readVec3W           (RegistryFriendlyByteBuf buf) { return buf.readVec3(); }
    static void          writeVec3W          (RegistryFriendlyByteBuf buf, Vec3 v) { buf.writeVec3(v); }
    static Vector3f      readVector3fW       (RegistryFriendlyByteBuf buf) { return buf.readVector3f(); }
    static void          writeVector3fW      (RegistryFriendlyByteBuf buf, Vector3f v) { buf.writeVector3f(v); }
    static Quaternionf   readQuaternionW     (RegistryFriendlyByteBuf buf) { return buf.readQuaternion(); }
    static void          writeQuaternionW    (RegistryFriendlyByteBuf buf, Quaternionf v) { buf.writeQuaternion(v); }
    static ResourceLocation readResourceLocationW (RegistryFriendlyByteBuf buf) { return buf.readResourceLocation(); }
    static void          writeResourceLocationW   (RegistryFriendlyByteBuf buf, ResourceLocation v) { buf.writeResourceLocation(v); }
    static BlockHitResult readBlockHitResultW(RegistryFriendlyByteBuf buf) { return buf.readBlockHitResult(); }
    static void          writeBlockHitResultW(RegistryFriendlyByteBuf buf, BlockHitResult v) { buf.writeBlockHitResult(v); }
    static CompoundTag   readCompoundTagW    (RegistryFriendlyByteBuf buf) { return buf.readNbt(); }
    static void          writeCompoundTagW   (RegistryFriendlyByteBuf buf, CompoundTag v) { buf.writeNbt(v); }
    static Tag           readTagW            (RegistryFriendlyByteBuf buf) { return buf.readNbt(NbtAccounter.unlimitedHeap()); }
    static void          writeTagW           (RegistryFriendlyByteBuf buf, Tag v) { buf.writeNbt(v); }
    static Enum<?>       readEnumW           (RegistryFriendlyByteBuf buf, Class<? extends Enum<?>> enumClass) { return buf.readEnum((Class) enumClass); }
    static void          writeEnumW          (RegistryFriendlyByteBuf buf, Enum<?> v) { buf.writeEnum(v); }
    static IntList       readIntIdListW      (RegistryFriendlyByteBuf buf) { return buf.readIntIdList(); }
    static void          writeIntIdListW     (RegistryFriendlyByteBuf buf, IntList v) { buf.writeIntIdList(v); }
    static <T> ResourceKey<T> readResourceKeyW(RegistryFriendlyByteBuf buf, ResourceKey<? extends Registry<T>> resourceKey) { return buf.readResourceKey(resourceKey); }
    static void          writeResourceKeyW   (RegistryFriendlyByteBuf buf, ResourceKey<?> resourceKey) { buf.writeResourceKey(resourceKey); }
    static <T> ResourceKey<? extends Registry<T>> readRegistryKeyW(RegistryFriendlyByteBuf buf) { return buf.readRegistryKey(); }
}