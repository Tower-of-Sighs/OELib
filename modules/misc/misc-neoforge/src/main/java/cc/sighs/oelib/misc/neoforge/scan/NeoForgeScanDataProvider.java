package cc.sighs.oelib.misc.neoforge.scan;

import cc.sighs.oelib.misc.scan.OELScanData;
import cc.sighs.oelib.misc.scan.spi.IScanDataProvider;
import com.flechazo.hkt.business.concurrent.ThreadConfig;
import com.flechazo.hkt.business.core.Pathway;
import com.flechazo.hkt.business.effect.VTask;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.modscan.ModAnnotation;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class NeoForgeScanDataProvider implements IScanDataProvider {
    private final VTask<OELScanData> adaptationTask = VTask.delay(NeoForgeScanDataProvider::adaptLoaderData)
            .memoize()
            .unsafeRun();
    private final AtomicBoolean started = new AtomicBoolean();

    @Override
    public void preload() {
        if (started.compareAndSet(false, true)) {
            Thread coordinator = ThreadConfig.platform("oelib-neoforge-scan-adapter-")
                    .factory()
                    .newThread(adaptationTask::unsafeRun);
            coordinator.start();
        }
    }

    @Override
    public OELScanData getScanData() {
        preload();
        return Pathway.vtask(adaptationTask)
                .mapErrorAll(error -> {
                    if (error instanceof InterruptedException) {
                        return new IllegalStateException("Interrupted while adapting NeoForge scan data", error);
                    }
                    return new IllegalStateException("Failed to adapt NeoForge scan data", error);
                })
                .unsafeRun();
    }

    private static OELScanData adaptLoaderData() {
        List<OELScanData.ClassData> classes = new ArrayList<>();
        List<OELScanData.AnnotationData> annotations = new ArrayList<>();

        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            Set<String> modIds = getModIds(scanData.getIModInfoData());
            for (ModFileScanData.ClassData classData : scanData.getClasses()) {
                classes.add(new OELScanData.ClassData(
                        classData.clazz().getClassName(),
                        classData.parent() == null ? null : classData.parent().getClassName(),
                        classData.interfaces().stream().map(Type::getClassName).collect(Collectors.toSet()),
                        modIds
                ));
            }
            for (ModFileScanData.AnnotationData annotation : scanData.getAnnotations()) {
                annotations.add(new OELScanData.AnnotationData(
                        annotation.annotationType().getClassName(),
                        annotation.targetType(),
                        annotation.clazz().getClassName(),
                        annotation.memberName(),
                        normalizeMap(annotation.annotationData()),
                        modIds
                ));
            }
        }
        return new OELScanData(classes, annotations);
    }

    private static Set<String> getModIds(Collection<IModFileInfo> modFiles) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (IModFileInfo modFile : modFiles) {
            modFile.getMods().stream().map(IModInfo::getModId).forEach(result::add);
        }
        return Set.copyOf(result);
    }

    private static Map<String, Object> normalizeMap(Map<String, ?> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> result = new LinkedHashMap<>(source.size());
        source.forEach((key, value) -> result.put(key, normalizeValue(value)));
        return result;
    }

    private static Object normalizeValue(Object value) {
        if (value instanceof Type type) {
            return new OELScanData.TypeValue(type.getDescriptor());
        }
        if (value instanceof ModAnnotation.EnumHolder(String desc, String value1)) {
            return new OELScanData.EnumValue(desc, value1);
        }
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> result = new LinkedHashMap<>(map.size());
            map.forEach((key, nested) -> result.put(String.valueOf(key), normalizeValue(nested)));
            return result;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(NeoForgeScanDataProvider::normalizeValue).toList();
        }
        if (value != null && value.getClass().isArray()) {
            int length = Array.getLength(value);
            ArrayList<Object> result = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                result.add(normalizeValue(Array.get(value, i)));
            }
            return List.copyOf(result);
        }
        return value;
    }
}
