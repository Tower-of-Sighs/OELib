package com.sighs.oelib.example;

import com.sighs.oelib.OELib;
import com.sighs.oelib.registry.DeferredRegister;
import com.sighs.oelib.registry.RegisterSupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class ExampleMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, OELib.MODID);

    public static void init() {
        MENUS.register();
    }    public static final RegisterSupplier<MenuType<FluidRenderExampleMenu>> FLUID_RENDER_EXAMPLE =
            MENUS.register("fluid_render_example",
                    () -> new MenuType<>(FluidRenderExampleMenu::new, FeatureFlags.DEFAULT_FLAGS));


}
