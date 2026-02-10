package cc.sighs.oelib.forge.network;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.network.api.CustomPacketPayload;
import cc.sighs.oelib.network.api.INetworkPacket;
import cc.sighs.oelib.network.api.NetworkPacket;
import cc.sighs.oelib.network.spi.INetworkAutoRegistration;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.annotation.ElementType;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;

public final class ForgeNetworkAutoRegistration implements INetworkAutoRegistration {

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
            loader = ForgeNetworkAutoRegistration.class.getClassLoader();
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
