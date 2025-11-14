package com.mafuyu404.oelib.forge.icon;

import com.mafuyu404.oelib.OELib;
import com.mafuyu404.oelib.icon.DynamicIconRegistry;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;

public final class DynamicIconForgeSubscriber {
    private DynamicIconForgeSubscriber() {}

    public static void onClientSetup() {
        for (IModInfo info : ModList.get().getMods()) {
            String modId = info.getModId();
            Optional<String> selected = DynamicIconRegistry.getSelectedForgePath(modId);
            if (selected.isEmpty()) continue;

            try {
                Field field = findLogoField(info);
                if (field == null) continue;
                field.setAccessible(true);
                field.set(info, selected);
                OELib.LOGGER.info("[OELib: DynamicIcon] Set icon for {}: {}", modId, selected.get());
            } catch (Throwable ignored) {}
        }
    }

    private static Field findLogoField(IModInfo info) {
        try {
            Field f = info.getClass().getDeclaredField("logoFile");
            if (Optional.class.isAssignableFrom(f.getType())) {
                return f;
            }
        } catch (NoSuchFieldException ignored) {}

        String current = info.getLogoFile().orElse(null);
        for (Field f : info.getClass().getDeclaredFields()) {
            if (Optional.class.isAssignableFrom(f.getType())) {
                try {
                    f.setAccessible(true);
                    Object v = f.get(info);
                    if (v instanceof Optional<?> opt) {
                        Object ov = opt.orElse(null);
                        if (ov instanceof String s && Objects.equals(s, current)) {
                            return f;
                        }
                    }
                } catch (IllegalAccessException ignored) {}
            }
        }
        return null;
    }
}