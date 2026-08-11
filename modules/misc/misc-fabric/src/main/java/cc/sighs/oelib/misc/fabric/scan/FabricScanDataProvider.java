package cc.sighs.oelib.misc.fabric.scan;

import cc.sighs.oelib.misc.OELibMisc;
import cc.sighs.oelib.misc.scan.OELScanData;
import cc.sighs.oelib.misc.scan.spi.IScanDataProvider;
import com.flechazo.hkt.Unit;
import com.flechazo.hkt.business.concurrent.ExecutorResources;
import com.flechazo.hkt.business.concurrent.ExecutorShutdownPolicy;
import com.flechazo.hkt.business.concurrent.ThreadConfig;
import com.flechazo.hkt.business.core.Pathway;
import com.flechazo.hkt.business.core.Traverses;
import com.flechazo.hkt.business.effect.VTask;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class FabricScanDataProvider implements IScanDataProvider {
    private final VTask<OELScanData> scanTask = VTask.delay(FabricScanDataProvider::scanMods)
            .memoize()
            .unsafeRun();
    private final AtomicBoolean started = new AtomicBoolean();

    @Override
    public void preload() {
        if (started.compareAndSet(false, true)) {
            Thread coordinator = ThreadConfig.platform("oelib-mod-scan-coordinator-")
                    .factory()
                    .newThread(scanTask::unsafeRun);
            coordinator.start();
        }
    }

    @Override
    public OELScanData getScanData() {
        preload();
        return Pathway.vtask(scanTask)
                .mapErrorAll(error -> mapFutureFailure(
                        error,
                        "Interrupted while waiting for the Fabric mod scan",
                        "Fabric mod scan failed"
                ))
                .unsafeRun();
    }

    private static OELScanData scanMods() {
        long startedAt = System.nanoTime();
        List<ScanTarget> targets = collectTargets();
        if (targets.isEmpty()) {
            return new OELScanData(List.of(), List.of());
        }

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int threadCount = Math.clamp(availableProcessors - 1, 1, targets.size());
        ScanAccumulator scanned = scanTargets(targets, threadCount);

        OELScanData result = new OELScanData(scanned.classes, scanned.annotations);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        OELibMisc.LOGGER.info(
                "Fabric mod scan completed: {} roots, {} classes, {} annotations, {} failures, {} ms",
                targets.size(), result.classes().size(), result.annotations().size(), scanned.failures, elapsedMillis
        );
        return result;
    }

    private static ScanAccumulator scanTargets(List<ScanTarget> targets, int threadCount) {
        return ExecutorResources.fixed(
                        threadCount,
                        ThreadConfig.platform("oelib-mod-scan-"),
                        new ExecutorShutdownPolicy(Duration.ofSeconds(5), Duration.ofSeconds(5)))
                .use(executor -> Traverses.parTraverseVTask(
                                targets,
                                threadCount,
                                executor,
                                target -> VTask.delay(() -> scanTarget(target)))
                        .map(FabricScanDataProvider::combineTargets)
                        .mapErrorAll(error -> mapFutureFailure(
                                error,
                                "Interrupted while scanning Fabric mods",
                                "Failed to scan Fabric mods"
                        )))
                .unsafeRun();
    }

    private static ScanAccumulator combineTargets(List<ScanAccumulator> targets) {
        ScanAccumulator combined = new ScanAccumulator();
        for (ScanAccumulator scanned : targets) {
            combined.classes.addAll(scanned.classes);
            combined.annotations.addAll(scanned.annotations);
            combined.failures += scanned.failures;
        }
        return combined;
    }

    private static List<ScanTarget> collectTargets() {
        LinkedHashMap<Path, LinkedHashSet<String>> roots = new LinkedHashMap<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            if ("builtin".equals(mod.getMetadata().getType())) {
                continue;
            }
            String modId = mod.getMetadata().getId();
            for (Path root : mod.getRootPaths()) {
                Path key = root.toAbsolutePath().normalize();
                roots.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(modId);
            }
        }
        return roots.entrySet().stream()
                .map(entry -> new ScanTarget(entry.getKey(), Set.copyOf(entry.getValue())))
                .toList();
    }

    private static ScanAccumulator scanTarget(ScanTarget target) {
        ScanAccumulator result = new ScanAccumulator();
        return Pathway.ioAutoResource((Callable<Stream<Path>>) () -> Files.walk(target.root()))
                .useSync(paths -> scanTargetFiles(paths, target, result))
                .toTryPath()
                .peekFailure(error -> {
                    result.failures++;
                    OELibMisc.LOGGER.warn("Failed to walk Fabric mod root {}", target.root(), error);
                })
                .getOrElse(result);
    }

    private static ScanAccumulator scanTargetFiles(
            Stream<Path> paths,
            ScanTarget target,
            ScanAccumulator result
    ) {
        paths.filter(Files::isRegularFile)
                .filter(FabricScanDataProvider::isClassFile)
                .forEach(path -> scanClass(path, target.modIds(), result));
        return result;
    }

    private static boolean isClassFile(Path path) {
        Path fileName = path.getFileName();
        return fileName != null && fileName.toString().endsWith(".class");
    }

    private static void scanClass(
            Path path,
            Set<String> modIds,
            ScanAccumulator result
    ) {
        Pathway.ioAutoResource((Callable<InputStream>) () -> Files.newInputStream(path))
                .use(input -> Pathway.io(() -> scanClassFile(input, modIds, result)))
                .toTryPath()
                .peekFailure(error -> {
                    result.failures++;
                    OELibMisc.LOGGER.debug("Failed to scan class file {}", path, error);
                });
    }

    private static Unit scanClassFile(
            InputStream input,
            Set<String> modIds,
            ScanAccumulator result
    ) throws IOException {
        ClassReader reader = new ClassReader(input);
        reader.accept(
                new ScanClassVisitor(modIds, result.classes, result.annotations),
                ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES
        );
        return Unit.INSTANCE;
    }

    private static Throwable mapFutureFailure(
            Throwable error,
            String interruptedMessage,
            String failedMessage
    ) {
        if (error instanceof InterruptedException) {
            return new IllegalStateException(interruptedMessage, error);
        }
        return new IllegalStateException(failedMessage, error);
    }

    private record ScanTarget(Path root, Set<String> modIds) {
    }

    private static final class ScanAccumulator {
        private final ArrayList<OELScanData.ClassData> classes = new ArrayList<>();
        private final ArrayList<OELScanData.AnnotationData> annotations = new ArrayList<>();
        private int failures;
    }

    private static final class ScanClassVisitor extends ClassVisitor {
        private final Set<String> modIds;
        private final Collection<OELScanData.ClassData> classes;
        private final Collection<OELScanData.AnnotationData> annotations;
        private String className;
        private String parentClassName;
        private Set<String> interfaces = Set.of();

        private ScanClassVisitor(
                Set<String> modIds,
                Collection<OELScanData.ClassData> classes,
                Collection<OELScanData.AnnotationData> annotations
        ) {
            super(Opcodes.ASM9);
            this.modIds = modIds;
            this.classes = classes;
            this.annotations = annotations;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaceNames) {
            className = Type.getObjectType(name).getClassName();
            parentClassName = superName == null ? null : Type.getObjectType(superName).getClassName();
            LinkedHashSet<String> values = new LinkedHashSet<>(interfaceNames.length);
            for (String interfaceName : interfaceNames) {
                values.add(Type.getObjectType(interfaceName).getClassName());
            }
            interfaces = Set.copyOf(values);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return annotationVisitor(descriptor, ElementType.TYPE, className);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            return new FieldVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
                    return annotationVisitor(annotationDescriptor, ElementType.FIELD, name);
                }
            };
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
                    return annotationVisitor(annotationDescriptor, ElementType.METHOD, name + descriptor);
                }
            };
        }

        @Override
        public void visitEnd() {
            classes.add(new OELScanData.ClassData(className, parentClassName, interfaces, modIds));
        }

        private AnnotationVisitor annotationVisitor(String descriptor, ElementType targetType, String memberName) {
            String annotationType = Type.getType(descriptor).getClassName();
            return new ValueCollector(values -> annotations.add(new OELScanData.AnnotationData(
                    annotationType,
                    targetType,
                    className,
                    memberName,
                    values,
                    modIds
            )));
        }
    }

    private static final class ValueCollector extends AnnotationVisitor {
        private final LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        private final Consumer<Map<String, Object>> completed;

        private ValueCollector(Consumer<Map<String, Object>> completed) {
            super(Opcodes.ASM9);
            this.completed = completed;
        }

        @Override
        public void visit(String name, Object value) {
            values.put(name, normalizeValue(value));
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            values.put(name, new OELScanData.EnumValue(descriptor, value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            String annotationType = Type.getType(descriptor).getClassName();
            return new ValueCollector(nested -> values.put(
                    name,
                    new OELScanData.NestedAnnotationValue(annotationType, nested)
            ));
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            ArrayList<Object> array = new ArrayList<>();
            values.put(name, array);
            return new ArrayCollector(array);
        }

        @Override
        public void visitEnd() {
            completed.accept(values);
        }
    }

    private static final class ArrayCollector extends AnnotationVisitor {
        private final List<Object> values;

        private ArrayCollector(List<Object> values) {
            super(Opcodes.ASM9);
            this.values = values;
        }

        @Override
        public void visit(String name, Object value) {
            values.add(normalizeValue(value));
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            values.add(new OELScanData.EnumValue(descriptor, value));
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            String annotationType = Type.getType(descriptor).getClassName();
            return new ValueCollector(nested -> values.add(
                    new OELScanData.NestedAnnotationValue(annotationType, nested)
            ));
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            ArrayList<Object> nested = new ArrayList<>();
            values.add(nested);
            return new ArrayCollector(nested);
        }
    }

    private static Object normalizeValue(Object value) {
        if (value instanceof Type type) {
            return new OELScanData.TypeValue(type.getDescriptor());
        }
        return value;
    }
}
