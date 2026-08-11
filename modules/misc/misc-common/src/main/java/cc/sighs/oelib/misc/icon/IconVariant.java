package cc.sighs.oelib.misc.icon;

import java.util.Objects;

public final class IconVariant {
    public enum Platform {COMMON, FABRIC, FORGE}

    private final String name;
    private final String path;
    private final int weight;
    private final Platform platform;

    private IconVariant(String name, String path, int weight, Platform platform) {
        this.name = name;
        this.path = path;
        this.weight = Math.max(1, weight);
        this.platform = platform;
    }

    public static IconVariant name(String name, int weight) {
        return new IconVariant(Objects.requireNonNull(name), null, weight, Platform.COMMON);
    }

    public static IconVariant path(String path, int weight) {
        return new IconVariant(null, Objects.requireNonNull(path), weight, Platform.COMMON);
    }

    public static IconVariant nameFabric(String name, int weight) {
        return new IconVariant(Objects.requireNonNull(name), null, weight, Platform.FABRIC);
    }

    public static IconVariant pathFabric(String path, int weight) {
        return new IconVariant(null, Objects.requireNonNull(path), weight, Platform.FABRIC);
    }

    public static IconVariant nameForge(String name, int weight) {
        return new IconVariant(Objects.requireNonNull(name), null, weight, Platform.FORGE);
    }

    public static IconVariant pathForge(String path, int weight) {
        return new IconVariant(null, Objects.requireNonNull(path), weight, Platform.FORGE);
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public int getWeight() {
        return weight;
    }

    public Platform getPlatform() {
        return platform;
    }

    boolean isName() {
        return name != null;
    }

    boolean isPath() {
        return path != null;
    }

    boolean appliesToFabric() {
        return platform == Platform.COMMON || platform == Platform.FABRIC;
    }

    boolean appliesToForge() {
        return platform == Platform.COMMON || platform == Platform.FORGE;
    }
}