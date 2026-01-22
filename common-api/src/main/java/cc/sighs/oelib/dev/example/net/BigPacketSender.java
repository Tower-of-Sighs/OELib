package cc.sighs.oelib.dev.example.net;

import cc.sighs.oelib.OELib;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
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

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BigPacketSender {
    private static final ScheduledExecutorService STRESS_TESTER = Executors.newSingleThreadScheduledExecutor();

    private static final Stats STATS = new Stats();

    public static void runStressTest(ServerPlayer player, int count, int intervalMs) {
        STATS.initGcMonitor();
        int preCount = 20;
        int preInterval = Math.max(20, intervalMs / 2);
        long warmupDurationMs = (long) preCount * preInterval;
        for (int i = 0; i < preCount; i++) {
            int delay = i * preInterval;
            STRESS_TESTER.schedule(() -> sendOne(player, false, 2, 0, 0, 0), delay, TimeUnit.MILLISECONDS);
        }
        AtomicInteger sent = new AtomicInteger(0);
        for (int i = 0; i < count; i++) {
            int idx = i + 1;
            long delay = warmupDurationMs + (long) i * intervalMs;
            STRESS_TESTER.schedule(() -> {
                sendOne(player, true, 2, idx, count, intervalMs);
                if (sent.incrementAndGet() == count) {
                    STRESS_TESTER.schedule(BigPacketSender::printSummary, 500, TimeUnit.MILLISECONDS);
                }
            }, delay, TimeUnit.MILLISECONDS);
        }
    }

    private static void sendOne(ServerPlayer player, boolean collect, int targetMb, int idx, int total, int intervalMs) {
        BuildResult br = createMegabytePacket(targetMb);
        if (br == null || player == null) return;
        Runtime rt = Runtime.getRuntime();
        long usedBefore = rt.totalMemory() - rt.freeMemory();
        long enqueueNs = System.nanoTime();
        player.server.execute(() -> {
            try {
                br.packet.sendTo(player);
                long usedAfter = rt.totalMemory() - rt.freeMemory();
                double delayMs = (System.nanoTime() - enqueueNs) / 1_000_000.0;
                double totalMs = br.buildMs + delayMs;
                if (collect) {
                    STATS.recordSend(br.buildMs, totalMs, br.finalScore, usedBefore, usedAfter, br.firstBuild);
                }
                OELib.LOGGER.info("[Send] {}/{} size={}MB build={}ms total={}ms score={}",
                        idx, total,
                        String.format("%.3f", br.mbSize),
                        String.format("%.3f", br.buildMs),
                        String.format("%.3f", totalMs),
                        String.format("%.2f", br.finalScore));
            } catch (Throwable ignored) {
            }
        });
    }

    public static void recordReceive(double ms) {
        STATS.recordReceive(ms);
    }

    private static void printSummary() {
        String stability = STATS.stabilityLabel();
        OELib.LOGGER.info("[Summary] avgBuild={}ms avgTotal={}ms maxScore={} minScore={} avgScore={} firstBuildAvg={}ms steadyStateAvg={}ms stability={} avgRecv={}ms heapDelta={}MB stabilityScore={} gcCount={} minGCInterval={}ms maxGCPause={}ms heapVar={}MB^2(每包堆增量方差)",
                String.format("%.3f", STATS.avgBuild()),
                String.format("%.3f", STATS.avgTotal()),
                String.format("%.2f", STATS.maxScore),
                String.format("%.2f", STATS.minScore),
                String.format("%.2f", STATS.avgScore()),
                String.format("%.3f", STATS.firstBuildMs),
                String.format("%.3f", STATS.steadyAvgBuild()),
                stability,
                String.format("%.3f", STATS.avgReceive()),
                String.format("%.3f", STATS.heapDeltaMB()),
                String.format("%.2f", STATS.stabilityScore()),
                STATS.gcCount,
                String.format("%.2f", STATS.minGcIntervalMs == Double.POSITIVE_INFINITY ? 0.0 : STATS.minGcIntervalMs),
                String.format("%.2f", STATS.maxGcPauseMs),
                String.format("%.3f", STATS.deltaVarianceMB()));
        STATS.reset();
    }

    public static BuildResult createMegabytePacket(int targetMb) {
        byte[] megaBytes = new byte[targetMb * 1024 * 1024];
        new Random().nextBytes(megaBytes);
        return createBasePacket("SmallTestString", megaBytes);
    }

    private static BuildResult createBasePacket(String customString, byte[] customBytes) {
        long startNs = System.nanoTime();
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
//                    Items.ACACIA_BOAT.getDefaultInstance(),
//                    Component.literal("Test Component")
            );
            double buildMs = (System.nanoTime() - startNs) / 1_000_000.0;
            int fieldCount = 55;
            int complexStructures = 15;
            double mbSize = customBytes.length / (1024.0 * 1024.0);
            double scoreBase = (fieldCount * 1.2) + (complexStructures * 2.5) + (mbSize * 5.0);
            double efficiencyFactor = 1.0 / (1.0 + Math.pow(buildMs / 5.0, 2.0));
            double finalScore = scoreBase * efficiencyFactor;
            boolean first = STATS.markFirstIfNeeded(buildMs);
            return new BuildResult(packet, buildMs, finalScore, mbSize, first);
        } catch (Throwable t) {
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

    public record BuildResult(BigComponentPacket packet, double buildMs, double finalScore, double mbSize,
                              boolean firstBuild) {
    }

    private static final class Stats {
        private double buildSum;
        private double buildSqSum;
        private int buildCount;
        private double totalSum;
        private int totalCount;
        private double scoreSum;
        private int scoreCount;
        private double minScore = Double.POSITIVE_INFINITY;
        private double maxScore = Double.NEGATIVE_INFINITY;
        private double firstBuildMs = -1.0;
        private double recvSum;
        private int recvCount;
        private long minUsed = Long.MAX_VALUE;
        private long maxUsed = Long.MIN_VALUE;
        private double deltaSumMB;
        private double deltaSqSumMB;
        private int deltaCount;
        private boolean gcInitialized;
        private long lastGcEndMillis = -1L;
        private double minGcIntervalMs = Double.POSITIVE_INFINITY;
        private double maxGcPauseMs = 0.0;
        private int gcCount;

        synchronized boolean markFirstIfNeeded(double buildMs) {
            if (firstBuildMs < 0) {
                firstBuildMs = buildMs;
                return true;
            }
            return false;
        }

        synchronized void recordSend(double buildMs, double totalMs, double score, long usedBefore, long usedAfter, boolean first) {
            buildSum += buildMs;
            buildSqSum += buildMs * buildMs;
            buildCount++;
            totalSum += totalMs;
            totalCount++;
            scoreSum += score;
            scoreCount++;
            minScore = Math.min(minScore, score);
            maxScore = Math.max(maxScore, score);
            minUsed = Math.min(minUsed, usedAfter);
            maxUsed = Math.max(maxUsed, usedAfter);
            double deltaMB = (usedAfter - usedBefore) / (1024.0 * 1024.0);
            deltaSumMB += deltaMB;
            deltaSqSumMB += deltaMB * deltaMB;
            deltaCount++;
        }

        synchronized void recordReceive(double ms) {
            recvSum += ms;
            recvCount++;
        }

        synchronized double avgBuild() {
            return buildCount == 0 ? 0.0 : buildSum / buildCount;
        }

        synchronized double steadyAvgBuild() {
            if (buildCount <= 1 || firstBuildMs < 0) return 0.0;
            return (buildSum - firstBuildMs) / (buildCount - 1);
        }

        synchronized double avgTotal() {
            return totalCount == 0 ? 0.0 : totalSum / totalCount;
        }

        synchronized double avgScore() {
            return scoreCount == 0 ? 0.0 : scoreSum / scoreCount;
        }

        synchronized double avgReceive() {
            return recvCount == 0 ? 0.0 : recvSum / recvCount;
        }

        synchronized double heapDeltaMB() {
            if (minUsed == Long.MAX_VALUE || maxUsed == Long.MIN_VALUE) return 0.0;
            return (maxUsed - minUsed) / (1024.0 * 1024.0);
        }

        synchronized double projectedHeapDeltaMB(long usedAfter) {
            long min = Math.min(minUsed == Long.MAX_VALUE ? usedAfter : minUsed, usedAfter);
            long max = Math.max(maxUsed == Long.MIN_VALUE ? usedAfter : maxUsed, usedAfter);
            return (max - min) / (1024.0 * 1024.0);
        }

        synchronized double deltaAvgMB() {
            return deltaCount == 0 ? 0.0 : deltaSumMB / deltaCount;
        }

        synchronized double deltaVarianceMB() {
            if (deltaCount == 0) return 0.0;
            double mean = deltaSumMB / deltaCount;
            return Math.max(0.0, (deltaSqSumMB / deltaCount) - (mean * mean));
        }

        synchronized String stabilityLabel() {
            if (buildCount <= 1) return "unknown";
            double mean = buildSum / buildCount;
            double var = Math.max(0.0, (buildSqSum / buildCount) - (mean * mean));
            double sd = Math.sqrt(var);
            double cv = mean == 0 ? 0.0 : sd / mean;
            if (cv < 0.1) return "稳定";
            if (cv < 0.2) return "较稳定";
            return "波动较大";
        }

        synchronized double stabilityScore() {
            double score = 100.0;
            double varPenalty = Math.min(40.0, deltaVarianceMB() * 4.0);
            score -= varPenalty;
            double intervalPenalty = 0.0;
            if (minGcIntervalMs != Double.POSITIVE_INFINITY) {
                intervalPenalty = Math.min(30.0, Math.max(0.0, 50.0 - minGcIntervalMs) * 0.6);
            }
            score -= intervalPenalty;
            double pausePenalty = Math.min(30.0, Math.max(0.0, maxGcPauseMs - 50.0) * 0.5);
            score -= pausePenalty;
            if (score < 0.0) score = 0.0;
            return score;
        }

        synchronized void initGcMonitor() {
            if (gcInitialized) return;
            gcInitialized = true;
            for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
                if (gcBean instanceof NotificationEmitter emitter) {
                    NotificationListener listener = (Notification notification, Object handback) -> {
                        if (!GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION.equals(notification.getType())) {
                            return;
                        }
                        try {
                            CompositeData cd = (CompositeData) notification.getUserData();
                            GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from(cd);
                            GcInfo gcInfo = info.getGcInfo();
                            long startTime = gcInfo.getStartTime();
                            long endTime = gcInfo.getEndTime();
                            double pauseMs = gcInfo.getDuration();
                            gcCount++;
                            if (lastGcEndMillis >= 0) {
                                double interval = startTime - lastGcEndMillis;
                                if (interval > 0 && interval < minGcIntervalMs) {
                                    minGcIntervalMs = interval;
                                }
                            }
                            if (pauseMs > maxGcPauseMs) {
                                maxGcPauseMs = pauseMs;
                            }
                            lastGcEndMillis = endTime;
                        } catch (Throwable ignored) {
                        }
                    };
                    emitter.addNotificationListener(listener, null, null);
                }
            }
        }

        synchronized void reset() {
            buildSum = 0.0;
            buildSqSum = 0.0;
            buildCount = 0;
            totalSum = 0.0;
            totalCount = 0;
            scoreSum = 0.0;
            scoreCount = 0;
            minScore = Double.POSITIVE_INFINITY;
            maxScore = Double.NEGATIVE_INFINITY;
            recvSum = 0.0;
            recvCount = 0;
            minUsed = Long.MAX_VALUE;
            maxUsed = Long.MIN_VALUE;
            deltaSumMB = 0.0;
            deltaSqSumMB = 0.0;
            deltaCount = 0;
            lastGcEndMillis = -1L;
            minGcIntervalMs = Double.POSITIVE_INFINITY;
            maxGcPauseMs = 0.0;
            gcCount = 0;
        }
    }
}
