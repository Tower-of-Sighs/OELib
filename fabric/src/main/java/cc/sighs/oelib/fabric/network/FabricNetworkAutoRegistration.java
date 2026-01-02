package cc.sighs.oelib.fabric.network;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.spi.INetworkAutoRegistration;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class FabricNetworkAutoRegistration implements INetworkAutoRegistration {

    private static void scanDirectory(File directory, String basePackage, Set<Class<? extends INetworkPacket<?>>> result, ClassLoader loader) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                String subPackage = basePackage + "." + file.getName();
                scanDirectory(file, subPackage, result, loader);
            } else if (file.getName().endsWith(".class")) {
                String className = basePackage + "." + file.getName().substring(0, file.getName().length() - 6);
                maybeAddPacketClass(className, result, loader);
            }
        }
    }

    private static void scanJar(URL url, String path, String basePackage, Set<Class<? extends INetworkPacket<?>>> result, ClassLoader loader) {
        try {
            JarURLConnection connection = (JarURLConnection) url.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith(path) || !name.endsWith(".class") || entry.isDirectory()) {
                        continue;
                    }
                    String className = name.replace('/', '.').substring(0, name.length() - 6);
                    maybeAddPacketClass(className, result, loader);
                }
            }
        } catch (IOException e) {
            OELib.LOGGER.error("Failed to scan jar for network packets at {}", url, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void maybeAddPacketClass(String className, Set<Class<? extends INetworkPacket<?>>> result, ClassLoader loader) {
        try {
            Class<?> clazz = Class.forName(className, false, loader);
            if (!INetworkPacket.class.isAssignableFrom(clazz)) {
                return;
            }
            if (!CustomPacketPayload.class.isAssignableFrom(clazz)) {
                return;
            }
            if (!clazz.isAnnotationPresent(NetworkPacket.class)) {
                return;
            }
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                return;
            }
            result.add((Class<? extends INetworkPacket<?>>) clazz);
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
        }
    }

    @Override
    public Set<Class<? extends INetworkPacket<?>>> findAnnotatedPackets(Set<String> basePackages) {
        Set<Class<? extends INetworkPacket<?>>> result = new LinkedHashSet<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = FabricNetworkAutoRegistration.class.getClassLoader();
        }
        for (String basePackage : basePackages) {
            String path = basePackage.replace('.', '/');
            try {
                Enumeration<URL> resources = loader.getResources(path);
                while (resources.hasMoreElements()) {
                    URL url = resources.nextElement();
                    String protocol = url.getProtocol();
                    if ("file".equals(protocol)) {
                        scanDirectory(new File(url.getPath()), basePackage, result, loader);
                    } else if ("jar".equals(protocol)) {
                        scanJar(url, path, basePackage, result, loader);
                    }
                }
            } catch (IOException e) {
                OELib.LOGGER.error("Failed to scan for network packets in package {}", basePackage, e);
            }
        }
        return result;
    }
}

