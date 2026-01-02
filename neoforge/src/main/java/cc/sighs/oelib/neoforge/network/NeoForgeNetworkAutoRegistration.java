package cc.sighs.oelib.neoforge.network;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.spi.INetworkAutoRegistration;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.annotation.ElementType;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;

public final class NeoForgeNetworkAutoRegistration implements INetworkAutoRegistration {

    private static boolean matchesBasePackages(String className, Set<String> basePackages) {
        if (basePackages.isEmpty()) {
            return true;
        }
        for (String base : basePackages) {
            if (className.startsWith(base + ".") || className.equals(base)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Class<? extends INetworkPacket<?>>> findAnnotatedPackets(Set<String> basePackages) {
        Set<Class<? extends INetworkPacket<?>>> result = new LinkedHashSet<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = NeoForgeNetworkAutoRegistration.class.getClassLoader();
        }
        Type annotationType = Type.getType(NetworkPacket.class);
        for (ModFileScanData scanData : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData annotation : scanData.getAnnotations()) {
                if (annotation.targetType() != ElementType.TYPE) {
                    continue;
                }
                if (!annotation.annotationType().equals(annotationType)) {
                    continue;
                }
                String className = annotation.clazz().getClassName();
                if (!matchesBasePackages(className, basePackages)) {
                    continue;
                }
                try {
                    Class<?> clazz = Class.forName(className, false, loader);
                    if (!INetworkPacket.class.isAssignableFrom(clazz)) {
                        continue;
                    }
                    if (!CustomPacketPayload.class.isAssignableFrom(clazz)) {
                        continue;
                    }
                    if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                        continue;
                    }
                    result.add((Class<? extends INetworkPacket<?>>) clazz);
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    OELib.LOGGER.error("Failed to load annotated network packet {}", className, e);
                }
            }
        }
        return result;
    }
}

