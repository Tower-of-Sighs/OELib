package cc.sighs.oelib.dev.example.net;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.network.api.INetworkContext;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.api.Side;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@NetworkPacket(modId = OELib.MODID, id = "big_component", side = Side.BOTH, chunkThreshold = 8192)
public record BigComponentPacket(
        int pInt,
        Integer pInteger,
        long pLong,
        Long pLongW,
        boolean pBoolean,
        Boolean pBooleanW,
        float pFloat,
        Float pFloatW,
        double pDouble,
        Double pDoubleW,
        byte pByte,
        Byte pByteW,
        short pShort,
        Short pShortW,
        String refString,
        UUID refUuid,
        Date refDate,
        Instant refInstant,
        BitSet refBitSet,
        PublicKey refPublicKey,
        byte[] refBytes,
        int[] refIntArray,
        long[] refLongArray,
        BlockPos mcBlockPos,
        ChunkPos mcChunkPos,
        SectionPos mcSectionPos,
        GlobalPos mcGlobalPos,
        Vec3 mcVec3,
        Vector3f mcVector3f,
        Quaternionf mcQuaternionf,
        ResourceLocation mcResourceLocation,
        ResourceKey<?> mcResourceKey,
        BlockHitResult mcBlockHitResult,
        CompoundTag mcCompoundTag,
        Tag mcTag,
        ExampleEnum enumVal,
        NestedRecord nestedRecord,
        Optional<String> optString,
        Pair<Integer, String> pairDFU,
        org.apache.commons.lang3.tuple.Pair<Long, UUID> pairCommons,
        Either<String, Integer> eitherVal,
        Triple<String, Integer, Long> tripleVal,
        List<String> listStrings,
        Set<Integer> setIntegers,
        Map<String, Long> mapStringLong,
        EnumSet<ExampleEnum> enumSet,
        ObjectList<String> fastList,
        ObjectSet<UUID> fastObjectSet,
        IntSet fastIntSet,
        LongSet fastLongSet,
        Object2IntMap<String> fastO2I,
        Object2LongMap<String> fastO2L,
        Object2ObjectMap<String, UUID> fastO2O,
        Int2ObjectMap<String> fastI2O,
        Int2IntMap fastI2I,
        Long2ObjectMap<String> fastL2O,
        IntList fastIntList
) implements INetworkPacket<BigComponentPacket> {
    private static boolean hasSavedSample = false;

    @Override
    public void handle(INetworkContext context) {
        long startTime = System.nanoTime();
        int dataLength = this.refString().length();

        OELib.LOGGER.info("[Performance] Received packets (Size: {} bytes)", dataLength);

        if (!hasSavedSample) {
            CompletableFuture.runAsync(() -> {
                try {
                    var path = Paths.get("logs", "packet_dumps");
                    Files.createDirectories(path);
                    String summary = "Packet Received at " + new Date() + "\nString Length: " + dataLength;
                    Files.writeString(path.resolve("stress_sample_summary.txt"), summary);
                    OELib.LOGGER.info("[Performance] Sample abstracts are saved.");
                } catch (Exception e) {
                    OELib.LOGGER.error("Save failed", e);
                }
            });
            hasSavedSample = true;
        }

        long endTime = System.nanoTime();
        OELib.LOGGER.info("[Performance] Receiving logic triggering is time-consuming: {} ms", (endTime - startTime) / 1_000_000.0);
    }

    public enum ExampleEnum {
        A, B, C
    }

    public record NestedRecord(String a, int b, ResourceLocation c) {
    }
}
