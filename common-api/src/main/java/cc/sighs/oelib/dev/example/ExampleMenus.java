package cc.sighs.oelib.dev.example;

import cc.sighs.oelib.OELib;
import cc.sighs.oelib.registry.DeferredRegister;
import cc.sighs.oelib.registry.RegisterSupplier;
import cc.sighs.oelib.registry.extra.MenuRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class ExampleMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, OELib.MODID);

    public static void init() {
        MENUS.register();
    }
    public static final RegisterSupplier<MenuType<FluidRenderExampleMenu>> FLUID_RENDER_EXAMPLE =
            MENUS.register("fluid_render_example",
                    () -> new MenuType<>(FluidRenderExampleMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void registerScreens() {
        MenuRegister.registerScreenFactory(
                FLUID_RENDER_EXAMPLE,
                FluidRenderExampleScreen::new
        );
    }

}
