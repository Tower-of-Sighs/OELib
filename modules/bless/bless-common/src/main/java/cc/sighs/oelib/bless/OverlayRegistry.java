package cc.sighs.oelib.bless;

import cc.sighs.oelib.bless.render.AbstractShaderOverlay;
import cc.sighs.oelib.bless.render.ChongYangOverlay;
import cc.sighs.oelib.bless.render.NewYearOverlay;
import cc.sighs.oelib.bless.render.ValentineOverlay;

import java.util.ArrayList;
import java.util.List;

public final class OverlayRegistry {
    public static final List<AbstractShaderOverlay> REGISTERED_OVERLAYS = new ArrayList<>();

    private OverlayRegistry() {
    }

    public static void init() {
        register(ChongYangOverlay.INSTANCE);
        register(NewYearOverlay.INSTANCE);
        register(ValentineOverlay.INSTANCE);
    }

    public static void register(AbstractShaderOverlay overlay) {
        if (!REGISTERED_OVERLAYS.contains(overlay)) {
            REGISTERED_OVERLAYS.add(overlay);
        }
    }
}
