package cc.sighs.oelib.network.serialization;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.classfile.ClassFile;
import java.lang.classfile.CodeBuilder;
import java.lang.classfile.TypeKind;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates a {@link StreamCodec} implementation for a specific record type.
 *
 * <p>The generated codec is a hidden class, cached by {@link NetworkSerialization#autoCodec(Class)}.
 * If generation fails for any reason, callers fall back to the legacy reflection builder.</p>
 *
 * <p>Implementation note: this generator only inlines common leaf component types. Container types
 * such as {@code Optional<T>}, {@code List<T>} and {@code Map<K,V>} are intentionally not expanded
 * into specialized bytecode; they continue to use {@link ComponentIO}'s plan-based implementation.</p>
 *
 * <p>Runtime switches:</p>
 * <ul>
 *     <li>{@code -Doelib.network.codegen=false} disables generation (always uses legacy builder)</li>
 *     <li>{@code -Doelib.network.codegen.maxClasses=N} limits hidden classes created (default: 4096)</li>
 * </ul>
 */
final class ClassFileCodecGenerator {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private static final boolean ENABLED =
            Boolean.parseBoolean(System.getProperty("oelib.network.codegen", "true"));

    private static final int MAX_CLASSES = parseIntProperty("oelib.network.codegen.maxClasses", 4096);

    private static volatile boolean disabledAfterOome = false;

    private ClassFileCodecGenerator() {
    }

    @SuppressWarnings("unchecked")
    static <T> StreamCodec<RegistryFriendlyByteBuf, T> tryGenerate(Class<T> recordClass) {
        return tryGenerate(MethodHandles.lookup(), recordClass);
    }

    @SuppressWarnings("unchecked")
    static <T> StreamCodec<RegistryFriendlyByteBuf, T> tryGenerate(MethodHandles.Lookup lookup, Class<T> recordClass) {
        if (!ENABLED || disabledAfterOome) {
            return null;
        }
        Objects.requireNonNull(lookup, "lookup");
        Objects.requireNonNull(recordClass, "recordClass");
        if (!recordClass.isRecord()) {
            return null;
        }
        if (!Modifier.isPublic(recordClass.getModifiers())) {
            // Generated class lives in our package, so it can't access non-public record members.
            return null;
        }
        if (COUNTER.get() >= MAX_CLASSES) {
            return null;
        }

        final RecordComponent[] components = recordClass.getRecordComponents();
        final StreamCodec<RegistryFriendlyByteBuf, Object>[] customCodecs =
                (StreamCodec<RegistryFriendlyByteBuf, Object>[]) new StreamCodec<?, ?>[components.length];
        final ComponentIO.ComponentPlan[] plans = new ComponentIO.ComponentPlan[components.length];
        final ComponentIO.Kind[] kinds = new ComponentIO.Kind[components.length];
        final boolean[] hasCustom = new boolean[components.length];

        for (int i = 0; i < components.length; i++) {
            var component = components[i];
            customCodecs[i] = CustomCodecResolver.resolve(recordClass, component);
            hasCustom[i] = customCodecs[i] != null;
            if (customCodecs[i] == null) {
                plans[i] = ComponentIO.planOf(component.getType(), component.getGenericType());
                kinds[i] = plans[i].kind;
            }
        }

        try {
            final int id = COUNTER.incrementAndGet();
            if (id > MAX_CLASSES) {
                return null;
            }

            byte[] bytes = buildClassBytes(id, recordClass, components, hasCustom, kinds);

            // Hidden classes avoid name collisions and reduce classloader pollution.
            var hiddenLookup = lookup.defineHiddenClass(bytes, true);
            Class<?> generatedClass = hiddenLookup.lookupClass();

            var ctor = hiddenLookup.findConstructor(generatedClass,
                    MethodType.methodType(void.class, StreamCodec[].class, ComponentIO.ComponentPlan[].class));
            Object instance = ctor.invoke((Object) customCodecs, (Object) plans);
            return (StreamCodec<RegistryFriendlyByteBuf, T>) instance;
        } catch (OutOfMemoryError oom) {
            disabledAfterOome = true;
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private static <T> byte[] buildClassBytes(
            int id,
            Class<T> recordClass,
            RecordComponent[] components,
            boolean[] hasCustom,
            ComponentIO.Kind[] kinds
    ) {
        // This name is only used inside the hidden classfile; it does not leak into the class namespace.
        final ClassDesc thisClass = ClassDesc.of("cc.sighs.oelib.network.serialization.OELib$GenCodec$" + id);

        final ClassDesc cdObject = ConstantDescs.CD_Object;
        final ClassDesc cdStreamCodec = ClassDesc.of("net.minecraft.network.codec.StreamCodec");
        final ClassDesc cdRegistryFriendlyByteBuf = ClassDesc.of("net.minecraft.network.RegistryFriendlyByteBuf");
        final ClassDesc cdComponentIo = ClassDesc.of("cc.sighs.oelib.network.serialization.ComponentIO");
        final ClassDesc cdComponentPlan = ClassDesc.of("cc.sighs.oelib.network.serialization.ComponentIO$ComponentPlan");

        final ClassDesc streamCodecArray = cdStreamCodec.arrayType();
        final ClassDesc planArray = cdComponentPlan.arrayType();

        final MethodTypeDesc mtdVoid = ConstantDescs.MTD_void;
        final MethodTypeDesc mtdCtor = MethodTypeDesc.of(ConstantDescs.CD_void, streamCodecArray, planArray);

        // StreamDecoder<I, T>#decode(I) erases to decode(Object), and
        // StreamEncoder<O, T>#encode(O, T) erases to encode(Object, Object).
        final MethodTypeDesc mtdDecodeGeneric = MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_Object);
        final MethodTypeDesc mtdEncodeGeneric = MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_Object, ConstantDescs.CD_Object);

        final MethodTypeDesc mtdDecodeWithPlan = MethodTypeDesc.of(
                ConstantDescs.CD_Object,
                cdRegistryFriendlyByteBuf,
                cdComponentPlan,
                ConstantDescs.CD_int,
                ConstantDescs.CD_String,
                ConstantDescs.CD_Class
        );
        final MethodTypeDesc mtdEncodeWithPlan = MethodTypeDesc.of(
                ConstantDescs.CD_void,
                cdRegistryFriendlyByteBuf,
                cdComponentPlan,
                ConstantDescs.CD_Object,
                ConstantDescs.CD_int,
                ConstantDescs.CD_String,
                ConstantDescs.CD_Class
        );

        final ClassDesc recordDesc = recordClass.describeConstable().orElseThrow();
        final ClassDesc recordClassConst = recordClass.describeConstable().orElseThrow();

        MethodTypeDesc recordCtor = MethodTypeDesc.of(ConstantDescs.CD_void,
                Arrays.stream(components)
                        .map(RecordComponent::getType)
                        .map(ClassFileCodecGenerator::classDescOf)
                        .toArray(ClassDesc[]::new));

        return ClassFile.of().build(thisClass, clb -> clb
                .withVersion(ClassFile.latestMajorVersion(), 0)
                .withFlags(ClassFile.ACC_PUBLIC | ClassFile.ACC_FINAL | ClassFile.ACC_SUPER)
                .withSuperclass(cdObject)
                .withInterfaceSymbols(cdStreamCodec)
                .withField("codecs", streamCodecArray, ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL)
                .withField("plans", planArray, ClassFile.ACC_PRIVATE | ClassFile.ACC_FINAL)
                .withMethodBody(ConstantDescs.INIT_NAME, mtdCtor, ClassFile.ACC_PUBLIC, cob -> cob
                        .aload(0)
                        .invokespecial(cdObject, ConstantDescs.INIT_NAME, mtdVoid)
                        .aload(0)
                        .aload(cob.parameterSlot(0))
                        .putfield(thisClass, "codecs", streamCodecArray)
                        .aload(0)
                        .aload(cob.parameterSlot(1))
                        .putfield(thisClass, "plans", planArray)
                        .return_()
                )
                .withMethodBody("decode", mtdDecodeGeneric, ClassFile.ACC_PUBLIC, cob -> {
                    final int bufSlot = cob.allocateLocal(TypeKind.REFERENCE);
                    cob.aload(cob.parameterSlot(0))
                            .checkcast(cdRegistryFriendlyByteBuf)
                            .astore(bufSlot);

                    final int[] valueSlots = new int[components.length];
                    for (int i = 0; i < components.length; i++) {
                        Class<?> t = components[i].getType();
                        var kind = typeKindOf(t);
                        int slot = cob.allocateLocal(kind);
                        valueSlots[i] = slot;

                        if (hasCustom[i]) {
                            cob.aload(0)
                                    .getfield(thisClass, "codecs", streamCodecArray)
                                    .loadConstant(i)
                                    .aaload()
                                    .aload(bufSlot)
                                    .invokeinterface(cdStreamCodec, "decode", mtdDecodeGeneric);
                        } else if (kinds[i] == ComponentIO.Kind.RECORD) {
                            cob.aload(0)
                                    .getfield(thisClass, "plans", planArray)
                                    .loadConstant(i)
                                    .aaload()
                                    .getfield(cdComponentPlan, "codec", cdStreamCodec)
                                    .aload(bufSlot)
                                    .invokeinterface(cdStreamCodec, "decode", mtdDecodeGeneric);
                        } else if (isInlineableLeafKind(kinds[i]) && (kinds[i] != ComponentIO.Kind.ENUM || t.isEnum())) {
                            emitInlineLeafDecode(cob, cdComponentIo, cdRegistryFriendlyByteBuf, kinds[i], t, bufSlot);
                        } else {
                            cob.aload(bufSlot)
                                    .aload(0)
                                    .getfield(thisClass, "plans", planArray)
                                    .loadConstant(i)
                                    .aaload()
                                    .loadConstant(0)
                                    .ldc(components[i].getName())
                                    .ldc(recordClassConst)
                                    .invokestatic(cdComponentIo, "decodeWithPlan", mtdDecodeWithPlan);
                        }

                        emitLeafPostDecodeConversion(cob, kinds[i], t);
                        emitStore(cob, kind, slot);
                    }

                    cob.new_(recordDesc)
                            .dup();
                    for (int i = 0; i < components.length; i++) {
                        emitLoad(cob, typeKindOf(components[i].getType()), valueSlots[i]);
                    }
                    cob.invokespecial(recordDesc, ConstantDescs.INIT_NAME, recordCtor)
                            .areturn();
                })
                .withMethodBody("encode", mtdEncodeGeneric, ClassFile.ACC_PUBLIC, cob -> {
                    final int bufSlot = cob.allocateLocal(TypeKind.REFERENCE);
                    final int valueSlot = cob.allocateLocal(TypeKind.REFERENCE);

                    cob.aload(cob.parameterSlot(0))
                            .checkcast(cdRegistryFriendlyByteBuf)
                            .astore(bufSlot);

                    cob.aload(cob.parameterSlot(1))
                            .checkcast(recordDesc)
                            .astore(valueSlot);

                    for (int i = 0; i < components.length; i++) {
                        Class<?> t = components[i].getType();
                        MethodTypeDesc accessorType = MethodTypeDesc.of(classDescOf(t));

                        if (hasCustom[i]) {
                            cob.aload(0)
                                    .getfield(thisClass, "codecs", streamCodecArray)
                                    .loadConstant(i)
                                    .aaload()
                                    .aload(bufSlot)
                                    .aload(valueSlot)
                                    .invokevirtual(recordDesc, components[i].getName(), accessorType);
                            emitBoxIfNeeded(cob, t);
                            cob.invokeinterface(cdStreamCodec, "encode", mtdEncodeGeneric);
                        } else if (kinds[i] == ComponentIO.Kind.RECORD) {
                            cob.aload(0)
                                    .getfield(thisClass, "plans", planArray)
                                    .loadConstant(i)
                                    .aaload()
                                    .getfield(cdComponentPlan, "codec", cdStreamCodec)
                                    .aload(bufSlot)
                                    .aload(valueSlot)
                                    .invokevirtual(recordDesc, components[i].getName(), accessorType)
                                    .invokeinterface(cdStreamCodec, "encode", mtdEncodeGeneric);
                        } else if (isInlineableLeafKind(kinds[i]) && (kinds[i] != ComponentIO.Kind.ENUM || t.isEnum())) {
                            emitInlineLeafEncode(cob, cdComponentIo, cdRegistryFriendlyByteBuf, kinds[i], t, bufSlot, valueSlot,
                                    recordDesc, components[i].getName(), accessorType);
                        } else {
                            cob.aload(bufSlot)
                                    .aload(0)
                                    .getfield(thisClass, "plans", planArray)
                                    .loadConstant(i)
                                    .aaload()
                                    .aload(valueSlot)
                                    .invokevirtual(recordDesc, components[i].getName(), accessorType);
                            emitBoxIfNeeded(cob, t);
                            cob.loadConstant(0)
                                    .ldc(components[i].getName())
                                    .ldc(recordClassConst)
                                    .invokestatic(cdComponentIo, "encodeWithPlan", mtdEncodeWithPlan);
                        }
                    }
                    cob.return_();
                })
        );
    }

    private static boolean isInlineableLeafKind(ComponentIO.Kind kind) {
        if (kind == null) return false;
        return switch (kind) {
            case INT, LONG, BOOLEAN, FLOAT, DOUBLE, BYTE, SHORT,
                 STRING, UUID, BYTE_ARRAY, INT_ARRAY, LONG_ARRAY, INSTANT, BITSET, PUBLIC_KEY,
                 BLOCK_POS, CHUNK_POS, GLOBAL_POS, VECTOR3F, QUATERNIONF, RESOURCE_LOCATION, BLOCK_HIT_RESULT,
                 COMPOUND_TAG, TAG, ENUM, INT_LIST, RESOURCE_KEY -> true;
            default -> false;
        };
    }

    private static void emitInlineLeafDecode(
            CodeBuilder cob,
            ClassDesc cdComponentIo,
            ClassDesc cdRegistryFriendlyByteBuf,
            ComponentIO.Kind kind,
            Class<?> componentType,
            int bufSlot
    ) {
        // All wrapper methods are package-private static on ComponentIO, and return either a primitive or the target type.
        cob.aload(bufSlot);
        if (kind == ComponentIO.Kind.ENUM && componentType.isEnum()) {
            cob.ldc(classDescOf(componentType));
        }
        cob.invokestatic(cdComponentIo, leafReadMethodName(kind), leafReadMethodType(kind, cdRegistryFriendlyByteBuf));

        // If the record component is a wrapper type, convert the primitive return to its boxed form.
        if (!componentType.isPrimitive()) {
            Class<?> primitive = primitiveForLeafKind(kind);
            if (primitive != null) {
                emitBoxIfNeeded(cob, primitive);
            }
        }
    }

    private static void emitInlineLeafEncode(
            CodeBuilder cob,
            ClassDesc cdComponentIo,
            ClassDesc cdRegistryFriendlyByteBuf,
            ComponentIO.Kind kind,
            Class<?> componentType,
            int bufSlot,
            int valueSlot,
            ClassDesc recordDesc,
            String accessorName,
            MethodTypeDesc accessorType
    ) {
        cob.aload(bufSlot)
                .aload(valueSlot)
                .invokevirtual(recordDesc, accessorName, accessorType);

        Class<?> primitive = primitiveForLeafKind(kind);
        if (primitive != null && !componentType.isPrimitive()) {
            emitCastOrUnbox(cob, primitive);
        }

        cob.invokestatic(cdComponentIo, leafWriteMethodName(kind), leafWriteMethodType(kind, cdRegistryFriendlyByteBuf));
    }

    private static void emitLeafPostDecodeConversion(CodeBuilder cob, ComponentIO.Kind kind, Class<?> target) {
        // Inline decode paths return either primitives, or values already in the correct reference type.
        // For plan/custom decode paths we still need casts/unboxing.
        if (isInlineableLeafKind(kind)) {
            if (kind == ComponentIO.Kind.ENUM && target.isEnum()) {
                cob.checkcast(classDescOf(target));
            }
            return;
        }
        if (kind == ComponentIO.Kind.RECORD) {
            cob.checkcast(classDescOf(target));
            return;
        }
        emitCastOrUnbox(cob, target);
    }

    private static String leafReadMethodName(ComponentIO.Kind kind) {
        return switch (kind) {
            case INT -> "readVarIntW";
            case LONG -> "readVarLongW";
            case BOOLEAN -> "readBooleanW";
            case FLOAT -> "readFloatW";
            case DOUBLE -> "readDoubleW";
            case BYTE -> "readByteW";
            case SHORT -> "readShortW";
            case STRING -> "readUtfW";
            case UUID -> "readUUIDW";
            case BYTE_ARRAY -> "readByteArrayW";
            case INT_ARRAY -> "readVarIntArrayW";
            case LONG_ARRAY -> "readLongArrayW";
            case INSTANT -> "readInstantW";
            case BITSET -> "readBitSetW";
            case PUBLIC_KEY -> "readPublicKeyW";
            case INT_LIST -> "readIntIdListW";
            case RESOURCE_KEY -> "readRegistryKeyW";
            case BLOCK_POS -> "readBlockPosW";
            case CHUNK_POS -> "readChunkPosW";
            case GLOBAL_POS -> "readGlobalPosW";
            case VECTOR3F -> "readVector3fW";
            case QUATERNIONF -> "readQuaternionW";
            case RESOURCE_LOCATION -> "readIdentifierW";
            case BLOCK_HIT_RESULT -> "readBlockHitResultW";
            case COMPOUND_TAG -> "readCompoundTagW";
            case TAG -> "readTagW";
            case ENUM -> "readEnumW";
            default -> throw new IllegalStateException("No leaf read wrapper for kind: " + kind);
        };
    }

    private static String leafWriteMethodName(ComponentIO.Kind kind) {
        return switch (kind) {
            case INT -> "writeVarIntW";
            case LONG -> "writeVarLongW";
            case BOOLEAN -> "writeBooleanW";
            case FLOAT -> "writeFloatW";
            case DOUBLE -> "writeDoubleW";
            case BYTE -> "writeByteW";
            case SHORT -> "writeShortW";
            case STRING -> "writeUtfW";
            case UUID -> "writeUUIDW";
            case BYTE_ARRAY -> "writeByteArrayW";
            case INT_ARRAY -> "writeVarIntArrayW";
            case LONG_ARRAY -> "writeLongArrayW";
            case INSTANT -> "writeInstantW";
            case BITSET -> "writeBitSetW";
            case PUBLIC_KEY -> "writePublicKeyW";
            case INT_LIST -> "writeIntIdListW";
            case RESOURCE_KEY -> "writeResourceKeyW";
            case BLOCK_POS -> "writeBlockPosW";
            case CHUNK_POS -> "writeChunkPosW";
            case GLOBAL_POS -> "writeGlobalPosW";
            case VECTOR3F -> "writeVector3fW";
            case QUATERNIONF -> "writeQuaternionW";
            case RESOURCE_LOCATION -> "writeIdentifierW";
            case BLOCK_HIT_RESULT -> "writeBlockHitResultW";
            case COMPOUND_TAG -> "writeCompoundTagW";
            case TAG -> "writeTagW";
            case ENUM -> "writeEnumW";
            default -> throw new IllegalStateException("No leaf write wrapper for kind: " + kind);
        };
    }

    private static MethodTypeDesc leafReadMethodType(ComponentIO.Kind kind, ClassDesc cdRegistryFriendlyByteBuf) {
        if (kind == ComponentIO.Kind.ENUM) {
            return MethodTypeDesc.of(classDescOf(Enum.class), cdRegistryFriendlyByteBuf, ConstantDescs.CD_Class);
        }
        return MethodTypeDesc.of(classDescOf(leafReadReturnType(kind)), cdRegistryFriendlyByteBuf);
    }

    private static MethodTypeDesc leafWriteMethodType(ComponentIO.Kind kind, ClassDesc cdRegistryFriendlyByteBuf) {
        if (kind == ComponentIO.Kind.ENUM) {
            return MethodTypeDesc.of(ConstantDescs.CD_void, cdRegistryFriendlyByteBuf, classDescOf(Enum.class));
        }
        return MethodTypeDesc.of(ConstantDescs.CD_void, cdRegistryFriendlyByteBuf, classDescOf(leafWriteValueType(kind)));
    }

    private static Class<?> leafReadReturnType(ComponentIO.Kind kind) {
        return switch (kind) {
            case INT -> int.class;
            case LONG -> long.class;
            case BOOLEAN -> boolean.class;
            case FLOAT -> float.class;
            case DOUBLE -> double.class;
            case BYTE -> byte.class;
            case SHORT -> short.class;
            case STRING -> String.class;
            case UUID -> UUID.class;
            case BYTE_ARRAY -> byte[].class;
            case INT_ARRAY -> int[].class;
            case LONG_ARRAY -> long[].class;
            case INSTANT -> Instant.class;
            case BITSET -> BitSet.class;
            case PUBLIC_KEY -> PublicKey.class;
            case INT_LIST -> IntList.class;
            case RESOURCE_KEY -> ResourceKey.class;
            case BLOCK_POS -> BlockPos.class;
            case CHUNK_POS -> ChunkPos.class;
            case GLOBAL_POS -> GlobalPos.class;
            case VECTOR3F -> Vector3f.class;
            case QUATERNIONF -> Quaternionf.class;
            case RESOURCE_LOCATION -> Identifier.class;
            case BLOCK_HIT_RESULT -> BlockHitResult.class;
            case COMPOUND_TAG -> CompoundTag.class;
            case TAG -> Tag.class;
            default -> throw new IllegalStateException("Leaf return type not mapped for kind: " + kind);
        };
    }

    private static Class<?> leafWriteValueType(ComponentIO.Kind kind) {
        // Most wrappers accept the same type they read (or the primitive counterpart).
        return leafReadReturnType(kind);
    }

    private static Class<?> primitiveForLeafKind(ComponentIO.Kind kind) {
        return switch (kind) {
            case INT -> int.class;
            case LONG -> long.class;
            case BOOLEAN -> boolean.class;
            case FLOAT -> float.class;
            case DOUBLE -> double.class;
            case BYTE -> byte.class;
            case SHORT -> short.class;
            default -> null;
        };
    }

    private static ClassDesc classDescOf(Class<?> type) {
        return ClassDesc.ofDescriptor(type.descriptorString());
    }

    private static TypeKind typeKindOf(Class<?> type) {
        if (!type.isPrimitive()) return TypeKind.REFERENCE;
        if (type == long.class) return TypeKind.LONG;
        if (type == float.class) return TypeKind.FLOAT;
        if (type == double.class) return TypeKind.DOUBLE;
        return TypeKind.INT;
    }

    private static void emitLoad(CodeBuilder cob, TypeKind kind, int slot) {
        switch (kind) {
            case INT -> cob.iload(slot);
            case LONG -> cob.lload(slot);
            case FLOAT -> cob.fload(slot);
            case DOUBLE -> cob.dload(slot);
            default -> cob.aload(slot);
        }
    }

    private static void emitStore(CodeBuilder cob, TypeKind kind, int slot) {
        switch (kind) {
            case INT -> cob.istore(slot);
            case LONG -> cob.lstore(slot);
            case FLOAT -> cob.fstore(slot);
            case DOUBLE -> cob.dstore(slot);
            default -> cob.astore(slot);
        }
    }

    private static void emitCastOrUnbox(CodeBuilder cob, Class<?> target) {
        if (!target.isPrimitive()) {
            cob.checkcast(classDescOf(target));
            return;
        }
        if (target == int.class) {
            cob.checkcast(ClassDesc.of("java.lang.Integer"))
                    .invokevirtual(ClassDesc.of("java.lang.Integer"), "intValue",
                            MethodTypeDesc.of(ConstantDescs.CD_int));
            return;
        }
        if (target == long.class) {
            cob.checkcast(ClassDesc.of("java.lang.Long"))
                    .invokevirtual(ClassDesc.of("java.lang.Long"), "longValue",
                            MethodTypeDesc.of(ConstantDescs.CD_long));
            return;
        }
        if (target == boolean.class) {
            cob.checkcast(ClassDesc.of("java.lang.Boolean"))
                    .invokevirtual(ClassDesc.of("java.lang.Boolean"), "booleanValue",
                            MethodTypeDesc.of(ConstantDescs.CD_boolean));
            return;
        }
        if (target == byte.class) {
            cob.checkcast(ClassDesc.of("java.lang.Byte"))
                    .invokevirtual(ClassDesc.of("java.lang.Byte"), "byteValue",
                            MethodTypeDesc.of(ConstantDescs.CD_byte));
            return;
        }
        if (target == short.class) {
            cob.checkcast(ClassDesc.of("java.lang.Short"))
                    .invokevirtual(ClassDesc.of("java.lang.Short"), "shortValue",
                            MethodTypeDesc.of(ConstantDescs.CD_short));
            return;
        }
        if (target == char.class) {
            cob.checkcast(ClassDesc.of("java.lang.Character"))
                    .invokevirtual(ClassDesc.of("java.lang.Character"), "charValue",
                            MethodTypeDesc.of(ConstantDescs.CD_char));
            return;
        }
        if (target == float.class) {
            cob.checkcast(ClassDesc.of("java.lang.Float"))
                    .invokevirtual(ClassDesc.of("java.lang.Float"), "floatValue",
                            MethodTypeDesc.of(ConstantDescs.CD_float));
            return;
        }
        if (target == double.class) {
            cob.checkcast(ClassDesc.of("java.lang.Double"))
                    .invokevirtual(ClassDesc.of("java.lang.Double"), "doubleValue",
                            MethodTypeDesc.of(ConstantDescs.CD_double));
            return;
        }
        throw new IllegalStateException("Unsupported primitive: " + target.getName());
    }

    private static void emitBoxIfNeeded(CodeBuilder cob, Class<?> type) {
        if (!type.isPrimitive()) return;

        if (type == int.class) {
            cob.invokestatic(ClassDesc.of("java.lang.Integer"), "valueOf",
                    MethodTypeDesc.of(ClassDesc.of("java.lang.Integer"), ConstantDescs.CD_int));
            return;
        }
        if (type == long.class) {
            cob.invokestatic(ClassDesc.of("java.lang.Long"), "valueOf",
                    MethodTypeDesc.of(ClassDesc.of("java.lang.Long"), ConstantDescs.CD_long));
            return;
        }
        if (type == boolean.class) {
            cob.invokestatic(ClassDesc.of("java.lang.Boolean"), "valueOf",
                    MethodTypeDesc.of(ClassDesc.of("java.lang.Boolean"), ConstantDescs.CD_boolean));
            return;
        }
        if (type == byte.class) {
            cob.invokestatic(ClassDesc.of("java.lang.Byte"), "valueOf",
                    MethodTypeDesc.of(ClassDesc.of("java.lang.Byte"), ConstantDescs.CD_byte));
            return;
        }
        if (type == short.class) {
            cob.invokestatic(ClassDesc.of("java.lang.Short"), "valueOf",
                    MethodTypeDesc.of(ClassDesc.of("java.lang.Short"), ConstantDescs.CD_short));
            return;
        }
        if (type == char.class) {
            cob.invokestatic(ClassDesc.of("java.lang.Character"), "valueOf",
                    MethodTypeDesc.of(ClassDesc.of("java.lang.Character"), ConstantDescs.CD_char));
            return;
        }
        if (type == float.class) {
            cob.invokestatic(ClassDesc.of("java.lang.Float"), "valueOf",
                    MethodTypeDesc.of(ClassDesc.of("java.lang.Float"), ConstantDescs.CD_float));
            return;
        }
        if (type == double.class) {
            cob.invokestatic(ClassDesc.of("java.lang.Double"), "valueOf",
                    MethodTypeDesc.of(ClassDesc.of("java.lang.Double"), ConstantDescs.CD_double));
            return;
        }
        throw new IllegalStateException("Unsupported primitive: " + type.getName());
    }

    private static int parseIntProperty(String key, int fallback) {
        String v = System.getProperty(key);
        if (v == null || v.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
