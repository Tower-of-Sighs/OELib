package cc.sighs.oelib.config.datafix;

import cc.sighs.oelib.config.OELibConfig;
import cc.sighs.oelib.config.model.ConfigValueMeta;
import com.flechazo.hkt.Maybe;
import com.flechazo.hkt.Try;
import com.flechazo.hkt.business.util.OptionalOps;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Dynamic;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Registers and applies versioned configuration migrations.
 *
 * <p>Each configuration identifier may have one global migration chain. Field migrations declared
 * by the configuration schema participate in the same version transitions. A migration request
 * either returns the value at the current version or an explicit {@link ConfigFixError}.
 */
public final class ConfigFixRegistry {
    private static final Map<ResourceLocation, Chain> CHAINS = new LinkedHashMap<>();

    private ConfigFixRegistry() {
    }

    /**
     * Registers the complete global migration chain for a configuration.
     *
     * <p>The consumer must define a connected chain from version {@code 0} to
     * {@code currentVersion}. Registration fails without replacing an existing chain when the
     * definition is invalid.
     *
     * @param id the configuration identifier
     * @param currentVersion the nonnegative version produced by the chain
     * @param consumer the action that defines migration steps
     * @throws IllegalArgumentException if a chain is already registered for {@code id}, or if the
     *         defined chain is invalid or disconnected
     */
    public static synchronized void register(
            ResourceLocation id, int currentVersion, Consumer<Builder> consumer) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(consumer, "consumer");
        if (CHAINS.containsKey(id)) {
            throw new IllegalArgumentException("A config fix chain is already registered for " + id);
        }
        Builder builder = new Builder(currentVersion);
        consumer.accept(builder);
        Chain chain = builder.build();
        CHAINS.put(id, chain);
        OELibConfig.LOGGER.info(
                "Registered config fix chain {} currentVersion={}", id, currentVersion);
    }

    /**
     * Returns the global migration chain registered for a configuration.
     *
     * @param id the configuration identifier
     * @return the registered chain, or an empty value when no chain is registered
     */
    public static synchronized Maybe<Chain> get(ResourceLocation id) {
        return Maybe.ofNullable(CHAINS.get(id));
    }

    /**
     * Returns the current version declared by global and field migrations.
     *
     * @param id the configuration identifier
     * @param fields the field descriptors whose migrations participate in the chain
     * @return the greatest target version, or {@code 0} when no migration is defined
     */
    public static int currentVersion(
            ResourceLocation id, List<ConfigValueMeta> fields) {
        int version = get(id).map(Chain::currentVersion).orElse(0);
        for (ConfigValueMeta field : fields) {
            for (ConfigValueMeta.FieldMigration migration : field.migrations()) {
                version = Math.max(version, migration.toVersion());
            }
        }
        return version;
    }

    /**
     * Applies all required global and field migrations to a dynamic value.
     *
     * <p>For each version transition, the global step runs before field steps, and field steps run
     * in schema declaration order. A missing step, inconsistent target, future version, or
     * migration exception produces a left value and no later step is evaluated.
     *
     * @param id the configuration identifier
     * @param input the dynamic value at {@code inputVersion}
     * @param inputVersion the nonnegative version of {@code input}
     * @param fields the field descriptors whose migrations participate in the chain
     * @return a right value containing the migrated dynamic, or a left value describing failure
     */
    public static Either<ConfigFixError, Dynamic<?>> apply(
            ResourceLocation id,
            Dynamic<?> input,
            int inputVersion,
            List<ConfigValueMeta> fields) {
        Objects.requireNonNull(fields, "fields");
        Maybe<Chain> registered = get(id);
        int targetVersion = currentVersion(id, fields);
        if (inputVersion < 0) {
            return Either.left(new ConfigFixError(
                    "negative_version", inputVersion, targetVersion,
                    "Input config version must not be negative"));
        }
        if (inputVersion > targetVersion) {
            return Either.left(new ConfigFixError(
                    "future_version", inputVersion, targetVersion,
                    "Input config version " + inputVersion
                            + " is newer than supported version " + targetVersion));
        }

        Dynamic<?> current = input;
        int version = inputVersion;
        while (version < targetVersion) {
            FixStep global = registered.isDefined()
                    ? registered.get().fixes.get(version)
                    : null;
            List<FieldStep> fieldSteps = fieldSteps(fields, version);
            int nextVersion;
            if (global != null) {
                nextVersion = global.toVersion();
                for (FieldStep field : fieldSteps) {
                    if (field.migration().toVersion() != nextVersion) {
                        return Either.left(new ConfigFixError(
                                "field_step_mismatch", version, targetVersion,
                                "Field migration at " + field.path()
                                        + " targets " + field.migration().toVersion()
                                        + " but the chain targets " + nextVersion));
                    }
                }
                Dynamic<?> stepInput = current;
                Try<Dynamic<?>> applied = Try.of(() ->
                        Objects.requireNonNull(global.operation().apply(stepInput)));
                if (applied.isFailure()) {
                    return Either.left(stepException(
                            global.fromVersion(), global.toVersion(), applied.cause()));
                }
                current = applied.get();
            } else if (!fieldSteps.isEmpty()) {
                nextVersion = fieldSteps.getFirst().migration().toVersion();
                for (FieldStep field : fieldSteps) {
                    if (field.migration().toVersion() != nextVersion) {
                        return Either.left(new ConfigFixError(
                                "field_step_mismatch", version, targetVersion,
                                "Field migrations from version " + version
                                        + " have different target versions"));
                    }
                }
            } else {
                return Either.left(new ConfigFixError(
                        "missing_step", version, targetVersion,
                        "No global or field config fix starts at version " + version));
            }

            for (FieldStep field : fieldSteps) {
                Dynamic<?> fieldInput = current;
                Try<Dynamic<?>> applied = Try.of(() -> FixContext.transformAt(
                        fieldInput, field.meta().pathSegments(), field.migration().migration()));
                if (applied.isFailure()) {
                    return Either.left(new ConfigFixError(
                            "field_step_exception", version, nextVersion,
                            "Field config fix at " + field.path() + " failed: "
                                    + applied.cause().getMessage(), applied.cause()));
                }
                current = applied.get();
            }
            version = nextVersion;
        }
        return Either.right(current);
    }

    private static List<FieldStep> fieldSteps(
            List<ConfigValueMeta> fields, int fromVersion) {
        ArrayList<FieldStep> steps = new ArrayList<>();
        for (ConfigValueMeta field : fields) {
            for (ConfigValueMeta.FieldMigration migration : field.migrations()) {
                if (migration.fromVersion() == fromVersion) {
                    steps.add(new FieldStep(field.key(), field, migration));
                }
            }
        }
        return steps;
    }

    private static ConfigFixError stepException(
            int fromVersion, int toVersion, Throwable throwable) {
        return new ConfigFixError(
                "step_exception", fromVersion, toVersion,
                "Config fix " + fromVersion + " -> " + toVersion
                        + " failed: " + throwable.getMessage(), throwable);
    }

    static synchronized void clearForTests() {
        CHAINS.clear();
    }

    /**
     * Describes a configuration migration failure.
     *
     * @param code the stable failure category
     * @param fromVersion the version at which migration failed
     * @param targetVersion the version the failed operation or chain was expected to produce
     * @param message the human-readable failure description
     * @param cause the originating exception, or {@code null} when no exception caused the failure
     */
    public record ConfigFixError(
            String code, int fromVersion, int targetVersion, String message, Throwable cause) {
        /**
         * Validates the required failure attributes.
         *
         * @param code the stable failure category
         * @param fromVersion the version at which migration failed
         * @param targetVersion the expected target version
         * @param message the human-readable failure description
         * @param cause the originating exception, or {@code null}
         */
        public ConfigFixError {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(message, "message");
        }

        /**
         * Creates a migration failure without an originating exception.
         *
         * @param code the stable failure category
         * @param fromVersion the version at which migration failed
         * @param targetVersion the expected target version
         * @param message the human-readable failure description
         */
        public ConfigFixError(String code, int fromVersion, int targetVersion, String message) {
            this(code, fromVersion, targetVersion, message, null);
        }
    }

    /**
     * Defines the global version transitions for one configuration.
     */
    public static final class Builder {
        private final int currentVersion;
        private final Map<Integer, FixStep> fixes = new LinkedHashMap<>();

        /**
         * Creates a builder for a chain with the specified current version.
         *
         * @param currentVersion the nonnegative version produced by the completed chain
         * @throws IllegalArgumentException if {@code currentVersion} is negative
         */
        public Builder(int currentVersion) {
            if (currentVersion < 0) {
                throw new IllegalArgumentException("currentVersion must not be negative");
            }
            this.currentVersion = currentVersion;
        }

        /**
         * Registers a dynamic transformation for one version transition.
         *
         * @param fromVersion the nonnegative source version
         * @param toVersion the target version, greater than {@code fromVersion} and no greater than
         *        the chain's current version
         * @param operation the transformation applied to values at {@code fromVersion}
         * @return this builder instance
         * @throws IllegalArgumentException if the versions are invalid or another step starts at
         *         {@code fromVersion}
         */
        public Builder fix(
                int fromVersion, int toVersion, UnaryOperator<Dynamic<?>> operation) {
            Objects.requireNonNull(operation, "operation");
            validateBounds(fromVersion, toVersion);
            FixStep old = fixes.putIfAbsent(
                    fromVersion, new FixStep(fromVersion, toVersion, operation));
            if (old != null) {
                throw new IllegalArgumentException(
                        "Duplicate config fix source version " + fromVersion);
            }
            return this;
        }

        private void validateBounds(int fromVersion, int toVersion) {
            if (fromVersion < 0) {
                throw new IllegalArgumentException("fromVersion must not be negative");
            }
            if (toVersion <= fromVersion) {
                throw new IllegalArgumentException(
                        "Config fixes must advance: " + fromVersion + " -> " + toVersion);
            }
            if (toVersion > currentVersion) {
                throw new IllegalArgumentException(
                        "Config fix target " + toVersion
                                + " exceeds current version " + currentVersion);
            }
        }

        private Chain build() {
            if (currentVersion == 0) {
                if (!fixes.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Version zero chain cannot contain migration steps");
                }
                return new Chain(0, Map.of());
            }

            int version = 0;
            while (version < currentVersion) {
                FixStep step = fixes.get(version);
                if (step == null) {
                    throw new IllegalArgumentException(
                            "Config fix chain is disconnected at version " + version
                                    + " before current version " + currentVersion);
                }
                version = step.toVersion();
            }
            if (version != currentVersion) {
                throw new IllegalArgumentException(
                        "Config fix chain ends at " + version
                                + " instead of current version " + currentVersion);
            }

            for (FixStep step : fixes.values()) {
                int cursor = step.toVersion();
                while (cursor < currentVersion) {
                    FixStep next = fixes.get(cursor);
                    if (next == null) {
                        throw new IllegalArgumentException(
                                "Config fix from " + step.fromVersion()
                                        + " cannot connect to current version " + currentVersion);
                    }
                    cursor = next.toVersion();
                }
            }
            return new Chain(currentVersion, fixes);
        }
    }

    /**
     * Represents an immutable global migration chain connected to its current version.
     */
    public static final class Chain {
        private final int currentVersion;
        private final Map<Integer, FixStep> fixes;

        private Chain(int currentVersion, Map<Integer, FixStep> fixes) {
            this.currentVersion = currentVersion;
            this.fixes = Map.copyOf(fixes);
        }

        /**
         * Returns the version produced by this chain.
         *
         * @return the current configuration version
         */
        public int currentVersion() {
            return currentVersion;
        }

        /**
         * Applies every required global step from an input version to the current version.
         *
         * <p>Steps run in ascending transition order. A negative or future input version, a missing
         * step, or an exception from a step produces a left value and prevents later steps from
         * running.
         *
         * @param input the dynamic value at {@code inputVersion}
         * @param inputVersion the version of {@code input}
         * @return a right value containing the migrated dynamic, or a left value describing failure
         */
        public Either<ConfigFixError, Dynamic<?>> apply(Dynamic<?> input, int inputVersion) {
            Objects.requireNonNull(input, "input");
            if (inputVersion < 0) {
                return Either.left(new ConfigFixError(
                        "negative_version", inputVersion, currentVersion,
                        "Input config version must not be negative"));
            }
            if (inputVersion > currentVersion) {
                return Either.left(new ConfigFixError(
                        "future_version", inputVersion, currentVersion,
                        "Input config version " + inputVersion
                                + " is newer than supported version " + currentVersion));
            }

            Dynamic<?> current = input;
            int version = inputVersion;
            while (version < currentVersion) {
                FixStep step = fixes.get(version);
                if (step == null) {
                    return Either.left(new ConfigFixError(
                            "missing_step", version, currentVersion,
                            "No config fix starts at version " + version));
                }
                Dynamic<?> stepInput = current;
                Try<Dynamic<?>> applied = Try.of(() -> Objects.requireNonNull(
                        step.operation().apply(stepInput), "Config fix returned null"));
                if (applied.isFailure()) {
                    return Either.left(stepException(
                            step.fromVersion(), step.toVersion(), applied.cause()));
                }
                current = applied.get();
                version = step.toVersion();
            }
            return Either.right(current);
        }

        List<FixStep> steps() {
            ArrayList<FixStep> result = new ArrayList<>(fixes.values());
            result.sort(Comparator.comparingInt(FixStep::fromVersion));
            return List.copyOf(result);
        }
    }

    private record FixStep(
            int fromVersion, int toVersion, UnaryOperator<Dynamic<?>> operation) {
    }

    private record FieldStep(
            String path,
            ConfigValueMeta meta,
            ConfigValueMeta.FieldMigration migration) {
    }

    /**
     * Provides structural migration operations for one dynamic value.
     *
     * <p>Every operation returns a dynamic value that uses the same dynamic operations as the
     * source. Paths use nonempty dot-separated map keys. Operations whose source path is absent
     * return the source unchanged.
     */
    public static final class FixContext {
        private final Dynamic<?> dynamic;

        /**
         * Creates structural migration operations for a dynamic value.
         *
         * @param dynamic the source dynamic value
         */
        public FixContext(Dynamic<?> dynamic) {
            this.dynamic = Objects.requireNonNull(dynamic, "dynamic");
        }

        /**
         * Renames a value by moving it from one path to another path.
         *
         * @param fromPath the path to remove after reading its value
         * @param toPath the path that receives the value
         * @return the updated dynamic, or the source when {@code fromPath} is absent
         * @throws IllegalArgumentException if either path is blank or contains an empty component
         */
        public Dynamic<?> rename(String fromPath, String toPath) {
            return move(fromPath, toPath);
        }

        /**
         * Moves a value from one path to another path.
         *
         * @param fromPath the path to remove after reading its value
         * @param toPath the path that receives the value
         * @return the updated dynamic, or the source when {@code fromPath} is absent
         * @throws IllegalArgumentException if either path is blank or contains an empty component
         */
        public Dynamic<?> move(String fromPath, String toPath) {
            Maybe<Dynamic<?>> value = get(dynamic, segments(fromPath), 0);
            if (value.isEmpty()) {
                return dynamic;
            }
            Dynamic<?> removed = remove(dynamic, segments(fromPath), 0);
            return set(removed, segments(toPath), 0, value.get());
        }

        /**
         * Copies a value to another path without removing its source.
         *
         * @param fromPath the path whose value is copied
         * @param toPath the path that receives the copied value
         * @return the updated dynamic, or the source when {@code fromPath} is absent
         * @throws IllegalArgumentException if either path is blank or contains an empty component
         */
        public Dynamic<?> copy(String fromPath, String toPath) {
            Maybe<Dynamic<?>> value = get(dynamic, segments(fromPath), 0);
            return value.isDefined()
                    ? set(dynamic, segments(toPath), 0, value.get())
                    : dynamic;
        }

        /**
         * Removes the value at a path.
         *
         * @param path the path to remove
         * @return the updated dynamic, or the source when {@code path} is absent
         * @throws IllegalArgumentException if {@code path} is blank or contains an empty component
         */
        public Dynamic<?> remove(String path) {
            return remove(dynamic, segments(path), 0);
        }

        /**
         * Sets a path only when no value is present at that path.
         *
         * @param path the path to inspect and possibly set
         * @param value the value assigned when the path is absent
         * @return the source when the path is present; otherwise the updated dynamic
         * @throws IllegalArgumentException if {@code path} is blank or contains an empty component
         */
        public Dynamic<?> setIfMissing(String path, Dynamic<?> value) {
            String[] parts = segments(path);
            return get(dynamic, parts, 0).isDefined()
                    ? dynamic
                    : set(dynamic, parts, 0, Objects.requireNonNull(value, "value"));
        }

        /**
         * Transforms a value when the specified path is present.
         *
         * @param path the path whose value is transformed
         * @param transformation the operation applied to the present value
         * @return the updated dynamic, or the source when {@code path} is absent
         * @throws IllegalArgumentException if {@code path} is blank or contains an empty component
         */
        public Dynamic<?> transform(String path, UnaryOperator<Dynamic<?>> transformation) {
            Objects.requireNonNull(transformation, "transformation");
            String[] parts = segments(path);
            Maybe<Dynamic<?>> value = get(dynamic, parts, 0);
            return value.isDefined()
                    ? set(dynamic, parts, 0, transformation.apply(value.get()))
                    : dynamic;
        }

        /**
         * Transforms a value when a pre-segmented path is present.
         *
         * @param root the complete dynamic value
         * @param path the nonempty path components that select a value
         * @param transformation the operation applied to the present value
         * @return the updated dynamic, or {@code root} when the path is absent
         * @throws IllegalArgumentException if {@code path} is empty or contains an empty component
         */
        public static Dynamic<?> transformAt(
                Dynamic<?> root,
                List<String> path,
                UnaryOperator<Dynamic<?>> transformation) {
            Objects.requireNonNull(root, "root");
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(transformation, "transformation");
            if (path.isEmpty() || path.stream().anyMatch(String::isBlank)) {
                throw new IllegalArgumentException("Config path components must not be empty");
            }
            String[] parts = path.toArray(String[]::new);
            Maybe<Dynamic<?>> value = get(root, parts, 0);
            return value.isDefined()
                    ? set(root, parts, 0, transformation.apply(value.get()))
                    : root;
        }

        private static String[] segments(String path) {
            Objects.requireNonNull(path, "path");
            if (path.isBlank()) {
                throw new IllegalArgumentException("Config path must not be blank");
            }
            String[] parts = path.split("\\.");
            for (String part : parts) {
                if (part.isBlank()) {
                    throw new IllegalArgumentException("Invalid config path: " + path);
                }
            }
            return parts;
        }

        private static Maybe<Dynamic<?>> get(Dynamic<?> root, String[] path, int index) {
            if (index == path.length) {
                return Maybe.some(root);
            }
            Maybe<?> child = OptionalOps.toMaybe(root.get(path[index]).result());
            if (child.isEmpty()) {
                return Maybe.none();
            }
            return get((Dynamic<?>) child.get(), path, index + 1);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static Dynamic<?> set(
                Dynamic<?> root, String[] path, int index, Dynamic<?> value) {
            if (index == path.length) {
                return value.convert(root.getOps());
            }
            Dynamic child = (Dynamic) OptionalOps.toMaybe(root.get(path[index]).result())
                    .fold(() -> new Dynamic(root.getOps(), root.getOps().emptyMap()), childValue -> childValue);
            Dynamic<?> updated = set(child, path, index + 1, value);
            return ((Dynamic) root).set(path[index], updated);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static Dynamic<?> remove(Dynamic<?> root, String[] path, int index) {
            if (index == path.length - 1) {
                return root.remove(path[index]);
            }
            Maybe<?> child = OptionalOps.toMaybe(root.get(path[index]).result());
            if (child.isEmpty()) {
                return root;
            }
            Dynamic<?> updated = remove((Dynamic<?>) child.get(), path, index + 1);
            return ((Dynamic) root).set(path[index], updated);
        }
    }
}
