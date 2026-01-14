package cc.sighs.oelib.dev.example.net;

import cc.sighs.oelib.OELib;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BigPacketSender {

    private static final ScheduledExecutorService STRESS_TESTER = Executors.newSingleThreadScheduledExecutor();

    public static BigComponentPacket createMegabytePacket(int targetMb) {
        long startTime = System.nanoTime();

        byte[] megaBytes = new byte[targetMb * 1024 * 1024];
        new Random().nextBytes(megaBytes);

        BigComponentPacket packet = createBasePacket("SmallTestString", megaBytes);

        long endTime = System.nanoTime();
        OELib.LOGGER.info("[Performance] The MB-level packet is complete. Size: ~{} MB, Time: {} ms",
                targetMb, String.format("%.3f", (endTime - startTime) / 1_000_000.0));
        return packet;
    }

    public static void runStressTest(ServerPlayer player, int count, int intervalMs) {
        var sentCount = new AtomicInteger(0);
        OELib.LOGGER.info("[StressTest] Start Stress Test: {} packages in total, interval {}ms", count, intervalMs);

        STRESS_TESTER.scheduleAtFixedRate(() -> {
            try {
                int current = sentCount.incrementAndGet();
                if (current > count || player.connection.player.hasDisconnected()) {
                    OELib.LOGGER.info("[StressTest] Test ends or player disconnects.");
                    throw new CancellationException("Test Finished");
                }

                BigComponentPacket packet = createMegabytePacket(2);

                if (packet != null) {
                    player.server.execute(() -> {
                        try {
                            packet.sendTo(player);
                            OELib.LOGGER.info("[StressTest] The {}/{} packet has been sent", current, count);
                        } catch (Exception e) {
                            OELib.LOGGER.error("[StressTest] An error occurred while sending:", e);
                        }
                    });
                }
            } catch (CancellationException e) {
                throw e;
            } catch (Exception e) {

                OELib.LOGGER.error("[StressTest] build task crashes:", e);
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    public static BigComponentPacket createBasePacket(String customString, byte[] customBytes) {
        long startTime = System.nanoTime();
        OELib.LOGGER.info("[Performance] Start building very large packets...");

        try {

            List<String> largeList = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                largeList.add("String_" + i + "_" + generateRandomString(50));
            }

            Set<Integer> largeSet = new HashSet<>();
            for (int i = 0; i < 500; i++) {
                largeSet.add(i * 100);
            }

            Map<String, Long> largeMap = new HashMap<>();
            for (int i = 0; i < 300; i++) {
                largeMap.put("Key_" + i + "_" + generateRandomString(20), (long) i * 1000);
            }

            int[] largeIntArray = new int[500];
            for (int i = 0; i < largeIntArray.length; i++) {
                largeIntArray[i] = i * 100;
            }

            long[] largeLongArray = new long[400];
            for (int i = 0; i < largeLongArray.length; i++) {
                largeLongArray[i] = i * 1000L;
            }

            BitSet largeBitSet = new BitSet(1000);
            for (int i = 0; i < 1000; i++) {
                if (i % 3 == 0) {
                    largeBitSet.set(i);
                }
            }

            var keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(512);
            var publicKey = keyGen.generateKeyPair().getPublic();

            ObjectList<String> fastList = new ObjectArrayList<>();
            for (int i = 0; i < 800; i++) {
                fastList.add("FastUtil_String_" + i + "_" + generateRandomString(30));
            }

            ObjectSet<UUID> fastObjectSet = new ObjectOpenHashSet<>();
            for (int i = 0; i < 300; i++) {
                fastObjectSet.add(UUID.randomUUID());
            }

            IntSet fastIntSet = new IntOpenHashSet();
            for (int i = 0; i < 1000; i++) {
                fastIntSet.add(i * 10);
            }

            LongSet fastLongSet = new LongOpenHashSet();
            for (int i = 0; i < 600; i++) {
                fastLongSet.add(i * 100L);
            }

            Object2IntMap<String> fastO2I = new Object2IntOpenHashMap<>();
            for (int i = 0; i < 200; i++) {
                fastO2I.put("O2I_Key_" + i, i * 100);
            }

            Object2LongMap<String> fastO2L = new Object2LongOpenHashMap<>();
            for (int i = 0; i < 200; i++) {
                fastO2L.put("O2L_Key_" + i, i * 1000L);
            }

            Object2ObjectMap<String, UUID> fastO2O = new Object2ObjectOpenHashMap<>();
            for (int i = 0; i < 150; i++) {
                fastO2O.put("O2O_Key_" + i, UUID.randomUUID());
            }

            Int2ObjectMap<String> fastI2O = new Int2ObjectOpenHashMap<>();
            for (int i = 0; i < 400; i++) {
                fastI2O.put(i * 5, "I2O_Value_" + i);
            }

            Int2IntMap fastI2I = new Int2IntOpenHashMap();
            for (int i = 0; i < 500; i++) {
                fastI2I.put(i, i * 2);
            }

            Long2ObjectMap<String> fastL2O = new Long2ObjectOpenHashMap<>();
            for (int i = 0; i < 300; i++) {
                fastL2O.put(i * 100L, "L2O_Value_" + i);
            }

            IntList fastIntList = new IntArrayList();
            for (int i = 0; i < 1000; i++) {
                fastIntList.add(i * 3);
            }

            BigComponentPacket packet = new BigComponentPacket(
                    42,
                    4242,
                    99999999999L,
                    88888888888L,
                    true,
                    false,
                    3.14159f,
                    2.71828f,
                    1.41421356,
                    1.7320508,
                    (byte) 127,
                    (byte) -128,
                    (short) 32767,
                    (short) -32768,
                    customString,
                    UUID.randomUUID(),
                    new Date(),
                    Instant.now(),
                    largeBitSet,
                    publicKey,
                    customBytes,
                    largeIntArray,
                    largeLongArray,
                    new BlockPos(100, 64, -200),
                    new ChunkPos(10, -5),
                    SectionPos.of(new BlockPos(0, 64, 0)),
                    GlobalPos.of(
                            ResourceKey.create(
                                    Registries.DIMENSION,
                                    ResourceLocation.parse("overworld")
                            ),
                            new BlockPos(100, 70, 100)
                    ),
                    new Vec3(1.5, 2.0, -3.5),
                    new Vector3f(1.0f, 2.0f, 3.0f),
                    new Quaternionf(),
                    ResourceLocation.fromNamespaceAndPath(OELib.MODID, "test"),
                    ResourceKey.create(
                            Registries.BLOCK,
                            ResourceLocation.withDefaultNamespace("stone")
                    ),
                    new BlockHitResult(
                            new Vec3(0.5, 1.0, 0.5),
                            Direction.UP,
                            new BlockPos(0, 64, 0),
                            false
                    ),
                    createLargeCompoundTag(),
                    null,
                    BigComponentPacket.ExampleEnum.B,
                    new BigComponentPacket.NestedRecord(
                            "NestedA",
                            123,
                            ResourceLocation.fromNamespaceAndPath(OELib.MODID, "test_nested")
                    ),
                    Optional.of("OptionalString"),
                    Pair.of(42, "PairValue"),
                    org.apache.commons.lang3.tuple.Pair.of(123456789L, UUID.randomUUID()),
                    Either.left("EitherLeftValue"),
                    Triple.of("First", 2, 3L),
                    largeList,
                    largeSet,
                    largeMap,
                    EnumSet.allOf(BigComponentPacket.ExampleEnum.class),
                    fastList,
                    fastObjectSet,
                    fastIntSet,
                    fastLongSet,
                    fastO2I,
                    fastO2L,
                    fastO2O,
                    fastI2O,
                    fastI2I,
                    fastL2O,
                    fastIntList
            );
            long endTime = System.nanoTime();
            double duration = (endTime - startTime) / 1_000_000.0;
            OELib.LOGGER.info("[Performance] The large data packet is built. Time-consuming: {} ms, Estimated total number of fields: 50+",
                    String.format("%.3f", duration));

            return packet;
        } catch (Exception e) {
            OELib.LOGGER.error("Failed to create massive packet", e);
            return null;
        }
    }

    private static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static CompoundTag createLargeCompoundTag() {
        CompoundTag tag = new CompoundTag();

        for (int i = 0; i < 100; i++) {
            tag.putString("StringKey_" + i, "Value_" + i + "_" + generateRandomString(20));
            tag.putInt("IntKey_" + i, i * 100);
            tag.putLong("LongKey_" + i, i * 1000L);
            tag.putFloat("FloatKey_" + i, i * 1.5f);
            tag.putDouble("DoubleKey_" + i, i * 2.5);
            tag.putByteArray("ByteArrayKey_" + i, new byte[]{(byte) i, (byte) (i + 1), (byte) (i + 2)});

            if (i % 10 == 0) {
                CompoundTag nested = new CompoundTag();
                nested.putString("Nested", "Level2_" + i);
                tag.put("CompoundKey_" + i, nested);
            }
        }

        return tag;
    }
}