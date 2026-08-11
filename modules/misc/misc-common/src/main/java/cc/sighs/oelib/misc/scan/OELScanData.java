package cc.sighs.oelib.misc.scan;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Immutable, loader-independent metadata collected from every loaded mod file.
 *
 * <p>The shape intentionally mirrors the useful parts of NeoForge's
 * {@code ModFileScanData}: class hierarchy data and annotations on types,
 * fields and methods. Implementations build this object once and callers use
 * its annotation index.</p>
 */
public final class OELScanData {
    private static final Comparator<ClassData> CLASS_ORDER = Comparator.comparing(ClassData::className);
    private static final Comparator<AnnotationData> ANNOTATION_ORDER = Comparator
            .comparing(AnnotationData::annotationType)
            .thenComparing(AnnotationData::className)
            .thenComparing(data -> data.targetType().ordinal())
            .thenComparing(AnnotationData::memberName);

    private final List<ClassData> classes;
    private final List<AnnotationData> annotations;
    private final Map<String, ClassData> classesByName;
    private final Map<String, List<AnnotationData>> annotationsByType;

    public OELScanData(Collection<ClassData> classes, Collection<AnnotationData> annotations) {
        Objects.requireNonNull(classes, "classes");
        Objects.requireNonNull(annotations, "annotations");

        LinkedHashMap<String, ClassData> classIndex = new LinkedHashMap<>();
        classes.stream()
                .map(ClassData::immutableCopy)
                .sorted(CLASS_ORDER)
                .forEach(data -> classIndex.merge(data.className(), data, ClassData::mergeOwners));
        this.classesByName = Map.copyOf(classIndex);
        this.classes = List.copyOf(classIndex.values());

        ArrayList<AnnotationData> annotationList = new ArrayList<>(annotations.size());
        annotations.stream()
                .map(AnnotationData::immutableCopy)
                .sorted(ANNOTATION_ORDER)
                .distinct()
                .forEach(annotationList::add);
        this.annotations = List.copyOf(annotationList);

        LinkedHashMap<String, List<AnnotationData>> annotationIndex = new LinkedHashMap<>();
        for (AnnotationData annotation : this.annotations) {
            annotationIndex.computeIfAbsent(annotation.annotationType(), ignored -> new ArrayList<>())
                    .add(annotation);
        }
        annotationIndex.replaceAll((ignored, values) -> List.copyOf(values));
        this.annotationsByType = Map.copyOf(annotationIndex);
    }

    public List<ClassData> classes() {
        return classes;
    }

    public List<AnnotationData> annotations() {
        return annotations;
    }

    public ClassData getClassData(String className) {
        return classesByName.get(className);
    }

    public List<AnnotationData> getAnnotatedBy(Class<? extends Annotation> annotationType) {
        return getAnnotatedBy(annotationType.getName());
    }

    public List<AnnotationData> getAnnotatedBy(String annotationType) {
        return annotationsByType.getOrDefault(annotationType, List.of());
    }

    public List<AnnotationData> getAnnotatedBy(Class<? extends Annotation> annotationType, ElementType targetType) {
        return getAnnotatedBy(annotationType).stream()
                .filter(data -> data.targetType() == targetType)
                .toList();
    }

    public record ClassData(
            String className,
            String parentClassName,
            Set<String> interfaces,
            Set<String> modIds
    ) {
        public ClassData {
            Objects.requireNonNull(className, "className");
            interfaces = Set.copyOf(interfaces);
            modIds = Set.copyOf(modIds);
        }

        private ClassData immutableCopy() {
            return new ClassData(className, parentClassName, interfaces, modIds);
        }

        private ClassData mergeOwners(ClassData other) {
            if (!className.equals(other.className)) {
                throw new IllegalArgumentException("Cannot merge different classes");
            }
            LinkedHashSet<String> owners = new LinkedHashSet<>(modIds);
            owners.addAll(other.modIds);
            return new ClassData(className, parentClassName, interfaces, owners);
        }
    }

    public record AnnotationData(
            String annotationType,
            ElementType targetType,
            String className,
            String memberName,
            Map<String, Object> values,
            Set<String> modIds
    ) {
        public AnnotationData {
            Objects.requireNonNull(annotationType, "annotationType");
            Objects.requireNonNull(targetType, "targetType");
            Objects.requireNonNull(className, "className");
            memberName = memberName == null ? className : memberName;
            values = immutableMap(values);
            modIds = Set.copyOf(modIds);
        }

        private AnnotationData immutableCopy() {
            return new AnnotationData(annotationType, targetType, className, memberName, values, modIds);
        }
    }

    public record TypeValue(String descriptor) {
        public TypeValue {
            Objects.requireNonNull(descriptor, "descriptor");
        }
    }

    public record EnumValue(String descriptor, String constantName) {
        public EnumValue {
            Objects.requireNonNull(descriptor, "descriptor");
            Objects.requireNonNull(constantName, "constantName");
        }
    }

    public record NestedAnnotationValue(String annotationType, Map<String, Object> values) {
        public NestedAnnotationValue {
            Objects.requireNonNull(annotationType, "annotationType");
            values = immutableMap(values);
        }
    }

    private static Map<String, Object> immutableMap(Map<String, ?> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>(source.size());
        source.forEach((key, value) -> copy.put(key, immutableValue(value)));
        return Map.copyOf(copy);
    }

    private static Object immutableValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> copy = new LinkedHashMap<>(map.size());
            map.forEach((key, nested) -> copy.put(String.valueOf(key), immutableValue(nested)));
            return Map.copyOf(copy);
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(OELScanData::immutableValue).toList();
        }
        if (value != null && value.getClass().isArray()) {
            int length = Array.getLength(value);
            ArrayList<Object> copy = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                copy.add(immutableValue(Array.get(value, i)));
            }
            return List.copyOf(copy);
        }
        return value;
    }
}
